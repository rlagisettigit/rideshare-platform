import { useEffect, useState } from "react";
import { getMyNotifications } from "../api/notifications";

export default function Notifications() {
  const [notifications, setNotifications] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getMyNotifications()
      .then((res) => setNotifications(res.data))
      .catch((err) => setError(err?.message ?? "Could not load notifications."));
  }, []);

  return (
    <div className="stack">
      <div>
        <h1>Notifications</h1>
        <p>Updates about your rides and bookings.</p>
      </div>

      {error && <div className="error-text">{error}</div>}

      {notifications && notifications.length === 0 && (
        <div className="empty-state">No notifications yet.</div>
      )}

      <div className="stack">
        {notifications?.map((n) => (
          <div key={n.id} className="card">
            <div className="between">
              <strong>{n.title}</strong>
              <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
            <p style={{ marginBottom: 0 }}>{n.body}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
