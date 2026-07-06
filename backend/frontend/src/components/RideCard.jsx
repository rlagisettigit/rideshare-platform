export default function RideCard({ ride, action }) {
  return (
    <div className="card">
      <div className="between">
        <div>
          <div className="row" style={{ marginBottom: 4 }}>
            <strong>{ride.driverName}</strong>
            {ride.driverRating != null && <span className="muted mono">★ {ride.driverRating.toFixed(1)}</span>}
            {ride.recurringRidePublicId && <span className="badge badge-active">Recurring</span>}
          </div>
          <span className="muted">{ride.vehicleModel}</span>
        </div>
        <div style={{ textAlign: "right" }}>
          <div className="mono" style={{ fontSize: "1.1rem", fontWeight: 600 }}>
            ₹{Number(ride.pricePerSeat).toFixed(0)}
          </div>
          <span className="muted">per seat</span>
        </div>
      </div>

      <div className="route-line">
        <span className="stop">{ride.originAddress ?? "Origin"}</span>
        <span className="path" />
        <span className="stop">{ride.destinationAddress ?? "Destination"}</span>
      </div>

      <div className="between">
        <span className="muted">
          {new Date(ride.departureAt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" })}
          {" · "}{ride.availableSeats} seat{ride.availableSeats === 1 ? "" : "s"} left
        </span>
        <div className="row">
          {ride.womenOnly && <span className="badge badge-pending">Women only</span>}
          {ride.petsAllowed && <span className="badge badge-active">Pets ok</span>}
        </div>
      </div>

      {action && <div style={{ marginTop: 12 }}>{action}</div>}
    </div>
  );
}
