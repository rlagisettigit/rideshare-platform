import { useState } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// Standalone items render as a single top-level link; grouped items collapse into a dropdown
// under their group label. `roles` gates an entire entry (standalone or group) - omitted means
// visible to any authenticated user. Kept in sync with the route guards in App.jsx and the
// backend's @PreAuthorize rules.
const NAV_ENTRIES = [
  { standalone: true, to: "/", label: "Home", end: true },
  {
    label: "Passenger",
    items: [
      { to: "/search", label: "Find a ride" },
      { to: "/bookings", label: "My bookings" },
      { to: "/recurring-rides", label: "Recurring rides" }
    ]
  },
  {
    label: "Driver",
    roles: ["DRIVER"],
    items: [
      { to: "/publish", label: "Publish a ride" },
      { to: "/driver", label: "Driver dashboard" }
    ]
  },
  {
    label: "Account",
    items: [
      { to: "/profile", label: "Edit profile" },
      { to: "/payments", label: "Payments" },
      { to: "/ratings", label: "Ratings" },
      { to: "/notifications", label: "Notifications" }
    ]
  },
  { standalone: true, to: "/admin", label: "Admin", roles: ["ADMIN"] }
];

function NavGroup({ label, items }) {
  const [open, setOpen] = useState(false);

  return (
    <div
      className="nav-group"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        className={"nav-group-trigger" + (open ? " active" : "")}
        onClick={() => setOpen((v) => !v)}
        onFocus={() => setOpen(true)}
        aria-expanded={open}
      >
        {label} <span className="nav-caret">▾</span>
      </button>
      {open && (
        <div className="nav-dropdown">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => "nav-dropdown-link" + (isActive ? " active" : "")}
              onClick={() => setOpen(false)}
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  );
}

export default function TopNav() {
  const { logout, roles: userRoles } = useAuth();
  const visibleEntries = NAV_ENTRIES.filter((entry) => !entry.roles || entry.roles.some((r) => userRoles.includes(r)));

  return (
    <nav className="top-nav">
      <div className="nav-brand">
        Waypoint<span className="dot">•</span>
      </div>
      <div className="nav-groups">
        {visibleEntries.map((entry) =>
          entry.standalone ? (
            <NavLink
              key={entry.to}
              to={entry.to}
              end={entry.end}
              className={({ isActive }) => "nav-link" + (isActive ? " active" : "")}
            >
              {entry.label}
            </NavLink>
          ) : (
            <NavGroup key={entry.label} label={entry.label} items={entry.items} />
          )
        )}
      </div>
      <div className="nav-footer">
        <a href="#" onClick={(e) => { e.preventDefault(); logout(); }} style={{ color: "#d6cfc0" }}>
          Sign out
        </a>
      </div>
    </nav>
  );
}
