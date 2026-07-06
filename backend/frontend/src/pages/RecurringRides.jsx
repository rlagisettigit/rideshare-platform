import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createRecurringRide, getMyRecurringRides, cancelRecurringRide } from "../api/recurringRides";
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

const DAYS = [
  { value: "MONDAY", label: "Mon" },
  { value: "TUESDAY", label: "Tue" },
  { value: "WEDNESDAY", label: "Wed" },
  { value: "THURSDAY", label: "Thu" },
  { value: "FRIDAY", label: "Fri" },
  { value: "SATURDAY", label: "Sat" },
  { value: "SUNDAY", label: "Sun" }
];
const WEEKDAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"];

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

const EMPTY_FORM = {
  vehicleId: "", originAddress: "", originLat: "", originLng: "",
  destinationAddress: "", destinationLat: "", destinationLng: "",
  daysOfWeek: [...WEEKDAYS], departureTime: "", startDate: todayIso(), endDate: "",
  availableSeats: "", pricePerSeat: "",
  luggageAllowed: true, smokingAllowed: false, womenOnly: false, petsAllowed: false,
  description: "", maxDetourKm: 5
};

export default function RecurringRides() {
  const [vehicles, setVehicles] = useState([]);
  const [series, setSeries] = useState(null);
  const [seriesError, setSeriesError] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);
  const [routeOptions, setRouteOptions] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [placesByLabel, setPlacesByLabel] = useState({});
  const [placesLoadingLabel, setPlacesLoadingLabel] = useState(null);

  const loadSeries = () => {
    getMyRecurringRides()
      .then((res) => setSeries(res.data))
      .catch((err) => setSeriesError(err?.message ?? "Could not load recurring rides."));
  };

  useEffect(() => {
    getMyVehicles().then((res) => setVehicles(res.data)).catch(() => {});
    loadSeries();
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

  const toggleDay = (day) => {
    setForm((f) => ({
      ...f,
      daysOfWeek: f.daysOfWeek.includes(day) ? f.daysOfWeek.filter((d) => d !== day) : [...f.daysOfWeek, day]
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
    if (form.daysOfWeek.length === 0) {
      setError("Select at least one day of the week.");
      return;
    }
    if (!form.endDate) {
      setError("Select an end date for this series.");
      return;
    }
    setStatus("publishing");
    try {
      await createRecurringRide({
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
      setForm(EMPTY_FORM);
      setRouteOptions(null);
      setSelectedRoute(null);
      loadSeries();
    } catch (err) {
      setError(err?.message ?? "Could not create this recurring ride.");
      setStatus(null);
    }
  };

  const handleCancelSeries = async (publicId) => {
    if (!confirm("Cancel this recurring ride? All not-yet-started occurrences will be cancelled.")) return;
    try {
      await cancelRecurringRide(publicId);
      loadSeries();
    } catch (err) {
      alert(err?.message ?? "Could not cancel this recurring ride.");
    }
  };

  return (
    <div className="stack">
      <div>
        <Link to="/recurring-rides">← Back to recurring rides overview</Link>
        <h1>Publish a recurring ride</h1>
        <p>Offer the same trip on a repeating schedule - each date books independently, just like a one-off ride.</p>
      </div>

      {vehicles.length === 0 && (
        <div className="card">
          <p style={{ marginBottom: 0 }}>
            No approved vehicles yet. Register a vehicle from the Driver dashboard before creating a recurring ride.
          </p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="card stack">
        <div className="field">
          <label>Vehicle</label>
          <select value={form.vehicleId} onChange={update("vehicleId")} required>
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

        <div className="field">
          <label>Days of week</label>
          <div className="row">
            {DAYS.map((d) => (
              <button
                key={d.value}
                type="button"
                className="btn btn-secondary"
                style={{
                  padding: "4px 12px",
                  background: form.daysOfWeek.includes(d.value) ? "var(--color-route)" : undefined,
                  color: form.daysOfWeek.includes(d.value) ? "white" : undefined
                }}
                onClick={() => toggleDay(d.value)}
              >
                {d.label}
              </button>
            ))}
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label>Departure time</label>
            <input type="time" value={form.departureTime} onChange={update("departureTime")} required />
          </div>
          <div className="field">
            <label>Start date</label>
            <input type="date" value={form.startDate} onChange={update("startDate")} min={todayIso()} required />
          </div>
          <div className="field">
            <label>End date</label>
            <input type="date" value={form.endDate} onChange={update("endDate")} min={form.startDate} required />
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
        {status === "published" && <div className="muted" style={{ color: "#3f7a5d" }}>Recurring ride created - occurrences are now searchable.</div>}

        <button className="btn btn-primary" type="submit" disabled={status === "publishing"}>
          {status === "publishing" ? "Creating…" : "Create recurring ride"}
        </button>
      </form>

      <div>
        <h3>Your recurring rides</h3>
        {seriesError && <div className="error-text">{seriesError}</div>}
        {series && series.length === 0 && (
          <div className="empty-state">No recurring rides yet.</div>
        )}
        <div className="stack">
          {series?.map((s) => (
            <div key={s.publicId} className="card between">
              <div>
                <div className="route-line">
                  <span className="stop">{s.originAddress}</span>
                  <span className="path" />
                  <span className="stop">{s.destinationAddress}</span>
                </div>
                <span className="muted">
                  {s.daysOfWeek.map((d) => d.slice(0, 3)).join(", ")} at {s.departureTime} · {s.startDate} → {s.endDate}
                </span>
                <div className="muted">{s.availableSeats} seats · ₹{Number(s.pricePerSeat).toFixed(0)} · {s.upcomingCount} of {s.occurrenceCount} occurrences upcoming</div>
              </div>
              <div className="row">
                <span className={`badge ${s.status === "ACTIVE" ? "badge-active" : "badge-cancelled"}`}>{s.status}</span>
                {s.status === "ACTIVE" && (
                  <button className="btn btn-secondary" onClick={() => handleCancelSeries(s.publicId)}>Cancel series</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
