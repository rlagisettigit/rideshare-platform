import { useEffect, useState } from "react";
import { getPendingReviews, getReceivedReviews, submitReview } from "../api/reviews";
import Modal from "../components/Modal";

const DIRECTION_LABEL = {
  RATE_DRIVER: "Rate your driver",
  RATE_PASSENGER: "Rate your passenger"
};

function StarPicker({ value, onChange }) {
  return (
    <div className="row" style={{ gap: 4 }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span
          key={n}
          onClick={() => onChange(n)}
          style={{ cursor: "pointer", fontSize: 28, color: n <= value ? "var(--color-signal)" : "var(--color-line)" }}
        >
          ★
        </span>
      ))}
    </div>
  );
}

export default function Ratings() {
  const [pending, setPending] = useState([]);
  const [pendingError, setPendingError] = useState(null);
  const [received, setReceived] = useState([]);
  const [receivedError, setReceivedError] = useState(null);
  const [target, setTarget] = useState(null);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

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
    setTarget(item);
    setRating(0);
    setComment("");
    setSubmitError(null);
  };

  const handleSubmit = async () => {
    if (rating < 1) {
      setSubmitError("Please select a star rating.");
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      await submitReview({
        ridePublicId: target.ridePublicId,
        revieweeUserPublicId: target.revieweeUserPublicId,
        rating,
        comment
      });
      setTarget(null);
      loadPending();
      loadReceived();
    } catch (err) {
      setSubmitError(err?.message ?? "Could not submit rating.");
    } finally {
      setSubmitting(false);
    }
  };

  const avgRating = received.length
    ? (received.reduce((sum, r) => sum + r.rating, 0) / received.length).toFixed(1)
    : null;

  return (
    <div className="stack">
      <div>
        <h1>Ratings</h1>
        <p>Rate drivers and passengers after a completed ride, and see the reviews you've received.</p>
      </div>

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
        <Modal title={DIRECTION_LABEL[target.direction] ?? "Rate"} onClose={() => setTarget(null)}>
          <div className="stack">
            <p className="muted" style={{ marginBottom: 0 }}>{target.revieweeName}</p>
            <StarPicker value={rating} onChange={setRating} />
            <div className="field">
              <label>Comment (optional)</label>
              <textarea rows="3" value={comment} onChange={(e) => setComment(e.target.value)} />
            </div>
            {submitError && <div className="error-text">{submitError}</div>}
            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
              {submitting ? "Submitting…" : "Submit rating"}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
