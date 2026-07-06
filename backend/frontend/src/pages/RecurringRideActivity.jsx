import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getMyRecurringRides, cancelRecurringRide } from "../api/recurringRides";
import { getMyRides, cancelRide, startRide, finishRide } from "../api/rides";
import { getMyBookings, cancelBooking } from "../api/bookings";

const RIDE_STATUS_BADGE = {
  PENDING: "badge-pending", ACTIVE: "badge-active", IN_PROGRESS: "badge-active",
  CANCELLED: "badge-cancelled", FINISHED: "badge-active"
};
const BOOKING_STATUS_BADGE = {
  PENDING: "badge-pending", CONFIRMED: "badge-active", REJECTED: "badge-cancelled",
  CANCELLED: "badge-cancelled", COMPLETED: "badge-active"
};

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}
function formatTime(iso) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

/** Groups items that share a recurringRidePublicId into one "thread" per series - the same
 *  idea as an email conversation view: one collapsible header per subject (here, per series),
 *  with the individual occurrences nested underneath instead of each date looking like an
 *  unrelated ride. Items with no recurringRidePublicId (one-off rides/bookings) are dropped -
 *  this page is deliberately just the recurring-only view. */
function buildThreads(items, dateOf) {
  const byKey = new Map();
  for (const item of items) {
    const key = item.recurringRidePublicId;
    if (!key) continue;
    if (!byKey.has(key)) byKey.set(key, []);
    byKey.get(key).push(item);
  }
  return [...byKey.entries()].map(([key, occurrences]) => ({
    key,
    occurrences: occurrences.slice().sort((a, b) => new Date(dateOf(a)) - new Date(dateOf(b)))
  }));
}

/** Of all threads across both roles, the one containing the soonest not-yet-departed
 *  occurrence is the most actionable - expand that one by default, like Outlook expanding
 *  the newest message in a conversation. */
function pickDefaultExpanded(threads, dateOf) {
  const now = Date.now();
  let best = null;
  for (const t of threads) {
    for (const occ of t.occurrences) {
      const time = new Date(dateOf(occ)).getTime();
      if (time < now) continue;
      if (!best || time < best.time) best = { key: t.key, time };
    }
  }
  return best?.key ?? null;
}

export default function RecurringRideActivity() {
  const [driverRides, setDriverRides] = useState(null);
  const [driverSeries, setDriverSeries] = useState([]);
  const [bookings, setBookings] = useState(null);
  const [expanded, setExpanded] = useState(new Set());
  const [defaultsApplied, setDefaultsApplied] = useState(false);
  const [busyKey, setBusyKey] = useState(null);

  const loadDriverRides = () => getMyRides().then((res) => setDriverRides(res.data)).catch(() => setDriverRides([]));
  const loadDriverSeries = () => getMyRecurringRides().then((res) => setDriverSeries(res.data)).catch(() => setDriverSeries([]));
  const loadBookings = () => getMyBookings().then((res) => setBookings(res.data)).catch(() => setBookings([]));

  useEffect(() => {
    loadDriverRides();
    loadDriverSeries();
    loadBookings();
  }, []);

  const driverThreads = useMemo(
    () => (driverRides ? buildThreads(driverRides, (r) => r.departureAt) : []),
    [driverRides]
  );
  const passengerThreads = useMemo(
    () => (bookings ? buildThreads(bookings, (b) => b.rideDepartureAt) : []),
    [bookings]
  );
  const seriesByPublicId = useMemo(
    () => new Map(driverSeries.map((s) => [s.publicId, s])),
    [driverSeries]
  );

  // Runs once both roles' data have loaded, so the "expand the next upcoming occurrence"
  // default only fires once instead of re-collapsing everything on every reload.
  useEffect(() => {
    if (defaultsApplied || driverRides === null || bookings === null) return;
    const key = pickDefaultExpanded(driverThreads, (r) => r.departureAt)
        ?? pickDefaultExpanded(passengerThreads, (b) => b.rideDepartureAt);
    if (key) setExpanded(new Set([key]));
    setDefaultsApplied(true);
  }, [defaultsApplied, driverRides, bookings, driverThreads, passengerThreads]);

  const toggle = (key) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  const withBusy = async (key, fn) => {
    setBusyKey(key);
    try {
      await fn();
    } catch (err) {
      alert(err?.message ?? "That action failed.");
    } finally {
      setBusyKey(null);
    }
  };

  const handleStart = (publicId) => withBusy(publicId, () => startRide(publicId).then(loadDriverRides));
  const handleFinish = (publicId) => withBusy(publicId, async () => {
    if (!confirm("Mark this occurrence as completed? This will settle fares for confirmed bookings.")) return;
    await finishRide(publicId);
    await loadDriverRides();
  });
  const handleCancelOccurrence = (publicId) => withBusy(publicId, async () => {
    if (!confirm("Cancel this occurrence? Passengers will be notified.")) return;
    await cancelRide(publicId);
    await loadDriverRides();
  });
  const handleCancelSeries = (publicId) => withBusy(publicId, async () => {
    if (!confirm("Cancel this whole recurring series? Every not-yet-started occurrence will be cancelled.")) return;
    await cancelRecurringRide(publicId);
    await Promise.all([loadDriverRides(), loadDriverSeries()]);
  });
  const handleCancelBooking = (publicId) => withBusy(publicId, async () => {
    if (!confirm("Cancel this booking?")) return;
    await cancelBooking(publicId, "Cancelled by passenger");
    await loadBookings();
  });

  const loading = driverRides === null || bookings === null;

  return (
    <div className="stack">
      <div className="between">
        <div>
          <h1>Recurring rides</h1>
          <p>Every date of a recurring series grouped into one thread, like a conversation - instead of one card per date.</p>
        </div>
        <Link to="/recurring-rides/publish" className="btn btn-primary">+ New recurring ride</Link>
      </div>

      {loading && <div className="empty-state">Loading…</div>}

      {!loading && driverThreads.length === 0 && passengerThreads.length === 0 && (
        <div className="empty-state">
          No recurring rides yet. <Link to="/recurring-rides/publish">Publish one</Link> or{" "}
          <Link to="/search">find one to book</Link>.
        </div>
      )}

      {driverThreads.length > 0 && (
        <div className="stack">
          <h3>As a driver</h3>
          {driverThreads.map((thread) => {
            const series = seriesByPublicId.get(thread.key);
            const first = thread.occurrences[0];
            const isOpen = expanded.has(thread.key);
            const upcoming = thread.occurrences.filter((o) => o.status === "ACTIVE" || o.status === "IN_PROGRESS").length;
            return (
              <div key={thread.key} className="card stack" style={{ gap: "var(--space-3)" }}>
                <button
                  type="button"
                  onClick={() => toggle(thread.key)}
                  className="between"
                  style={{ background: "none", border: "none", padding: 0, cursor: "pointer", textAlign: "left", width: "100%" }}
                >
                  <div>
                    <div className="row" style={{ marginBottom: 4 }}>
                      <span className="mono" style={{ color: "var(--color-muted)" }}>{isOpen ? "▾" : "▸"}</span>
                      <span className="badge badge-active">Recurring</span>
                      {series && <span className="muted">{series.daysOfWeek.map((d) => d.slice(0, 3)).join(", ")} at {series.departureTime}</span>}
                    </div>
                    <div className="route-line" style={{ margin: "4px 0" }}>
                      <span className="stop">{first.originAddress}</span>
                      <span className="path" />
                      <span className="stop">{first.destinationAddress}</span>
                    </div>
                    <span className="muted">{thread.occurrences.length} occurrence{thread.occurrences.length === 1 ? "" : "s"} · {upcoming} upcoming</span>
                  </div>
                  <div className="row">
                    {series && <span className={`badge ${series.status === "ACTIVE" ? "badge-active" : "badge-cancelled"}`}>{series.status}</span>}
                  </div>
                </button>

                {isOpen && (
                  <div className="stack" style={{ gap: "var(--space-2)", borderLeft: "2px solid var(--color-line)", paddingLeft: "var(--space-4)" }}>
                    {series?.status === "ACTIVE" && (
                      <div>
                        <button className="btn btn-secondary" disabled={busyKey === series.publicId} onClick={() => handleCancelSeries(series.publicId)}>
                          Cancel whole series
                        </button>
                      </div>
                    )}
                    {thread.occurrences.map((r) => (
                      <div key={r.publicId} className="between">
                        <span className="muted">{formatDate(r.departureAt)} · {formatTime(r.departureAt)} · {r.availableSeats} seats left</span>
                        <div className="row">
                          <span className={`badge ${RIDE_STATUS_BADGE[r.status] ?? "badge-pending"}`}>{r.status}</span>
                          {r.status === "ACTIVE" && (
                            <>
                              <button className="btn btn-secondary" disabled={busyKey === r.publicId} onClick={() => handleStart(r.publicId)}>Start</button>
                              <button className="btn btn-secondary" disabled={busyKey === r.publicId} onClick={() => handleCancelOccurrence(r.publicId)}>Cancel</button>
                            </>
                          )}
                          {r.status === "IN_PROGRESS" && (
                            <button className="btn btn-primary" disabled={busyKey === r.publicId} onClick={() => handleFinish(r.publicId)}>Complete</button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {passengerThreads.length > 0 && (
        <div className="stack">
          <h3>As a passenger</h3>
          {passengerThreads.map((thread) => {
            const first = thread.occurrences[0];
            const isOpen = expanded.has(thread.key);
            const activeCount = thread.occurrences.filter((b) => b.status === "PENDING" || b.status === "CONFIRMED").length;
            return (
              <div key={thread.key} className="card stack" style={{ gap: "var(--space-3)" }}>
                <button
                  type="button"
                  onClick={() => toggle(thread.key)}
                  className="between"
                  style={{ background: "none", border: "none", padding: 0, cursor: "pointer", textAlign: "left", width: "100%" }}
                >
                  <div>
                    <div className="row" style={{ marginBottom: 4 }}>
                      <span className="mono" style={{ color: "var(--color-muted)" }}>{isOpen ? "▾" : "▸"}</span>
                      <span className="badge badge-active">Recurring</span>
                    </div>
                    <div className="route-line" style={{ margin: "4px 0" }}>
                      <span className="stop">{first.rideOriginAddress}</span>
                      <span className="path" />
                      <span className="stop">{first.rideDestinationAddress}</span>
                    </div>
                    <span className="muted">{thread.occurrences.length} booking{thread.occurrences.length === 1 ? "" : "s"} · {activeCount} active</span>
                  </div>
                </button>

                {isOpen && (
                  <div className="stack" style={{ gap: "var(--space-2)", borderLeft: "2px solid var(--color-line)", paddingLeft: "var(--space-4)" }}>
                    {thread.occurrences.map((b) => {
                      const canCancel = (b.status === "PENDING" || b.status === "CONFIRMED") && b.rideStatus !== "IN_PROGRESS";
                      return (
                        <div key={b.publicId} className="between">
                          <span className="muted">{formatDate(b.rideDepartureAt)} · {formatTime(b.rideDepartureAt)} · ₹{Number(b.fare).toFixed(0)}</span>
                          <div className="row">
                            <span className={`badge ${BOOKING_STATUS_BADGE[b.status] ?? "badge-pending"}`}>{b.status}</span>
                            {canCancel && (
                              <button className="btn btn-secondary" disabled={busyKey === b.publicId} onClick={() => handleCancelBooking(b.publicId)}>Cancel</button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
