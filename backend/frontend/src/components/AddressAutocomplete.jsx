import { useEffect, useRef } from "react";
import { loadGoogleMaps } from "../lib/googleMaps";

export default function AddressAutocomplete({ label, value, onChange, onPlaceSelect, placeholder, required }) {
  const inputRef = useRef(null);
  const autocompleteRef = useRef(null);
  const onPlaceSelectRef = useRef(onPlaceSelect);
  onPlaceSelectRef.current = onPlaceSelect;

  useEffect(() => {
    let cancelled = false;

    loadGoogleMaps()
      .then((google) => {
        if (cancelled || !inputRef.current) return;
        autocompleteRef.current = new google.maps.places.Autocomplete(inputRef.current, {
          fields: ["formatted_address", "geometry"]
        });
        autocompleteRef.current.addListener("place_changed", () => {
          const place = autocompleteRef.current.getPlace();
          const location = place.geometry?.location;
          if (!location) return;
          onPlaceSelectRef.current({
            address: place.formatted_address ?? inputRef.current.value,
            lat: location.lat(),
            lng: location.lng()
          });
        });
      })
      .catch((err) => console.error("Google Maps failed to load:", err));

    return () => {
      cancelled = true;
      if (autocompleteRef.current) {
        window.google?.maps.event.clearInstanceListeners(autocompleteRef.current);
      }
    };
  }, []);

  return (
    <div className="field">
      {label && <label>{label}</label>}
      <input
        ref={inputRef}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        autoComplete="off"
      />
    </div>
  );
}
