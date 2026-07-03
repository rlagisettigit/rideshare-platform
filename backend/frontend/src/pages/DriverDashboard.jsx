import { useEffect, useState } from "react";
import { onboardDriver, goOnline, goOffline, registerVehicle, getMyVehicles } from "../api/driver";
import { getMyRides, cancelRide, startRide, finishRide } from "../api/rides";
import { getDriverBookingRequests, acceptBooking, rejectBooking, getBooking } from "../api/bookings";
import { getMyWallet, getWalletTransactions } from "../api/wallet";
import Modal from "../components/Modal";

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

export default function DriverDashboard() {
  const [tab, setTab] = useState("rides");
  const [rideStatusTab, setRideStatusTab] = useState("ALL");
  const [rides, setRides] = useState([]);
  const [ridesError, setRidesError] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [requests, setRequests] = useState([]);
  const [requestsError, setRequestsError] = useState(null);
  const [respondingTo, setRespondingTo] = useState(null);
  const [online, setOnline] = useState(false);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [walletError, setWalletError] = useState(null);
  const [viewingTransaction, setViewingTransaction] = useState(null);
  const [transactionBooking, setTransactionBooking] = useState(null);
  const [transactionDetailsError, setTransactionDetailsError] = useState(null);

  const [driverForm, setDriverForm] = useState({
    licenseNumber: "", licenseDocUrl: "", governmentIdType: "", governmentIdDocUrl: "",
    addressProofDocUrl: "", selfieDocUrl: ""
  });
  const [vehicleForm, setVehicleForm] = useState({
    vehicleNumber: "", brand: "", model: "", fuelType: "PETROL", transmission: "MANUAL",
    color: "", seatingCapacity: 4, insuranceExpiry: "", registrationExpiry: ""
  });

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

  useEffect(() => { loadRides(); loadVehicles(); loadRequests(); loadWallet(); }, []);

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
    try {
      await registerVehicle({ ...vehicleForm, seatingCapacity: Number(vehicleForm.seatingCapacity) });
      loadVehicles();
    } catch (err) {
      alert(err?.message ?? "Vehicle registration failed.");
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

  const visibleRides = rideStatusTab === "ALL" ? rides : rides.filter((r) => r.status === rideStatusTab);
  const pendingRequestCount = requests.filter((r) => r.status === "PENDING").length;

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
            <div key={r.publicId} className="card between">
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
                  <button className="btn btn-primary" onClick={() => handleFinish(r.publicId)}>Complete ride</button>
                )}
              </div>
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
          {requests.map((r) => (
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
              </div>
            </div>
          ))}
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

      {tab === "vehicles" && (
        <div className="stack">
          {vehicles.map((v) => (
            <div key={v.id} className="card between">
              <span>{v.brand} {v.model} · {v.vehicleNumber}</span>
              <span className="badge badge-pending">{v.status}</span>
            </div>
          ))}
          <form onSubmit={submitVehicle} className="card stack">
            <h3>Register a vehicle</h3>
            <div className="field-row">
              <div className="field"><label>Vehicle number</label><input value={vehicleForm.vehicleNumber} onChange={(e) => setVehicleForm({ ...vehicleForm, vehicleNumber: e.target.value })} required /></div>
              <div className="field"><label>Seating capacity</label><input type="number" min="1" value={vehicleForm.seatingCapacity} onChange={(e) => setVehicleForm({ ...vehicleForm, seatingCapacity: e.target.value })} required /></div>
            </div>
            <div className="field-row">
              <div className="field"><label>Brand</label><input value={vehicleForm.brand} onChange={(e) => setVehicleForm({ ...vehicleForm, brand: e.target.value })} /></div>
              <div className="field"><label>Model</label><input value={vehicleForm.model} onChange={(e) => setVehicleForm({ ...vehicleForm, model: e.target.value })} /></div>
            </div>
            <div className="field-row">
              <div className="field"><label>Insurance expiry</label><input type="date" value={vehicleForm.insuranceExpiry} onChange={(e) => setVehicleForm({ ...vehicleForm, insuranceExpiry: e.target.value })} /></div>
              <div className="field"><label>Registration expiry</label><input type="date" value={vehicleForm.registrationExpiry} onChange={(e) => setVehicleForm({ ...vehicleForm, registrationExpiry: e.target.value })} /></div>
            </div>
            <button className="btn btn-primary" type="submit">Submit for approval</button>
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
