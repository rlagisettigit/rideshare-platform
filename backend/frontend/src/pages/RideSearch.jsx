import { useState } from "react";
import { searchRides } from "../api/rides";
import { createBooking } from "../api/bookings";
import RideCard from "../components/RideCard";
import AddressAutocomplete from "../components/AddressAutocomplete";

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

  return (
    <div className="stack">
      <div>
        <h1>Find a ride</h1>
        <p>Search rides between any two points along a driver's route.</p>
      </div>

      <form onSubmit={handleSearch} className="card stack">
        <div className="field-row">
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
        </div>
        <div className="field-row">
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
                <button
                  className="btn btn-primary"
                  onClick={() => handleBook(ride.ridePublicId)}
                  disabled={bookingRideId === ride.ridePublicId}
                >
                  {bookingRideId === ride.ridePublicId ? "Requesting…" : "Request booking"}
                </button>
              }
            />
          ))}
        </div>
      )}
    </div>
  );
}
