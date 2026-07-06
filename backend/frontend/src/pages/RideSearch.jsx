import { useState } from "react";
import { searchRides } from "../api/rides";
import { createBooking } from "../api/bookings";
import RideCard from "../components/RideCard";
import AddressAutocomplete from "../components/AddressAutocomplete";
import MapLocationPicker from "../components/MapLocationPicker";
import RecurringBookingModal from "../components/RecurringBookingModal";

function todayDateString() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export default function RideSearch() {
  const [form, setForm] = useState({
    pickupAddress: "", pickupLat: "", pickupLng: "",
    dropAddress: "", dropLat: "", dropLng: "",
    travelDate: "", passengers: 1, sortBy: "EARLIEST_DEPARTURE"
  });
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [bookingRideId, setBookingRideId] = useState(null);
  const [recurringPickerId, setRecurringPickerId] = useState(null);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const setTravelDateToNow = () => setForm((f) => ({ ...f, travelDate: todayDateString() }));

  const handleSearch = async (e) => {
    e.preventDefault();
    setError(null);
    if (!form.pickupLat || !form.pickupLng) {
      setError("Please select the pickup location from the suggestions list.");
      return;
    }
    if (!form.dropLat || !form.dropLng) {
      setError("Please select the drop location from the suggestions list.");
      return;
    }
    setLoading(true);
    try {
      const res = await searchRides({
        pickupLat: parseFloat(form.pickupLat),
        pickupLng: parseFloat(form.pickupLng),
        dropLat: parseFloat(form.dropLat),
        dropLng: parseFloat(form.dropLng),
        travelDate: form.travelDate,
        passengers: Number(form.passengers),
        page: 0,
        size: 20,
        sortBy: form.sortBy
      });
      setResults(res.data);
    } catch (err) {
      setError(err?.message ?? "Search failed. Please check your inputs.");
    } finally {
      setLoading(false);
    }
  };

  const handleBook = async (ridePublicId) => {
    setBookingRideId(ridePublicId);
    try {
      await createBooking({
        ridePublicId,
        pickupLat: parseFloat(form.pickupLat),
        pickupLng: parseFloat(form.pickupLng),
        pickupAddress: form.pickupAddress,
        dropLat: parseFloat(form.dropLat),
        dropLng: parseFloat(form.dropLng),
        dropAddress: form.dropAddress,
        seats: Number(form.passengers)
      });
      alert("Booking requested — the driver will confirm shortly.");
    } catch (err) {
      alert(err?.message ?? "Booking failed.");
    } finally {
      setBookingRideId(null);
    }
  };

  const handleRecurringBooked = (summary) => {
    setRecurringPickerId(null);
    const { booked, requested, failures } = summary;
    let message = `Requested booking on ${booked} of ${requested} selected date${requested === 1 ? "" : "s"}.`;
    if (failures.length > 0) {
      message += `\nCouldn't book:\n${failures.join("\n")}`;
    }
    alert(message);
  };

  return (
    <div className="stack">
      <div>
        <h1>Find a ride</h1>
        <p>Search rides between any two points along a driver's route.</p>
      </div>

      <form onSubmit={handleSearch} className="card stack">
        <div className="field-row" style={{ alignItems: "flex-end" }}>
          <AddressAutocomplete
            label="Pickup location"
            value={form.pickupAddress}
            onChange={update("pickupAddress")}
            onPlaceSelect={({ address, lat, lng }) =>
              setForm((f) => ({ ...f, pickupAddress: address, pickupLat: lat, pickupLng: lng }))
            }
            placeholder="Start typing an address…"
            required
          />
          <div className="field" style={{ flex: "none" }}>
            <MapLocationPicker
              label="Pick pickup on map"
              initialLat={form.pickupLat ? parseFloat(form.pickupLat) : undefined}
              initialLng={form.pickupLng ? parseFloat(form.pickupLng) : undefined}
              useCurrentLocation
              onSelect={({ address, lat, lng }) =>
                setForm((f) => ({ ...f, pickupAddress: address, pickupLat: lat, pickupLng: lng }))
              }
            />
          </div>
        </div>
        <div className="field-row" style={{ alignItems: "flex-end" }}>
          <AddressAutocomplete
            label="Drop location"
            value={form.dropAddress}
            onChange={update("dropAddress")}
            onPlaceSelect={({ address, lat, lng }) =>
              setForm((f) => ({ ...f, dropAddress: address, dropLat: lat, dropLng: lng }))
            }
            placeholder="Start typing an address…"
            required
          />
          <div className="field" style={{ flex: "none" }}>
            <MapLocationPicker
              label="Pick drop on map"
              initialLat={form.dropLat ? parseFloat(form.dropLat) : undefined}
              initialLng={form.dropLng ? parseFloat(form.dropLng) : undefined}
              onSelect={({ address, lat, lng }) =>
                setForm((f) => ({ ...f, dropAddress: address, dropLat: lat, dropLng: lng }))
              }
            />
          </div>
        </div>
        <div className="field-row">
          <div className="field">
            <label>Travel date</label>
            <input type="date" value={form.travelDate} onChange={update("travelDate")} required />
          </div>
          <div className="field" style={{ display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
            <button type="button" className="btn btn-secondary" onClick={setTravelDateToNow}>Now</button>
          </div>
          <div className="field">
            <label>Passengers</label>
            <input type="number" min="1" value={form.passengers} onChange={update("passengers")} required />
          </div>
          <div className="field">
            <label>Sort by</label>
            <select value={form.sortBy} onChange={update("sortBy")}>
              <option value="EARLIEST_DEPARTURE">Earliest departure</option>
              <option value="NEAREST_PICKUP">Nearest pickup</option>
              <option value="LOWEST_DETOUR">Lowest detour</option>
              <option value="DRIVER_RATING">Driver rating</option>
              <option value="RIDE_PRICE">Price</option>
            </select>
          </div>
        </div>
        {error && <div className="error-text">{error}</div>}
        <button className="btn btn-primary" type="submit" disabled={loading}>
          {loading ? "Searching…" : "Search rides"}
        </button>
      </form>

      {results && (
        <div className="stack">
          <h3>{results.length} ride{results.length === 1 ? "" : "s"} found</h3>
          {results.length === 0 && (
            <div className="empty-state">No rides match this route and time window yet. Try a wider date range.</div>
          )}
          {results.map((ride) => (
            <RideCard
              key={ride.ridePublicId}
              ride={{ ...ride, originAddress: form.pickupAddress || "Pickup on route", destinationAddress: form.dropAddress || "Drop on route" }}
              action={
                <div className="row">
                  <button
                    className="btn btn-primary"
                    onClick={() => handleBook(ride.ridePublicId)}
                    disabled={bookingRideId === ride.ridePublicId}
                  >
                    {bookingRideId === ride.ridePublicId ? "Requesting…" : "Request booking"}
                  </button>
                  {ride.recurringRidePublicId && (
                    <button
                      className="btn btn-secondary"
                      onClick={() => setRecurringPickerId(ride.recurringRidePublicId)}
                      title="This trip repeats on a schedule - pick which upcoming dates to book"
                    >
                      Book upcoming dates
                    </button>
                  )}
                </div>
              }
            />
          ))}
        </div>
      )}

      {recurringPickerId && (
        <RecurringBookingModal
          recurringRidePublicId={recurringPickerId}
          pickup={{
            pickupLat: parseFloat(form.pickupLat),
            pickupLng: parseFloat(form.pickupLng),
            pickupAddress: form.pickupAddress,
            dropLat: parseFloat(form.dropLat),
            dropLng: parseFloat(form.dropLng),
            dropAddress: form.dropAddress,
            seats: Number(form.passengers)
          }}
          onClose={() => setRecurringPickerId(null)}
          onBooked={handleRecurringBooked}
        />
      )}
    </div>
  );
}
