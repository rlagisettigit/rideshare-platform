import { useState } from "react";
import Modal from "./Modal";
import { submitReview } from "../api/reviews";

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

/**
 * target: { ridePublicId, revieweeUserPublicId, revieweeName, title }
 * onSubmitted: called after a successful submit, so callers can refresh their lists.
 */
export default function RatingModal({ target, onClose, onSubmitted }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    if (rating < 1) {
      setError("Please select a star rating.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await submitReview({
        ridePublicId: target.ridePublicId,
        revieweeUserPublicId: target.revieweeUserPublicId,
        rating,
        comment
      });
      onSubmitted();
    } catch (err) {
      setError(err?.message ?? "Could not submit rating.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal title={target.title ?? "Rate"} onClose={onClose}>
      <div className="stack">
        <p className="muted" style={{ marginBottom: 0 }}>{target.revieweeName}</p>
        <StarPicker value={rating} onChange={setRating} />
        <div className="field">
          <label>Comment (optional)</label>
          <textarea rows="3" value={comment} onChange={(e) => setComment(e.target.value)} />
        </div>
        {error && <div className="error-text">{error}</div>}
        <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
          {submitting ? "Submitting…" : "Submit rating"}
        </button>
      </div>
    </Modal>
  );
}
