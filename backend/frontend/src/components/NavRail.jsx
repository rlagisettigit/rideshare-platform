import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const LINKS = [
  { to: "/search", label: "Find a ride" },
  { to: "/bookings", label: "My bookings" },
  { to: "/payments", label: "Payments" },
  { to: "/ratings", label: "Ratings" },
  { to: "/notifications", label: "Notifications" },
  { to: "/publish", label: "Publish a ride" },
  { to: "/recurring-rides", label: "Recurring rides" },
  { to: "/driver", label: "Driver dashboard" },
  { to: "/admin", label: "Admin" }
];

export default function NavRail() {
  const { logout } = useAuth();

  return (
    <nav className="nav-rail">
      <div className="nav-brand">
        Waypoint<span className="dot">•</span>
      </div>
      <div className="nav-links">
        {LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => "nav-link" + (isActive ? " active" : "")}
          >
            {link.label}
          </NavLink>
        ))}
      </div>
      <div className="nav-footer">
        <a href="#" onClick={(e) => { e.preventDefault(); logout(); }} style={{ color: "#c8cad6" }}>
          Sign out
        </a>
      </div>
    </nav>
  );
}
