import { useEffect, useState } from "react";
import client from "../api/client";
import PageHeader from "../components/PageHeader";
import bannerAnalytics from "../assets/images/banner-analytics.jpg";

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    client.get("/admin/dashboard/summary")
      .then((res) => setSummary(res.data))
      .catch((err) => setError(err?.message ?? "Admin access required."));
  }, []);

  return (
    <div className="stack">
      <PageHeader image={bannerAnalytics} title="Admin" description="Platform-wide operational summary." />

      {error && <div className="card"><span className="error-text">{error}</span></div>}

      {summary && (
        <div className="field-row">
          <div className="card" style={{ flex: 1 }}>
            <span className="muted">Total users</span>
            <h2>{summary.totalUsers}</h2>
          </div>
          <div className="card" style={{ flex: 1 }}>
            <span className="muted">Pending driver verifications</span>
            <h2>{summary.pendingDriverVerifications}</h2>
          </div>
          <div className="card" style={{ flex: 1 }}>
            <span className="muted">Active rides</span>
            <h2>{summary.activeRides}</h2>
          </div>
        </div>
      )}
    </div>
  );
}
