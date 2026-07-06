import RideStopMap from "./RideStopMap";

/** booking: needs ridePublicId, dropLat, dropLng. Kept as a thin wrapper (same name/props
 *  MyBookings.jsx already uses for "Track driver") around the shared RideStopMap, which shows
 *  every passenger's optimized pickup/drop-off order for this ride, not just this one booking's
 *  own stop. "Start navigation" is scoped to just this passenger's own drop-off, though - not
 *  the driver's full multi-stop plan, which would expose every other passenger's address. */
export default function DriverTrackingMap({ booking }) {
  return (
    <RideStopMap
      ridePublicId={booking.ridePublicId}
      showNavigation
      passengerDestination={{ lat: booking.dropLat, lng: booking.dropLng }}
    />
  );
}
