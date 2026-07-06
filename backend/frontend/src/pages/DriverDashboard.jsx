import { useEffect, useState } from "react";
import { onboardDriver, goOnline, goOffline, registerVehicle, getMyVehicles, updateVehicle, deleteVehicle } from "../api/driver";
import { getVehicleCatalog } from "../api/vehicleCatalog";
import { getMyRides, cancelRide, startRide, finishRide } from "../api/rides";
import { getDriverBookingRequests, acceptBooking, rejectBooking, getBooking, acceptBookingBatch, rejectBookingBatch } from "../api/bookings";
import { getMyWallet, getWalletTransactions } from "../api/wallet";
import { getPendingReviews } from "../api/reviews";
import { postLocation } from "../api/location";
import Modal from "../components/Modal";
import RatingModal from "../components/RatingModal";
import RideStopMap from "../components/RideStopMap";

const RIDE_STATUS_TABS = ["ALL", "PENDING", "ACTIVE", "IN_PROGRESS", "CANCELLED", "FINISHED"];
const RIDE_STATUS_BADGE = {
  PENDING: "badge-pending",
  ACTIVE: "badge-active",
  IN_PROGRESS: "badge-active",
  CANCELLED: "badge-cancelled",
  FINISHED: "badge-active"
};

function rideStatusLabel(status) {
  if (status === "ALL") return "All";
  return status.split("_").map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(" ");
}
const BOOKING_STATUS_BADGE = {
  PENDING: "badge-pending",
  CONFIRMED: "badge-active",
  REJECTED: "badge-cancelled",
  CANCELLED: "badge-cancelled",
  COMPLETED: "badge-active"
};

const EMPTY_VEHICLE_FORM = {
  vehicleNumber: "", brand: "", model: "", category: "", fuelType: "PETROL", transmission: "MANUAL",
  color: "", seatingCapacity: "", insuranceExpiry: "", registrationExpiry: ""
};

const OTHER = "__OTHER__";
const CATEGORY_OPTIONS = ["HATCHBACK", "SEDAN", "SUV", "MUV", "VAN", "COUPE", "PICKUP"];

export default function DriverDashboard() {
  const [tab, setTab] = useState("rides");
  const [rideStatusTab, setRideStatusTab] = useState("ALL");
  const [rides, setRides] = useState([]);
  const [ridesError, setRidesError] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [requests, setRequests] = useState([]);
  const [requestsError, setRequestsError] = useState(null);
  const [respondingTo, setRespondingTo] = useState(null);
  const [respondingBatch, setRespondingBatch] = useState(null);
  const [online, setOnline] = useState(false);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [walletError, setWalletError] = useState(null);
  const [viewingTransaction, setViewingTransaction] = useState(null);
  const [transactionBooking, setTransactionBooking] = useState(null);
  const [transactionDetailsError, setTransactionDetailsError] = useState(null);
  const [pendingReviews, setPendingReviews] = useState([]);
  const [viewingStopsFor, setViewingStopsFor] = useState(null);
  const [ratingTarget, setRatingTarget] = useState(null);

  const [driverForm, setDriverForm] = useState({
    licenseNumber: "", licenseDocUrl: "", governmentIdType: "", governmentIdDocUrl: "",
    addressProofDocUrl: "", selfieDocUrl: ""
  });
  const [vehicleForm, setVehicleForm] = useState(EMPTY_VEHICLE_FORM);
  const [editingVehicleId, setEditingVehicleId] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [otherBrand, setOtherBrand] = useState(false);
  const [otherModel, setOtherModel] = useState(false);

  const loadRides = () => {
    setRidesError(null);
    getMyRides()
      .then((res) => setRides(res.data))
      .catch((err) => setRidesError(err?.message ?? "Could not load your rides."));
  };
  const loadVehicles = () => getMyVehicles().then((res) => setVehicles(res.data)).catch(() => {});
  const loadRequests = () => {
    setRequestsError(null);
    getDriverBookingRequests()
      .then((res) => setRequests(res.data))
      .catch((err) => setRequestsError(err?.message ?? "Could not load booking requests."));
  };
  const loadWallet = () => {
    setWalletError(null);
    Promise.all([getMyWallet(), getWalletTransactions()])
      .then(([walletRes, txRes]) => {
        setWallet(walletRes.data);
        setTransactions(txRes.data);
      })
      .catch((err) => setWalletError(err?.message ?? "Could not load wallet."));
  };
  const loadPendingReviews = () => {
    getPendingReviews().then((res) => setPendingReviews(res.data)).catch(() => {});
  };

  useEffect(() => {
    loadRides(); loadVehicles(); loadRequests(); loadWallet(); loadPendingReviews();
    getVehicleCatalog().then((res) => setCatalog(res.data)).catch(() => {});
  }, []);

  // Broadcasts this driver's live position while any of their rides is IN_PROGRESS, throttled
  // client-side since watchPosition can fire far more often than passengers need to see updates.
  useEffect(() => {
    const inProgressRides = rides.filter((r) => r.status === "IN_PROGRESS");
    if (inProgressRides.length === 0 || !navigator.geolocation) return;

    let lastSentAt = 0;
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        const now = Date.now();
        if (now - lastSentAt < 5000) return;
        lastSentAt = now;
        const { latitude, longitude, heading } = position.coords;
        inProgressRides.forEach((r) => {
          postLocation(r.publicId, { lat: latitude, lng: longitude, heading: heading ?? null }).catch(() => {});
        });
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 4000, timeout: 10000 }
    );

    return () => navigator.geolocation.clearWatch(watchId);
  }, [rides]);

  const toggleOnline = async () => {
    try {
      if (online) await goOffline(); else await goOnline();
      setOnline(!online);
    } catch (err) {
      alert(err?.message ?? "Driver profile must be verified first.");
    }
  };

  const submitDriverOnboard = async (e) => {
    e.preventDefault();
    try {
      await onboardDriver(driverForm);
      alert("Submitted for KYC verification.");
    } catch (err) {
      alert(err?.message ?? "Onboarding failed.");
    }
  };

  const submitVehicle = async (e) => {
    e.preventDefault();
    const payload = { ...vehicleForm, seatingCapacity: Number(vehicleForm.seatingCapacity) };
    try {
      if (editingVehicleId) {
        await updateVehicle(editingVehicleId, payload);
      } else {
        await registerVehicle(payload);
      }
      setVehicleForm(EMPTY_VEHICLE_FORM);
      setEditingVehicleId(null);
      setOtherBrand(false);
      setOtherModel(false);
      loadVehicles();
    } catch (err) {
      alert(err?.message ?? (editingVehicleId ? "Could not update vehicle." : "Vehicle registration failed."));
    }
  };

  const handleEditVehicle = (v) => {
    const catalogHasBrand = catalog.some((c) => c.brand === v.brand);
    const catalogHasModel = catalog.some((c) => c.brand === v.brand && c.model === v.model);
    setOtherBrand(!catalogHasBrand);
    setOtherModel(!catalogHasModel);
    setEditingVehicleId(v.id);
    setVehicleForm({
      vehicleNumber: v.vehicleNumber ?? "",
      brand: v.brand ?? "",
      model: v.model ?? "",
      category: v.category ?? "",
      fuelType: v.fuelType ?? "PETROL",
      transmission: v.transmission ?? "MANUAL",
      color: v.color ?? "",
      seatingCapacity: v.seatingCapacity ?? "",
      insuranceExpiry: v.insuranceExpiry ?? "",
      registrationExpiry: v.registrationExpiry ?? ""
    });
  };

  // Selecting a brand clears any previously chosen model/category/seats, since those only
  // make sense paired with a specific catalog entry.
  const handleBrandChange = (brand) => {
    setVehicleForm({ ...vehicleForm, brand, model: "", category: "", seatingCapacity: "" });
  };

  // Category and seating capacity are catalog-derived, not manually entered, so picking a
  // model fills both in from the matching catalog row.
  const handleModelChange = (model) => {
    const entry = catalog.find((c) => c.brand === vehicleForm.brand && c.model === model);
    setVehicleForm({
      ...vehicleForm,
      model,
      category: entry?.category ?? "",
      seatingCapacity: entry?.seats ?? ""
    });
  };

  // "Other" switches brand/model to free-text entry for a vehicle the catalog doesn't have -
  // there's then no catalog row to derive category/seats from, so those become manual too.
  const handleBrandSelect = (value) => {
    if (value === OTHER) {
      setOtherBrand(true);
      setOtherModel(true);
      setVehicleForm((f) => ({ ...f, brand: "", model: "", category: "", seatingCapacity: "" }));
    } else {
      setOtherBrand(false);
      setOtherModel(false);
      handleBrandChange(value);
    }
  };

  const handleModelSelect = (value) => {
    if (value === OTHER) {
      setOtherModel(true);
      setVehicleForm((f) => ({ ...f, model: "", category: "", seatingCapacity: "" }));
    } else {
      setOtherModel(false);
      handleModelChange(value);
    }
  };

  const handleCancelEditVehicle = () => {
    setEditingVehicleId(null);
    setVehicleForm(EMPTY_VEHICLE_FORM);
    setOtherBrand(false);
    setOtherModel(false);
  };

  const handleDeleteVehicle = async (v) => {
    if (!confirm(`Delete ${v.brand} ${v.model} (${v.vehicleNumber})?`)) return;
    try {
      await deleteVehicle(v.id);
      if (editingVehicleId === v.id) handleCancelEditVehicle();
      loadVehicles();
    } catch (err) {
      alert(err?.message ?? "Could not delete this vehicle.");
    }
  };

  const handleStart = async (publicId) => {
    try {
      await startRide(publicId);
      loadRides();
    } catch (err) {
      alert(err?.message ?? "Could not start this ride.");
    }
  };

  const handleFinish = async (publicId) => {
    if (!confirm("Mark this ride as completed? This will settle fares for confirmed bookings.")) return;
    try {
      await finishRide(publicId);
      loadRides();
      loadRequests();
      loadWallet();
      loadPendingReviews();
    } catch (err) {
      alert(err?.message ?? "Could not complete this ride.");
    }
  };

  const handleViewTransaction = async (t) => {
    setViewingTransaction(t);
    setTransactionBooking(null);
    setTransactionDetailsError(null);
    if (!t.reference) {
      setTransactionDetailsError("No ride is linked to this transaction.");
      return;
    }
    try {
      const res = await getBooking(t.reference);
      setTransactionBooking(res.data);
    } catch (err) {
      setTransactionDetailsError(err?.message ?? "Could not load ride details.");
    }
  };

  const respond = async (publicId, accept) => {
    setRespondingTo(publicId);
    try {
      if (accept) await acceptBooking(publicId); else await rejectBooking(publicId);
      loadRequests();
      loadRides();
    } catch (err) {
      alert(err?.message ?? "Could not respond to this request.");
    } finally {
      setRespondingTo(null);
    }
  };

  // Bookings created together via a passenger's "book upcoming dates" action on a recurring
  // ride share a bookingBatchId - respond to the whole batch in one action instead of per date.
  const respondBatch = async (batchId, accept) => {
    setRespondingBatch(batchId);
    try {
      const res = accept ? await acceptBookingBatch(batchId) : await rejectBookingBatch(batchId);
      const { succeeded, requested, failures } = res.data;
      let message = `${accept ? "Accepted" : "Rejected"} ${succeeded} of ${requested} booking${requested === 1 ? "" : "s"} in this batch.`;
      if (failures.length > 0) message += `\nIssues:\n${failures.join("\n")}`;
      alert(message);
      loadRequests();
      loadRides();
    } catch (err) {
      alert(err?.message ?? "Could not respond to this batch.");
    } finally {
      setRespondingBatch(null);
    }
  };

  const visibleRides = rideStatusTab === "ALL" ? rides : rides.filter((r) => r.status === rideStatusTab);
  const pendingRequestCount = requests.filter((r) => r.status === "PENDING").length;

  // Requests from the same "book upcoming dates" action share a bookingBatchId - group them
  // into one card instead of showing one row per date.
  const groupedRequests = [];
  const seenBatches = new Set();
  requests.forEach((r) => {
    if (r.bookingBatchId) {
      if (seenBatches.has(r.bookingBatchId)) return;
      seenBatches.add(r.bookingBatchId);
      groupedRequests.push({ type: "batch", batchId: r.bookingBatchId, items: requests.filter((x) => x.bookingBatchId === r.bookingBatchId) });
    } else {
      groupedRequests.push({ type: "single", item: r });
    }
  });

  const catalogBrands = [...new Set(catalog.map((c) => c.brand))].sort();
  const catalogModelsForBrand = catalog.filter((c) => c.brand === vehicleForm.brand);

  // A ride can have multiple passengers, so a pending RATE_PASSENGER entry must be matched
  // on both the ride and the specific passenger, not the ride alone.
  const pendingPassengerRating = (ridePublicId, passengerUserPublicId) =>
    pendingReviews.find(
      (p) => p.direction === "RATE_PASSENGER" && p.ridePublicId === ridePublicId && p.revieweeUserPublicId === passengerUserPublicId
    );

  const handleRatingSubmitted = () => {
    setRatingTarget(null);
    loadPendingReviews();
  };

  return (
    <div className="stack">
      <div className="between">
        <div>
          <h1>Driver dashboard</h1>
          <p>Manage KYC, vehicles, availability, and your published rides.</p>
        </div>
        <button className={`btn ${online ? "btn-danger" : "btn-primary"}`} onClick={toggleOnline}>
          {online ? "Go offline" : "Go online"}
        </button>
      </div>

      <div className="row" style={{ borderBottom: "1px solid var(--color-line)", marginBottom: 4 }}>
        {["rides", "requests", "wallet", "vehicles", "kyc"].map((t) => (
          <button
            key={t}
            className="btn btn-secondary"
            style={{ border: "none", borderBottom: tab === t ? "2px solid var(--color-route)" : "2px solid transparent", borderRadius: 0 }}
            onClick={() => setTab(t)}
          >
            {t === "rides" ? "My rides"
              : t === "requests" ? `Booking requests${pendingRequestCount > 0 ? ` (${pendingRequestCount})` : ""}`
              : t === "wallet" ? "Wallet"
              : t === "vehicles" ? "Vehicles" : "KYC"}
          </button>
        ))}
      </div>

      {tab === "rides" && (
        <div className="stack">
          {ridesError && <div className="error-text">{ridesError}</div>}
          <div className="row">
            {RIDE_STATUS_TABS.map((s) => (
              <button
                key={s}
                className="btn btn-secondary"
                style={{
                  padding: "4px 12px",
                  background: rideStatusTab === s ? "var(--color-route)" : undefined,
                  color: rideStatusTab === s ? "white" : undefined
                }}
                onClick={() => setRideStatusTab(s)}
              >
                {rideStatusLabel(s)}
              </button>
            ))}
          </div>

          {visibleRides.length === 0 && <div className="empty-state">No rides in this status.</div>}
          {visibleRides.map((r) => (
            <div key={r.publicId} className="card stack">
              <div className="between">
                <div>
                  <div className="route-line">
                    <span className="stop">{r.originAddress}</span>
                    <span className="path" />
                    <span className="stop">{r.destinationAddress}</span>
                  </div>
                  <span className="muted">{new Date(r.departureAt).toLocaleString()} · {r.availableSeats} seats left</span>
                </div>
                <div className="row">
                  <span className={`badge ${RIDE_STATUS_BADGE[r.status] ?? "badge-pending"}`}>{r.status}</span>
                  {r.status === "ACTIVE" && (
                    <>
                      <button className="btn btn-primary" onClick={() => handleStart(r.publicId)}>Start ride</button>
                      <button className="btn btn-secondary" onClick={() => cancelRide(r.publicId).then(loadRides)}>Cancel</button>
                    </>
                  )}
                  {r.status === "IN_PROGRESS" && (
                    <>
                      <button
                        className="btn btn-secondary"
                        onClick={() => setViewingStopsFor(viewingStopsFor === r.publicId ? null : r.publicId)}
                      >
                        {viewingStopsFor === r.publicId ? "Hide route plan" : "View route plan"}
                      </button>
                      <button className="btn btn-primary" onClick={() => handleFinish(r.publicId)}>Complete ride</button>
                    </>
                  )}
                </div>
              </div>
              {viewingStopsFor === r.publicId && <RideStopMap ridePublicId={r.publicId} showNavigation />}
            </div>
          ))}
        </div>
      )}

      {tab === "requests" && (
        <div className="stack">
          {requestsError && <div className="error-text">{requestsError}</div>}
          {requests.length === 0 && !requestsError && (
            <div className="empty-state">No booking requests yet.</div>
          )}
          {groupedRequests.map((group) => {
            if (group.type === "single") {
              const r = group.item;
              const ratable = r.status === "COMPLETED" ? pendingPassengerRating(r.ridePublicId, r.passengerUserPublicId) : null;
              return (
                <div key={r.publicId} className="card between">
                  <div>
                    <div className="row" style={{ marginBottom: 4 }}>
                      <strong>{r.passengerName}</strong>
                      <span className="muted">on {r.rideOriginAddress} → {r.rideDestinationAddress}</span>
                    </div>
                    <div className="route-line">
                      <span className="stop">{r.pickupAddress ?? "Pickup"}</span>
                      <span className="path" />
                      <span className="stop">{r.dropAddress ?? "Drop"}</span>
                    </div>
                    <span className="muted">
                      {r.seatsBooked} seat{r.seatsBooked === 1 ? "" : "s"} · ₹{Number(r.fare).toFixed(0)}
                      {r.rideDepartureAt && <> · {new Date(r.rideDepartureAt).toLocaleString()}</>}
                    </span>
                  </div>
                  <div className="row">
                    <span className={`badge ${BOOKING_STATUS_BADGE[r.status] ?? "badge-pending"}`}>{r.status}</span>
                    {r.status === "PENDING" && (
                      <>
                        <button
                          className="btn btn-primary"
                          disabled={respondingTo === r.publicId}
                          onClick={() => respond(r.publicId, true)}
                        >
                          Accept
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={respondingTo === r.publicId}
                          onClick={() => respond(r.publicId, false)}
                        >
                          Reject
                        </button>
                      </>
                    )}
                    {ratable && (
                      <button
                        className="btn btn-primary"
                        onClick={() => setRatingTarget({ ...ratable, title: "Rate your passenger" })}
                      >
                        Rate passenger
                      </button>
                    )}
                  </div>
                </div>
              );
            }

            const first = group.items[0];
            const pendingCount = group.items.filter((it) => it.status === "PENDING").length;
            return (
              <div key={group.batchId} className="card stack">
                <div className="row" style={{ marginBottom: 4 }}>
                  <strong>{first.passengerName}</strong>
                  <span className="muted">on {first.rideOriginAddress} → {first.rideDestinationAddress}</span>
                  <span className="badge badge-active">Recurring · {group.items.length} dates</span>
                </div>
                <div className="stack" style={{ gap: 4 }}>
                  {group.items.map((it) => {
                    const ratable = it.status === "COMPLETED" ? pendingPassengerRating(it.ridePublicId, it.passengerUserPublicId) : null;
                    return (
                      <div key={it.publicId} className="between">
                        <span className="muted">
                          {it.rideDepartureAt && new Date(it.rideDepartureAt).toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })}
                        </span>
                        <div className="row">
                          <span className={`badge ${BOOKING_STATUS_BADGE[it.status] ?? "badge-pending"}`}>{it.status}</span>
                          {ratable && (
                            <button className="btn btn-secondary" onClick={() => setRatingTarget({ ...ratable, title: "Rate your passenger" })}>
                              Rate
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
                {pendingCount > 0 && (
                  <div className="row">
                    <button
                      className="btn btn-primary"
                      disabled={respondingBatch === group.batchId}
                      onClick={() => respondBatch(group.batchId, true)}
                    >
                      {respondingBatch === group.batchId ? "Responding…" : `Accept all (${pendingCount})`}
                    </button>
                    <button
                      className="btn btn-secondary"
                      disabled={respondingBatch === group.batchId}
                      onClick={() => respondBatch(group.batchId, false)}
                    >
                      Reject all
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {tab === "wallet" && (
        <div className="stack">
          {walletError && <div className="error-text">{walletError}</div>}
          <div className="card">
            <p className="muted" style={{ marginBottom: 4 }}>Balance</p>
            <h2 style={{ margin: 0 }}>₹{Number(wallet?.balance ?? 0).toFixed(2)}</h2>
          </div>
          {transactions.length === 0 && !walletError && (
            <div className="empty-state">No transactions yet. Earnings appear here once a ride is completed.</div>
          )}
          {transactions.map((t) => (
            <div
              key={t.id}
              className="card between"
              style={{ cursor: "pointer" }}
              onClick={() => handleViewTransaction(t)}
            >
              <div>
                <strong>{t.reason ?? t.type}</strong>
                <div className="muted">{new Date(t.createdAt).toLocaleString()}</div>
              </div>
              <span className={t.type === "CREDIT" ? "badge badge-active" : "badge badge-cancelled"}>
                {t.type === "CREDIT" ? "+" : "-"}₹{Number(t.amount).toFixed(2)}
              </span>
            </div>
          ))}
        </div>
      )}

      {viewingTransaction && (
        <Modal title="Ride details" onClose={() => setViewingTransaction(null)}>
          {transactionDetailsError && <div className="error-text">{transactionDetailsError}</div>}
          {!transactionBooking && !transactionDetailsError && <p className="muted">Loading…</p>}
          {transactionBooking && (
            <div className="stack">
              <div className="route-line">
                <span className="stop">{transactionBooking.rideOriginAddress}</span>
                <span className="path" />
                <span className="stop">{transactionBooking.rideDestinationAddress}</span>
              </div>
              <div><strong>Passenger:</strong> {transactionBooking.passengerName}</div>
              <div><strong>Date:</strong> {new Date(transactionBooking.rideDepartureAt).toLocaleString()}</div>
              <div><strong>Amount:</strong> ₹{Number(transactionBooking.fare).toFixed(2)}</div>
            </div>
          )}
        </Modal>
      )}

      {ratingTarget && (
        <RatingModal target={ratingTarget} onClose={() => setRatingTarget(null)} onSubmitted={handleRatingSubmitted} />
      )}

      {tab === "vehicles" && (
        <div className="stack">
          {vehicles.map((v) => (
            <div key={v.id} className="card between">
              <span>{v.brand} {v.model}{v.category ? ` · ${v.category}` : ""} · {v.vehicleNumber} · {v.seatingCapacity} seats</span>
              <div className="row">
                <span className="badge badge-pending">{v.status}</span>
                <button className="btn btn-secondary" onClick={() => handleEditVehicle(v)}>Edit</button>
                <button className="btn btn-danger" onClick={() => handleDeleteVehicle(v)}>Delete</button>
              </div>
            </div>
          ))}
          <form onSubmit={submitVehicle} className="card stack">
            <h3>{editingVehicleId ? "Edit vehicle" : "Register a vehicle"}</h3>
            <div className="field-row">
              <div className="field">
                <label>Brand</label>
                {otherBrand ? (
                  <div className="row">
                    <input
                      value={vehicleForm.brand}
                      onChange={(e) => setVehicleForm({ ...vehicleForm, brand: e.target.value })}
                      placeholder="Enter brand name"
                      required
                    />
                    <button type="button" className="btn btn-secondary" onClick={() => handleBrandSelect("")}>
                      Choose from list
                    </button>
                  </div>
                ) : (
                  <select value={vehicleForm.brand} onChange={(e) => handleBrandSelect(e.target.value)} required>
                    <option value="">Select brand</option>
                    {catalogBrands.map((brand) => <option key={brand} value={brand}>{brand}</option>)}
                    <option value={OTHER}>Other (enter manually)</option>
                  </select>
                )}
              </div>
              <div className="field">
                <label>Model</label>
                {otherModel ? (
                  <div className="row">
                    <input
                      value={vehicleForm.model}
                      onChange={(e) => setVehicleForm({ ...vehicleForm, model: e.target.value })}
                      placeholder="Enter model name"
                      required
                    />
                    {!otherBrand && (
                      <button type="button" className="btn btn-secondary" onClick={() => handleModelSelect("")}>
                        Choose from list
                      </button>
                    )}
                  </div>
                ) : (
                  <select
                    value={vehicleForm.model}
                    onChange={(e) => handleModelSelect(e.target.value)}
                    disabled={!vehicleForm.brand}
                    required
                  >
                    <option value="">{vehicleForm.brand ? "Select model" : "Select a brand first"}</option>
                    {catalogModelsForBrand.map((c) => <option key={c.id} value={c.model}>{c.model}</option>)}
                    <option value={OTHER}>Other (enter manually)</option>
                  </select>
                )}
              </div>
            </div>
            <div className="field-row">
              <div className="field">
                <label>Category</label>
                {otherBrand || otherModel ? (
                  <select value={vehicleForm.category} onChange={(e) => setVehicleForm({ ...vehicleForm, category: e.target.value })} required>
                    <option value="">Select category</option>
                    {CATEGORY_OPTIONS.map((c) => <option key={c} value={c}>{c}</option>)}
                  </select>
                ) : (
                  <input value={vehicleForm.category} disabled placeholder="Auto-filled from model" />
                )}
              </div>
              <div className="field">
                <label>Seating capacity</label>
                {otherBrand || otherModel ? (
                  <input
                    type="number"
                    min="1"
                    value={vehicleForm.seatingCapacity}
                    onChange={(e) => setVehicleForm({ ...vehicleForm, seatingCapacity: e.target.value })}
                    required
                  />
                ) : (
                  <input value={vehicleForm.seatingCapacity} disabled placeholder="Auto-filled from model" />
                )}
              </div>
            </div>
            <div className="field-row">
              <div className="field"><label>Vehicle number</label><input value={vehicleForm.vehicleNumber} onChange={(e) => setVehicleForm({ ...vehicleForm, vehicleNumber: e.target.value })} required /></div>
              <div className="field"><label>Color</label><input value={vehicleForm.color} onChange={(e) => setVehicleForm({ ...vehicleForm, color: e.target.value })} /></div>
            </div>
            <div className="field-row">
              <div className="field"><label>Insurance expiry</label><input type="date" value={vehicleForm.insuranceExpiry} onChange={(e) => setVehicleForm({ ...vehicleForm, insuranceExpiry: e.target.value })} /></div>
              <div className="field"><label>Registration expiry</label><input type="date" value={vehicleForm.registrationExpiry} onChange={(e) => setVehicleForm({ ...vehicleForm, registrationExpiry: e.target.value })} /></div>
            </div>
            <div className="row">
              <button className="btn btn-primary" type="submit">{editingVehicleId ? "Save changes" : "Submit for approval"}</button>
              {editingVehicleId && (
                <button type="button" className="btn btn-secondary" onClick={handleCancelEditVehicle}>Cancel</button>
              )}
            </div>
          </form>
        </div>
      )}

      {tab === "kyc" && (
        <form onSubmit={submitDriverOnboard} className="card stack">
          <h3>Driver verification</h3>
          <p className="muted">Upload documents to storage first, then paste the resulting URLs below.</p>
          <div className="field"><label>License number</label><input value={driverForm.licenseNumber} onChange={(e) => setDriverForm({ ...driverForm, licenseNumber: e.target.value })} required /></div>
          <div className="field"><label>License document URL</label><input value={driverForm.licenseDocUrl} onChange={(e) => setDriverForm({ ...driverForm, licenseDocUrl: e.target.value })} required /></div>
          <div className="field"><label>Government ID type</label><input value={driverForm.governmentIdType} onChange={(e) => setDriverForm({ ...driverForm, governmentIdType: e.target.value })} required /></div>
          <div className="field"><label>Government ID document URL</label><input value={driverForm.governmentIdDocUrl} onChange={(e) => setDriverForm({ ...driverForm, governmentIdDocUrl: e.target.value })} required /></div>
          <div className="field"><label>Address proof URL</label><input value={driverForm.addressProofDocUrl} onChange={(e) => setDriverForm({ ...driverForm, addressProofDocUrl: e.target.value })} required /></div>
          <div className="field"><label>Selfie URL</label><input value={driverForm.selfieDocUrl} onChange={(e) => setDriverForm({ ...driverForm, selfieDocUrl: e.target.value })} required /></div>
          <button className="btn btn-primary" type="submit">Submit for verification</button>
        </form>
      )}
    </div>
  );
}
