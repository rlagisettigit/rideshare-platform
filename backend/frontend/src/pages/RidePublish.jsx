import { useEffect, useState } from "react";
import { publishRide } from "../api/rides";
import { previewRoutes, fetchRoutePlaces } from "../api/routes";
import { getMyVehicles } from "../api/driver";
import AddressAutocomplete from "../components/AddressAutocomplete";

const ROUTE_LABELS = {
  FASTEST: "Fastest route",
  TOLL_FREE: "Toll-free route"
};

function routeLabel(label) {
  return ROUTE_LABELS[label] ?? `Alternate route ${label.replace("ALTERNATE_", "")}`;
}

// A couple of minutes of buffer so the departure time is still safely in the future by the
// time the request reaches the backend (which rejects a departure that isn't strictly after now).
function nowPlusBuffer(bufferMinutes = 2) {
  const d = new Date(Date.now() + bufferMinutes * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return {
    date: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
    time: `${pad(d.getHours())}:${pad(d.getMinutes())}`
  };
}

export default function RidePublish() {
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState({
    vehicleId: "", originAddress: "", originLat: "", originLng: "",
    destinationAddress: "", destinationLat: "", destinationLng: "",
    departureDate: "", departureTime: "", availableSeats: 1, pricePerSeat: "",
    luggageAllowed: true, smokingAllowed: false, womenOnly: false, petsAllowed: false,
    description: "", maxDetourKm: 5
  });
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);
  const [routeOptions, setRouteOptions] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [placesByLabel, setPlacesByLabel] = useState({});
  const [placesLoadingLabel, setPlacesLoadingLabel] = useState(null);

  useEffect(() => {
    getMyVehicles().then((res) => setVehicles(res.data)).catch(() => {});
  }, []);

  const update = (key) => (e) => {
    const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm({ ...form, [key]: value });
    if (key === "originAddress" || key === "destinationAddress") {
      setRouteOptions(null);
      setSelectedRoute(null);
      setPlacesByLabel({});
    }
  };

  // Defaults available seats to the vehicle's capacity minus the driver's own seat - still
  // editable afterward, since a driver may want to offer fewer (e.g. carrying luggage).
  const handleVehicleChange = (vehicleId) => {
    const vehicle = vehicles.find((v) => String(v.id) === String(vehicleId));
    setForm((f) => ({
      ...f,
      vehicleId,
      availableSeats: vehicle ? Math.max(1, vehicle.seatingCapacity - 1) : f.availableSeats
    }));
  };

  const handlePreviewRoutes = async () => {
    setPreviewError(null);
    if (!form.originLat || !form.originLng || !form.destinationLat || !form.destinationLng) {
      setPreviewError("Select both an origin and destination address first.");
      return;
    }
    setPreviewLoading(true);
    setRouteOptions(null);
    setSelectedRoute(null);
    setPlacesByLabel({});
    try {
      const res = await previewRoutes({
        originLat: parseFloat(form.originLat),
        originLng: parseFloat(form.originLng),
        destinationLat: parseFloat(form.destinationLat),
        destinationLng: parseFloat(form.destinationLng)
      });
      setRouteOptions(res.data);
    } catch (err) {
      setPreviewError(err?.message ?? "Could not fetch route options.");
    } finally {
      setPreviewLoading(false);
    }
  };

  const setDepartureToNow = () => {
    const { date, time } = nowPlusBuffer();
    setForm((f) => ({ ...f, departureDate: date, departureTime: time }));
  };

  const handleShowPlaces = async (option) => {
    setPlacesLoadingLabel(option.label);
    try {
      const res = await fetchRoutePlaces(option.encodedPolyline);
      setPlacesByLabel((prev) => ({ ...prev, [option.label]: res.data }));
    } catch {
      setPlacesByLabel((prev) => ({ ...prev, [option.label]: [] }));
    } finally {
      setPlacesLoadingLabel(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!form.originLat || !form.originLng) {
      setError("Please select the origin address from the suggestions list.");
      return;
    }
    if (!form.destinationLat || !form.destinationLng) {
      setError("Please select the destination address from the suggestions list.");
      return;
    }
    setStatus("publishing");
    try {
      await publishRide({
        ...form,
        vehicleId: Number(form.vehicleId),
        originLat: parseFloat(form.originLat),
        originLng: parseFloat(form.originLng),
        destinationLat: parseFloat(form.destinationLat),
        destinationLng: parseFloat(form.destinationLng),
        availableSeats: Number(form.availableSeats),
        pricePerSeat: parseFloat(form.pricePerSeat),
        maxDetourKm: parseFloat(form.maxDetourKm),
        ...(selectedRoute && {
          selectedRouteProvider: "MAPPLS",
          selectedRoutePolyline: selectedRoute.encodedPolyline,
          selectedRouteDistanceMeters: selectedRoute.distanceMeters,
          selectedRouteDurationSeconds: selectedRoute.durationSeconds
        })
      });
      setStatus("published");
    } catch (err) {
      setError(err?.message ?? "Could not publish this ride.");
      setStatus(null);
    }
  };

  return (
    <div className="stack">
      <div>
        <h1>Publish a ride</h1>
        <p>Your route is generated automatically and indexed for search once published.</p>
      </div>

      {vehicles.length === 0 && (
        <div className="card">
          <p style={{ marginBottom: 0 }}>
            No approved vehicles yet. Register a vehicle from the Driver dashboard before publishing a ride.
          </p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="card stack">
        <div className="field">
          <label>Vehicle</label>
          <select value={form.vehicleId} onChange={(e) => handleVehicleChange(e.target.value)} required>
            <option value="">Select a vehicle</option>
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>{v.brand} {v.model} · {v.vehicleNumber} ({v.status})</option>
            ))}
          </select>
        </div>

        <div className="field-row">
          <AddressAutocomplete
            label="Origin address"
            value={form.originAddress}
            onChange={update("originAddress")}
            onPlaceSelect={({ address, lat, lng }) => {
              setForm((f) => ({ ...f, originAddress: address, originLat: lat, originLng: lng }));
              setRouteOptions(null);
              setSelectedRoute(null);
              setPlacesByLabel({});
            }}
            placeholder="Start typing an address…"
            required
          />
        </div>
        <div className="field-row">
          <AddressAutocomplete
            label="Destination address"
            value={form.destinationAddress}
            onChange={update("destinationAddress")}
            onPlaceSelect={({ address, lat, lng }) => {
              setForm((f) => ({ ...f, destinationAddress: address, destinationLat: lat, destinationLng: lng }));
              setRouteOptions(null);
              setSelectedRoute(null);
              setPlacesByLabel({});
            }}
            placeholder="Start typing an address…"
            required
          />
        </div>

        <div className="field">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handlePreviewRoutes}
            disabled={previewLoading}
          >
            {previewLoading ? "Fetching route options…" : "Preview route options"}
          </button>
          {previewError && <div className="error-text">{previewError}</div>}
          {routeOptions && (
            <div className="stack" style={{ marginTop: 12 }}>
              {routeOptions.map((option) => {
                const isSelected = selectedRoute?.label === option.label;
                const places = placesByLabel[option.label];
                const placesLoading = placesLoadingLabel === option.label;
                return (
                  <div
                    key={option.label}
                    className="card"
                    style={isSelected ? { borderColor: "var(--color-route)", borderWidth: 2 } : undefined}
                  >
                    <div className="between">
                      <strong>{routeLabel(option.label)}</strong>
                      {option.tollFree && <span className="muted">No tolls</span>}
                    </div>
                    <p className="muted" style={{ marginBottom: 4 }}>
                      {(option.distanceMeters / 1000).toFixed(1)} km · ~{Math.round(option.durationSeconds / 60)} min
                    </p>
                    {places === undefined && (
                      <button
                        type="button"
                        className="btn btn-secondary"
                        style={{ marginBottom: 8 }}
                        onClick={() => handleShowPlaces(option)}
                        disabled={placesLoading}
                      >
                        {placesLoading ? "Loading cities…" : "Show cities along this route"}
                      </button>
                    )}
                    {places?.length > 0 && <p>Via: {places.join(" → ")}</p>}
                    {places?.length === 0 && (
                      <p className="muted">City names unavailable right now.</p>
                    )}
                    <button
                      type="button"
                      className={isSelected ? "btn btn-primary" : "btn btn-secondary"}
                      onClick={() => setSelectedRoute(isSelected ? null : option)}
                    >
                      {isSelected ? "Selected" : "Use this route"}
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="field-row">
          <div className="field">
            <label>Departure date</label>
            <input type="date" value={form.departureDate} onChange={update("departureDate")} required />
          </div>
          <div className="field">
            <label>Departure time</label>
            <input type="time" value={form.departureTime} onChange={update("departureTime")} required />
          </div>
          <div className="field" style={{ display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
            <button type="button" className="btn btn-secondary" onClick={setDepartureToNow}>Now</button>
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label>Available seats</label>
            <input type="number" min="1" value={form.availableSeats} onChange={update("availableSeats")} required />
          </div>
          <div className="field">
            <label>Price per seat</label>
            <input type="number" min="0" step="0.01" value={form.pricePerSeat} onChange={update("pricePerSeat")} required />
          </div>
          <div className="field">
            <label>Max detour (km)</label>
            <input type="number" min="0" step="0.5" value={form.maxDetourKm} onChange={update("maxDetourKm")} />
          </div>
        </div>

        <div className="row">
          <label className="checkbox-field"><input type="checkbox" checked={form.luggageAllowed} onChange={update("luggageAllowed")} /> Luggage allowed</label>
          <label className="checkbox-field"><input type="checkbox" checked={form.smokingAllowed} onChange={update("smokingAllowed")} /> Smoking allowed</label>
          <label className="checkbox-field"><input type="checkbox" checked={form.womenOnly} onChange={update("womenOnly")} /> Women only</label>
          <label className="checkbox-field"><input type="checkbox" checked={form.petsAllowed} onChange={update("petsAllowed")} /> Pets allowed</label>
        </div>

        <div className="field">
          <label>Description</label>
          <textarea rows="3" value={form.description} onChange={update("description")} />
        </div>

        {error && <div className="error-text">{error}</div>}
        {status === "published" && <div className="muted" style={{ color: "#3f7a5d" }}>Ride published and now searchable.</div>}

        <button className="btn btn-primary" type="submit" disabled={status === "publishing"}>
          {status === "publishing" ? "Publishing…" : "Publish ride"}
        </button>
      </form>
    </div>
  );
}
