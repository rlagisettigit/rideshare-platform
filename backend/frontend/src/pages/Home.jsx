import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getMyBookings, getDriverBookingRequests } from "../api/bookings";
import { getMyRides } from "../api/rides";
import { getMyWallet } from "../api/wallet";
import EmergencyContacts from "../components/EmergencyContacts";
import PageHeader from "../components/PageHeader";
import bannerRoad from "../assets/images/banner-road.jpg";

function timeOfDayGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function isUpcomingBooking(b) {
  return (b.status === "PENDING" || b.status === "CONFIRMED") && b.rideStatus !== "FINISHED" && b.rideStatus !== "CANCELLED";
}

/** label/value only - no delta/trend, since there's no historical baseline to compare
 *  against here. Value stays in the ordinary ink text token rather than a data color:
 *  it's a plain count, not a status/series identity, so it doesn't earn color. */
function StatTile({ label, value, badge }) {
  return (
    <div className="card" style={{ padding: "var(--space-4) var(--space-5)" }}>
      <div className="between" style={{ alignItems: "flex-start" }}>
        <span className="muted">{label}</span>
        {badge}
      </div>
      <div style={{ fontFamily: "var(--font-display)", fontSize: "var(--text-2xl)", fontWeight: 600, marginTop: "var(--space-1)" }}>
        {value}
      </div>
    </div>
  );
}

function ActionCard({ to, title, description }) {
  return (
    <Link to={to} className="card stack" style={{ textDecoration: "none", gap: "var(--space-1)" }}>
      <strong style={{ color: "var(--color-ink)" }}>{title}</strong>
      <span className="muted">{description}</span>
    </Link>
  );
}

export default function Home() {
  const { isDriver, isAdmin } = useAuth();
  const [upcomingBookings, setUpcomingBookings] = useState(null);
  const [pendingRequests, setPendingRequests] = useState(null);
  const [walletBalance, setWalletBalance] = useState(null);
  const [activeRides, setActiveRides] = useState(null);

  useEffect(() => {
    getMyBookings()
      .then((res) => setUpcomingBookings(res.data.filter(isUpcomingBooking).length))
      .catch(() => setUpcomingBookings(0));

    if (isDriver) {
      getDriverBookingRequests()
        .then((res) => setPendingRequests(res.data.filter((r) => r.status === "PENDING").length))
        .catch(() => setPendingRequests(0));
      getMyWallet()
        .then((res) => setWalletBalance(Number(res.data?.balance ?? 0)))
        .catch(() => setWalletBalance(0));
      getMyRides()
        .then((res) => setActiveRides(res.data.filter((r) => r.status === "ACTIVE" || r.status === "IN_PROGRESS").length))
        .catch(() => setActiveRides(0));
    }
  }, [isDriver]);

  return (
    <div className="stack">
      <PageHeader
        image={bannerRoad}
        kicker={timeOfDayGreeting()}
        title="Welcome to Aura Ride"
        description="Find a ride, publish one of your own, or keep tabs on the trips you've already got lined up - everything's one click away below."
      />

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "var(--space-4)" }}>
        <StatTile label="Upcoming bookings" value={upcomingBookings ?? "–"} />
        {isDriver && (
          <StatTile
            label="Pending requests"
            value={pendingRequests ?? "–"}
            badge={pendingRequests > 0 ? <span className="badge badge-pending">Needs reply</span> : null}
          />
        )}
        {isDriver && <StatTile label="Active rides" value={activeRides ?? "–"} />}
        {isDriver && <StatTile label="Wallet balance" value={walletBalance != null ? `₹${walletBalance.toFixed(2)}` : "–"} />}
      </div>

      <div className="card">
        <h3>Emergency contacts</h3>
        <EmergencyContacts />
      </div>

      <div>
        <h3>Quick actions</h3>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "var(--space-4)" }}>
          <ActionCard to="/search" title="Find a ride" description="Search rides along your route and request a seat." />
          <ActionCard to="/bookings" title="My bookings" description="Track requests, confirmations, and past rides." />
          <ActionCard to="/recurring-rides" title="Recurring rides" description="See every date of a recurring trip grouped as one thread." />
          {isDriver && <ActionCard to="/publish" title="Publish a ride" description="Offer seats on a route you're already driving." />}
          {isDriver && <ActionCard to="/driver" title="Driver dashboard" description="Manage vehicles, bookings, and your wallet." />}
          {isAdmin && <ActionCard to="/admin" title="Admin" description="Platform oversight and moderation tools." />}
          <ActionCard to="/payments" title="Payments" description="Review fares, refunds, and transaction history." />
          <ActionCard to="/ratings" title="Ratings" description="See feedback from drivers and passengers." />
        </div>
      </div>
    </div>
  );
}
