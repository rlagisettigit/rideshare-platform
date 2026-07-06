import { useEffect, useRef, useState } from "react";
import { loadGoogleMaps } from "../lib/googleMaps";

const DEFAULT_CENTER = { lat: 17.385, lng: 78.4867 }; // Hyderabad - same fallback city AddressAutocomplete's results cluster around

/** A "Pick on map" companion to AddressAutocomplete: opens a map overlay, lets the user click or
 *  drag a pin to the exact spot, reverse-geocodes it back into a human-readable address, and
 *  reports {address, lat, lng} - the same shape AddressAutocomplete's onPlaceSelect already gives,
 *  so callers can wire it into the same state update.
 *
 *  useCurrentLocation: when true and no address has been picked yet, opening the map centers on
 *  and drops a pin at the browser's geolocation instead of a fixed city center - the natural
 *  default for "where do you want picking up from", same as Uber/Ola's pickup pin. Falls back to
 *  the fixed default silently if geolocation is denied/unavailable. */
export default function MapLocationPicker({ label = "Pick on map", initialLat, initialLng, onSelect, useCurrentLocation = false }) {
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState(null);
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const geocoderRef = useRef(null);
  const placeMarkerRef = useRef(null);

  const locateMe = () => {
    if (!navigator.geolocation) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocating(false);
        const { latitude, longitude } = position.coords;
        if (mapRef.current) {
          mapRef.current.setCenter({ lat: latitude, lng: longitude });
          mapRef.current.setZoom(15);
        }
        placeMarkerRef.current?.(latitude, longitude);
      },
      () => setLocating(false),
      { enableHighAccuracy: true, timeout: 8000 }
    );
  };

  useEffect(() => {
    if (!open || !containerRef.current) return;
    let cancelled = false;
    setError(null);

    loadGoogleMaps()
      .then((google) => {
        if (cancelled || !containerRef.current) return;
        const hasInitial = initialLat != null && initialLng != null;
        const center = hasInitial ? { lat: initialLat, lng: initialLng } : DEFAULT_CENTER;
        const map = new google.maps.Map(containerRef.current, {
          center,
          zoom: hasInitial ? 15 : 11,
          streetViewControl: false,
          mapTypeControl: false,
          fullscreenControl: false
        });
        mapRef.current = map;
        geocoderRef.current = new google.maps.Geocoder();

        const placeMarker = (lat, lng) => {
          if (markerRef.current) {
            markerRef.current.setPosition({ lat, lng });
          } else {
            markerRef.current = new google.maps.Marker({ position: { lat, lng }, map, draggable: true });
            markerRef.current.addListener("dragend", (e) => reverseGeocode(e.latLng.lat(), e.latLng.lng()));
          }
          reverseGeocode(lat, lng);
        };
        placeMarkerRef.current = placeMarker;

        map.addListener("click", (e) => placeMarker(e.latLng.lat(), e.latLng.lng()));
        if (hasInitial) {
          placeMarker(initialLat, initialLng);
        } else if (useCurrentLocation) {
          locateMe();
        }
      })
      .catch((err) => setError(err?.message ?? "Could not load the map."));

    return () => {
      cancelled = true;
      mapRef.current = null;
      markerRef.current = null;
      geocoderRef.current = null;
      placeMarkerRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const reverseGeocode = (lat, lng) => {
    setLoading(true);
    geocoderRef.current.geocode({ location: { lat, lng } }, (results, status) => {
      setLoading(false);
      if (status === "OK" && results?.[0]) {
        setSelected({ lat, lng, address: results[0].formatted_address });
      } else {
        setSelected({ lat, lng, address: `${lat.toFixed(5)}, ${lng.toFixed(5)}` });
      }
    });
  };

  const confirm = () => {
    if (!selected) return;
    onSelect(selected);
    setOpen(false);
    setSelected(null);
  };

  return (
    <>
      <button type="button" className="btn btn-secondary" onClick={() => setOpen(true)}>
        📍 {label}
      </button>

      {open && (
        <div
          onClick={() => setOpen(false)}
          style={{
            position: "fixed", inset: 0, background: "rgba(20,23,31,0.5)",
            display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000
          }}
        >
          <div onClick={(e) => e.stopPropagation()} className="card stack" style={{ maxWidth: 640, width: "92%" }}>
            <div className="between">
              <h3 style={{ margin: 0 }}>Pick a location</h3>
              <button type="button" className="btn btn-secondary" onClick={() => setOpen(false)}>Close</button>
            </div>
            <div className="between">
              <p className="muted" style={{ margin: 0 }}>Click the map to drop a pin, or drag it to fine-tune the spot.</p>
              <button type="button" className="btn btn-secondary" onClick={locateMe} disabled={locating}>
                {locating ? "Locating…" : "📍 Use my current location"}
              </button>
            </div>
            {error && <div className="error-text">{error}</div>}
            <div ref={containerRef} style={{ height: 360, borderRadius: 8 }} />
            <div className="between">
              <span className="muted">{loading ? "Looking up address…" : (selected?.address ?? "No location selected yet")}</span>
              <button type="button" className="btn btn-primary" disabled={!selected || loading} onClick={confirm}>
                Use this location
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
