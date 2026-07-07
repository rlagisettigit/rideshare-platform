export default function RideCard({ ride, action }) {
  // estimatedFare comes from search results (priced for this passenger's actual pickup/drop/seats);
  // estimatedFarePerSeat comes from a ride-details lookup (priced for the full route, 1 seat).
  // Fall back to the driver's listed pricePerSeat only if neither estimate is present.
  const estimatedFare = ride.estimatedFare ?? ride.estimatedFarePerSeat;
  const hasEstimate = estimatedFare != null;
  const displayPrice = hasEstimate ? estimatedFare : ride.pricePerSeat;

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
            ₹{Number(displayPrice).toFixed(0)}{hasEstimate ? "*" : ""}
          </div>
          <span className="muted">per seat{hasEstimate ? " (estimated)" : ""}</span>
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

      {hasEstimate && ride.fareDisclaimer && (
        <div className="muted" style={{ fontSize: "0.75rem", marginTop: 4 }}>{ride.fareDisclaimer}</div>
      )}

      {action && <div style={{ marginTop: 12 }}>{action}</div>}
    </div>
  );
}
