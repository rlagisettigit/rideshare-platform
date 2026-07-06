import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { getRideStops } from "../api/rides";
import { getLocation } from "../api/location";
import { loadGoogleMaps } from "../lib/googleMaps";

/** Fetches the real road-following path through an ordered list of stops via Google's
 *  DirectionsService (available worldwide, unlike Mappls) - used purely as a geometry source
 *  here, not for rendering, so the actual map stays Leaflet/OSM like the rest of the app.
 *  optimizeWaypoints is deliberately false: RideStopPlanningService already decided the order
 *  (and factors in per-passenger max-detour limits Google's own optimizer knows nothing about),
 *  so this must draw that exact order, not silently re-optimize it. */
async function fetchRoadPath(orderedLatLngs) {
  if (orderedLatLngs.length < 2) return null;
  const google = await loadGoogleMaps();
  const directionsService = new google.maps.DirectionsService();
  const [origin, ...rest] = orderedLatLngs;
  const destination = rest[rest.length - 1];
  const waypoints = rest.slice(0, -1).map(([lat, lng]) => ({ location: { lat, lng }, stopover: true }));

  return new Promise((resolve) => {
    directionsService.route(
      {
        origin: { lat: origin[0], lng: origin[1] },
        destination: { lat: destination[0], lng: destination[1] },
        waypoints,
        optimizeWaypoints: false,
        travelMode: google.maps.TravelMode.DRIVING
      },
      (result, status) => {
        if (status !== "OK" || !result.routes?.[0]) {
          resolve(null);
          return;
        }
        const path = result.routes[0].overview_path.map((p) => [p.lat(), p.lng()]);
        const legs = result.routes[0].legs.map((l) => l.distance.value / 1000);
        resolve({ path, legDistancesKm: legs });
      }
    );
  });
}

/** Builds a "let the user pick which maps app opens" deep link set - the web equivalent of the
 *  native "Open in..." chooser a phone shows for a maps link, since a browser can't invoke that
 *  OS picker directly.
 *
 *  Only Google Maps' web URL reliably supports multiple waypoints (documented `waypoints=`
 *  param), so it alone gets the full P1->P2->...->D1->D2->...->destination plan. Apple Maps has
 *  no waypoint support at all (confirmed via Apple's own developer forums). Mappls' `places=`
 *  param *looks* like it should take a semicolon-separated list of stops, and it does accept
 *  more than two without erroring - but verified empirically that anything beyond the first and
 *  last point is silently dropped from the actual calculated route (confirmed against a real
 *  India route with intermediate stops: the resulting page showed only a direct start->end
 *  route, ignoring every via point even though they're visible in the URL's own data blob).
 *  So both Apple Maps and Mappls only ever get origin->destination here. */
function buildNavigationLinks(origin, waypoints, destination) {
  const originStr = origin ? `${origin.lat},${origin.lng}` : "";
  const destStr = `${destination.lat},${destination.lng}`;
  const singleStopNote = waypoints.length > 0 ? "opens directions to your final destination only - no multi-stop support" : null;

  const googleParams = new URLSearchParams({ api: "1", destination: destStr, travelmode: "driving" });
  if (originStr) googleParams.set("origin", originStr);
  if (waypoints.length > 0) googleParams.set("waypoints", waypoints.map((w) => `${w.lat},${w.lng}`).join("|"));

  const appleParams = new URLSearchParams({ daddr: destStr, dirflg: "d" });
  if (originStr) appleParams.set("saddr", originStr);

  const mapplsPlaces = (origin ? `${originStr};` : "") + destStr;
  const mapplsParams = new URLSearchParams({ places: mapplsPlaces, isNav: "true", mode: "driving" });

  return [
    { label: "Google Maps", url: `https://www.google.com/maps/dir/?${googleParams.toString()}`, note: null },
    { label: "Apple Maps", url: `https://maps.apple.com/?${appleParams.toString()}`, note: singleStopNote },
    { label: "Mappls", url: `https://mappls.com/navigation?${mapplsParams.toString()}`, note: singleStopNote }
  ];
}

const driverIcon = L.divIcon({
  className: "",
  html: '<div style="background:var(--color-route);width:16px;height:16px;border-radius:50%;border:2px solid white;box-shadow:0 0 4px rgba(0,0,0,0.4)"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8]
});

function stopIcon(label, color) {
  return L.divIcon({
    className: "",
    html: `<div style="background:${color};color:white;width:24px;height:24px;border-radius:50%;border:2px solid white;
           box-shadow:0 0 4px rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;
           font-family:'JetBrains Mono',monospace;font-size:11px;font-weight:700;">${label}</div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  });
}

/** FR: Pickup/Drop-off Ordering - shows the driver's optimized visiting order for an
 *  in-progress ride (numbered pickup/drop markers connected in that order), plus the driver's
 *  live position. Used both on the driver's own dashboard and on a passenger's "Track driver"
 *  view - same map, same order, since every passenger on the ride shares one trip plan.
 *
 *  passengerDestination: when set (the passenger view), "Start navigation" points from the
 *  driver's current live position to just *this* passenger's own drop-off - not the driver's
 *  full multi-stop plan, which would expose every other passenger's pickup/drop address through
 *  an external maps link. A single leg also sidesteps Apple Maps/Mappls' lack of multi-stop
 *  support entirely, since there's nothing to drop. */
export default function RideStopMap({ ridePublicId, showNavigation = false, passengerDestination = null }) {
  const mapRef = useRef(null);
  const containerRef = useRef(null);
  const driverMarkerRef = useRef(null);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [stops, setStops] = useState(null);
  const [stopsError, setStopsError] = useState(null);
  const [driverLocation, setDriverLocation] = useState(null);
  const [navMenuOpen, setNavMenuOpen] = useState(false);

  useEffect(() => {
    if (!containerRef.current) return;

    const map = L.map(containerRef.current).setView([20.5937, 78.9629], 5);
    mapRef.current = map;
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap contributors",
      maxZoom: 19
    }).addTo(map);

    // Deliberately does NOT draw the ride's raw origin-to-destination line: that's the
    // published corridor, not the driver's actual plan, and drawing both was confusing (it
    // just showed as one straight line ignoring every stop). The only path drawn is the real
    // one: driver -> pickups in order -> drop-offs in order -> the ride's final destination.
    getRideStops(ridePublicId)
      .then(async (res) => {
        if (!mapRef.current) return;
        const { startLat, startLng, pickups, dropoffs, destLat, destLng, destAddress } = res.data;
        const boundsPoints = [[startLat, startLng]];

        pickups.forEach((s) => {
          L.marker([s.lat, s.lng], { icon: stopIcon(`P${s.order}`, "#2f3c7e") })
            .addTo(mapRef.current)
            .bindPopup(`Pickup ${s.order}: ${s.passengerName}<br/>${s.address ?? ""}`);
          boundsPoints.push([s.lat, s.lng]);
        });
        dropoffs.forEach((s) => {
          L.marker([s.lat, s.lng], { icon: stopIcon(`D${s.order}`, "#f2a93b") })
            .addTo(mapRef.current)
            .bindPopup(`Drop-off ${s.order}: ${s.passengerName}<br/>${s.address ?? ""}`);
          boundsPoints.push([s.lat, s.lng]);
        });

        const lastDrop = dropoffs.length > 0 ? dropoffs[dropoffs.length - 1] : null;
        // The last passenger's drop-off is rarely the driver's own final destination - show it
        // as its own stop (unless it's essentially the same point already).
        const destIsDistinct = !lastDrop || L.latLng(lastDrop.lat, lastDrop.lng).distanceTo([destLat, destLng]) > 150;
        if (destIsDistinct) {
          L.marker([destLat, destLng], { icon: stopIcon("🏁", "#14171f") })
            .addTo(mapRef.current)
            .bindPopup(`Final destination<br/>${destAddress ?? ""}`);
          boundsPoints.push([destLat, destLng]);
        }

        const orderedLatLngs = [
          [startLat, startLng],
          ...pickups.map((s) => [s.lat, s.lng]),
          ...dropoffs.map((s) => [s.lat, s.lng]),
          ...(destIsDistinct ? [[destLat, destLng]] : [])
        ];

        // Try the real road-following path first; a straight line between stops is only a
        // fallback for when Directions itself is unavailable (no key, quota, no route found).
        const roadPath = await fetchRoadPath(orderedLatLngs).catch(() => null);
        if (roadPath?.path?.length > 1) {
          const line = L.polyline(roadPath.path, { color: "#2f3c7e", weight: 4, opacity: 0.85 }).addTo(mapRef.current);
          mapRef.current.fitBounds(line.getBounds(), { padding: [24, 24] });
          // legDistancesKm[i] is the leg *ending* at orderedLatLngs[i+1] - reattach onto the
          // matching pickup/drop/destination so the numbers under the map match what's drawn.
          setStops({
            ...res.data,
            pickups: pickups.map((s, i) => ({ ...s, legDistanceKm: roadPath.legDistancesKm[i] ?? s.legDistanceKm })),
            dropoffs: dropoffs.map((s, i) => ({ ...s, legDistanceKm: roadPath.legDistancesKm[pickups.length + i] ?? s.legDistanceKm }))
          });
        } else if (orderedLatLngs.length > 1) {
          const line = L.polyline(orderedLatLngs, { color: "#2f3c7e", weight: 4, opacity: 0.85, dashArray: "1 8" }).addTo(mapRef.current);
          mapRef.current.fitBounds(line.getBounds(), { padding: [24, 24] });
          setStops(res.data);
        } else {
          setStops(res.data);
          if (boundsPoints.length > 0) mapRef.current.setView(boundsPoints[0], 13);
        }
      })
      .catch((err) => setStopsError(err?.message ?? "Could not load the route plan."));

    return () => map.remove();
  }, [ridePublicId]);

  useEffect(() => {
    let cancelled = false;

    const poll = () => {
      getLocation(ridePublicId)
        .then((res) => {
          if (cancelled || !res.data || !mapRef.current) return;
          setError(null);
          setLastUpdated(res.data.updatedAt);
          const { lat, lng } = res.data;
          setDriverLocation({ lat, lng });
          if (driverMarkerRef.current) {
            driverMarkerRef.current.setLatLng([lat, lng]);
          } else {
            driverMarkerRef.current = L.marker([lat, lng], { icon: driverIcon }).addTo(mapRef.current).bindPopup("Driver");
          }
        })
        .catch((err) => !cancelled && setError(err?.message ?? "Could not load driver location."));
    };

    poll();
    const interval = setInterval(poll, 5000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [ridePublicId]);

  const stopCount = (stops?.pickups?.length ?? 0) + (stops?.dropoffs?.length ?? 0);

  const navLinks = stops
    ? passengerDestination
      ? buildNavigationLinks(driverLocation ?? { lat: stops.startLat, lng: stops.startLng }, [], passengerDestination)
      : buildNavigationLinks(
          driverLocation ?? { lat: stops.startLat, lng: stops.startLng },
          [...stops.pickups, ...stops.dropoffs].map((s) => ({ lat: s.lat, lng: s.lng })),
          { lat: stops.destLat, lng: stops.destLng }
        )
    : [];
  const navButtonLabel = passengerDestination ? "Get directions" : "Start navigation";
  const navHelperText = passengerDestination
    ? "Open directions to your drop-off in:"
    : "Open turn-by-turn directions in:";

  return (
    <div className="card stack">
      <div className="between">
        <strong>Route plan</strong>
        {showNavigation && stops && stopCount > 0 && (
          <div style={{ position: "relative" }}>
            <button type="button" className="btn btn-primary" onClick={() => setNavMenuOpen((v) => !v)}>
              {navButtonLabel} ▾
            </button>
            {navMenuOpen && (
              <div
                className="card stack"
                style={{ position: "absolute", right: 0, top: "110%", zIndex: 500, width: 260, gap: 4, padding: "8px" }}
              >
                <span className="muted" style={{ fontSize: "0.75rem", padding: "0 4px" }}>{navHelperText}</span>
                {navLinks.map((link) => (
                  <a
                    key={link.label}
                    href={link.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-secondary"
                    style={{ justifyContent: "flex-start", flexDirection: "column", alignItems: "flex-start", height: "auto", padding: "8px 12px" }}
                    onClick={() => setNavMenuOpen(false)}
                  >
                    <span>{link.label}</span>
                    {link.note && <span className="muted" style={{ fontSize: "0.7rem", fontWeight: 400 }}>{link.note}</span>}
                  </a>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
      {stopsError && <div className="error-text">{stopsError}</div>}
      {!stopsError && stops && stopCount === 0 && (
        <p className="muted">No confirmed passengers on this ride.</p>
      )}
      {error && <div className="error-text">{error}</div>}
      {!error && !lastUpdated && <p className="muted">Waiting for the driver's first location update…</p>}
      <div ref={containerRef} style={{ height: 380, borderRadius: 8 }} />
      <div className="row" style={{ flexWrap: "wrap", gap: 12 }}>
        {stops?.pickups?.map((s) => (
          <span key={`p-${s.bookingPublicId}`} className="muted" style={{ fontSize: "0.8rem" }}>
            <strong style={{ color: "var(--color-route)" }}>P{s.order}</strong> {s.passengerName} ({s.legDistanceKm.toFixed(1)} km)
          </span>
        ))}
      </div>
      <div className="row" style={{ flexWrap: "wrap", gap: 12 }}>
        {stops?.dropoffs?.map((s) => (
          <span key={`d-${s.bookingPublicId}`} className="muted" style={{ fontSize: "0.8rem" }}>
            <strong style={{ color: "var(--color-signal)" }}>D{s.order}</strong> {s.passengerName} ({s.legDistanceKm.toFixed(1)} km)
          </span>
        ))}
      </div>
      {lastUpdated && <span className="muted">Driver location updated {new Date(lastUpdated).toLocaleTimeString()}</span>}
    </div>
  );
}
