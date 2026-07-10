import { useEffect, useState } from "react";
import { getPendingReviews, getReceivedReviews } from "../api/reviews";
import RatingModal from "../components/RatingModal";
import PageHeader from "../components/PageHeader";
import bannerRating from "../assets/images/banner-rating.jpg";

const DIRECTION_LABEL = {
  RATE_DRIVER: "Rate your driver",
  RATE_PASSENGER: "Rate your passenger"
};

export default function Ratings() {
  const [pending, setPending] = useState([]);
  const [pendingError, setPendingError] = useState(null);
  const [received, setReceived] = useState([]);
  const [receivedError, setReceivedError] = useState(null);
  const [target, setTarget] = useState(null);

  const loadPending = () => {
    setPendingError(null);
    getPendingReviews()
      .then((res) => setPending(res.data))
      .catch((err) => setPendingError(err?.message ?? "Could not load pending ratings."));
  };
  const loadReceived = () => {
    setReceivedError(null);
    getReceivedReviews()
      .then((res) => setReceived(res.data))
      .catch((err) => setReceivedError(err?.message ?? "Could not load your reviews."));
  };

  useEffect(() => { loadPending(); loadReceived(); }, []);

  const openRatingModal = (item) => {
    setTarget({ ...item, title: DIRECTION_LABEL[item.direction] ?? "Rate" });
  };

  const handleSubmitted = () => {
    setTarget(null);
    loadPending();
    loadReceived();
  };

  const avgRating = received.length
    ? (received.reduce((sum, r) => sum + r.rating, 0) / received.length).toFixed(1)
    : null;

  return (
    <div className="stack">
      <PageHeader image={bannerRating} title="Ratings" description="Rate drivers and passengers after a completed ride, and see the reviews you've received." />

      <div>
        <h3>Pending ratings</h3>
        {pendingError && <div className="error-text">{pendingError}</div>}
        {pending.length === 0 && !pendingError && (
          <div className="empty-state">Nothing to rate right now.</div>
        )}
        <div className="stack">
          {pending.map((item) => (
            <div key={`${item.ridePublicId}-${item.revieweeUserPublicId}`} className="card between">
              <div>
                <strong>{DIRECTION_LABEL[item.direction] ?? "Rate"}: {item.revieweeName}</strong>
                <div className="route-line">
                  <span className="stop">{item.rideOriginAddress}</span>
                  <span className="path" />
                  <span className="stop">{item.rideDestinationAddress}</span>
                </div>
                <span className="muted">{new Date(item.rideDepartureAt).toLocaleString()}</span>
              </div>
              <button className="btn btn-primary" onClick={() => openRatingModal(item)}>Rate</button>
            </div>
          ))}
        </div>
      </div>

      <div>
        <div className="between">
          <h3>Reviews you've received</h3>
          {avgRating && <span className="muted">Average: {avgRating} ★ ({received.length})</span>}
        </div>
        {receivedError && <div className="error-text">{receivedError}</div>}
        {received.length === 0 && !receivedError && (
          <div className="empty-state">No reviews yet.</div>
        )}
        <div className="stack">
          {received.map((r) => (
            <div key={r.id} className="card">
              <div className="between" style={{ marginBottom: 4 }}>
                <strong>{r.reviewerName}</strong>
                <span>{"★".repeat(r.rating)}{"☆".repeat(5 - r.rating)}</span>
              </div>
              {r.comment && <p className="muted" style={{ marginBottom: 4 }}>{r.comment}</p>}
              <span className="muted">{new Date(r.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>
      </div>

      {target && (
        <RatingModal target={target} onClose={() => setTarget(null)} onSubmitted={handleSubmitted} />
      )}
    </div>
  );
}
