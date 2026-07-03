import { useEffect, useState } from "react";
import { getMyBookings, cancelBooking } from "../api/bookings";

const STATUS_CLASS = {
  CONFIRMED: "badge-active",
  PENDING: "badge-pending",
  CANCELLED: "badge-cancelled",
  REJECTED: "badge-cancelled",
  COMPLETED: "badge-active"
};

// Ride progress is more informative than the static booking status once a booking is
// CONFIRMED - e.g. "CONFIRMED" alone doesn't tell a passenger the driver has actually
// started driving. Only overrides the label/class when it adds information.
function tripStatus(b) {
  if (b.status === "CONFIRMED" && b.rideStatus === "IN_PROGRESS") {
    return { label: "Ride in progress", cls: "badge-active" };
  }
  if (b.status === "COMPLETED") {
    return { label: "Ride completed", cls: "badge-active" };
  }
  return { label: b.status, cls: STATUS_CLASS[b.status] ?? "badge-pending" };
}

export default function MyBookings() {
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState(null);

  const load = () => {
    getMyBookings().then((res) => setBookings(res.data)).catch((err) => setError(err?.message));
  };

  useEffect(load, []);

  const handleCancel = async (publicId) => {
    if (!confirm("Cancel this booking?")) return;
    try {
      await cancelBooking(publicId, "Cancelled by passenger");
      load();
    } catch (err) {
      alert(err?.message ?? "Could not cancel booking.");
    }
  };

  return (
    <div className="stack">
      <div>
        <h1>My bookings</h1>
        <p>Requests, confirmations, and past rides.</p>
      </div>

      {error && <div className="error-text">{error}</div>}

      {bookings && bookings.length === 0 && (
        <div className="empty-state">No bookings yet. Search for a ride to get started.</div>
      )}

      <div className="stack">
        {bookings?.map((b) => {
          const trip = tripStatus(b);
          const canCancel = (b.status === "PENDING" || b.status === "CONFIRMED") && b.rideStatus !== "IN_PROGRESS";
          return (
            <div key={b.publicId} className="card between">
              <div>
                <div className="route-line">
                  <span className="stop">{b.pickupAddress ?? "Pickup"}</span>
                  <span className="path" />
                  <span className="stop">{b.dropAddress ?? "Drop"}</span>
                </div>
                <span className="muted">{b.seatsBooked} seat{b.seatsBooked === 1 ? "" : "s"} · ₹{Number(b.fare).toFixed(0)}</span>
              </div>
              <div className="row">
                <span className={`badge ${trip.cls}`}>{trip.label}</span>
                {canCancel && (
                  <button className="btn btn-secondary" onClick={() => handleCancel(b.publicId)}>Cancel</button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
