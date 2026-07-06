import { useEffect, useState } from "react";
import Modal from "./Modal";
import { getRecurringOccurrences, bookAllRecurring } from "../api/recurringRides";

/**
 * pickup/drop: { pickupLat, pickupLng, pickupAddress, dropLat, dropLng, dropAddress, seats }
 * A passenger might only travel some days of a driver's recurring schedule (e.g. skip Tuesdays),
 * so this lets them pick specific dates instead of always booking every upcoming occurrence.
 */
export default function RecurringBookingModal({ recurringRidePublicId, pickup, onClose, onBooked }) {
  const [occurrences, setOccurrences] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(new Set());
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getRecurringOccurrences(recurringRidePublicId)
      .then((res) => {
        setOccurrences(res.data);
        setSelected(new Set(res.data.map((o) => o.ridePublicId)));
      })
      .catch((err) => setError(err?.message ?? "Could not load upcoming dates."));
  }, [recurringRidePublicId]);

  const toggle = (ridePublicId) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(ridePublicId)) next.delete(ridePublicId); else next.add(ridePublicId);
      return next;
    });
  };

  const selectAll = () => setSelected(new Set(occurrences.map((o) => o.ridePublicId)));
  const clearAll = () => setSelected(new Set());

  const handleConfirm = async () => {
    if (selected.size === 0) {
      setError("Select at least one date.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const res = await bookAllRecurring(recurringRidePublicId, {
        ...pickup,
        ridePublicIds: Array.from(selected)
      });
      onBooked(res.data);
    } catch (err) {
      setError(err?.message ?? "Could not book these dates.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal title="Choose dates to book" onClose={onClose}>
      <div className="stack">
        {error && <div className="error-text">{error}</div>}
        {!occurrences && !error && <p className="muted">Loading upcoming dates…</p>}
        {occurrences && occurrences.length === 0 && (
          <div className="empty-state">No upcoming occurrences left to book.</div>
        )}
        {occurrences && occurrences.length > 0 && (
          <>
            <div className="row">
              <button type="button" className="btn btn-secondary" onClick={selectAll}>Select all</button>
              <button type="button" className="btn btn-secondary" onClick={clearAll}>Clear</button>
            </div>
            <div className="stack" style={{ maxHeight: 280, overflowY: "auto" }}>
              {occurrences.map((o) => (
                <label key={o.ridePublicId} className="checkbox-field between">
                  <span>
                    <input
                      type="checkbox"
                      checked={selected.has(o.ridePublicId)}
                      onChange={() => toggle(o.ridePublicId)}
                      style={{ marginRight: 8 }}
                    />
                    {new Date(o.departureAt).toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })}
                  </span>
                  <span className="muted">{o.availableSeats} seats left</span>
                </label>
              ))}
            </div>
            <button className="btn btn-primary" onClick={handleConfirm} disabled={submitting}>
              {submitting ? "Requesting…" : `Book ${selected.size} selected date${selected.size === 1 ? "" : "s"}`}
            </button>
          </>
        )}
      </div>
    </Modal>
  );
}
