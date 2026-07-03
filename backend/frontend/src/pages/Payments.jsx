import { useEffect, useState } from "react";
import { getMyPayments } from "../api/payments";

const STATUS_CLASS = {
  SUCCESS: "badge-active",
  INITIATED: "badge-pending",
  FAILED: "badge-cancelled",
  REFUNDED: "badge-cancelled"
};

export default function Payments() {
  const [payments, setPayments] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getMyPayments()
      .then((res) => setPayments(res.data))
      .catch((err) => setError(err?.message ?? "Could not load payments."));
  }, []);

  return (
    <div className="stack">
      <div>
        <h1>Payments</h1>
        <p>Fares charged for your completed rides.</p>
      </div>

      {error && <div className="error-text">{error}</div>}

      {payments && payments.length === 0 && (
        <div className="empty-state">No payments yet.</div>
      )}

      <div className="stack">
        {payments?.map((p) => (
          <div key={p.id} className="card between">
            <div>
              <strong>₹{Number(p.amount).toFixed(2)}</strong>
              <div className="muted">{p.provider} · {new Date(p.createdAt).toLocaleString()}</div>
            </div>
            <span className={`badge ${STATUS_CLASS[p.status] ?? "badge-pending"}`}>{p.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
