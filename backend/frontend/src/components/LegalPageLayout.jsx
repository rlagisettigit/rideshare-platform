import { Link } from "react-router-dom";

export default function LegalPageLayout({ title, lastUpdated, children }) {
  return (
    <div className="lp-root">
      <header className="lp-header">
        <div className="lp-header-inner">
          <Link to="/" className="nav-brand" style={{ textDecoration: "none" }}>
            Aura Ride<span className="dot">•</span>
          </Link>
          <nav className="lp-header-links" style={{ justifyContent: "flex-end" }}>
            <Link to="/terms">Terms & Conditions</Link>
            <Link to="/privacy">Privacy Policy</Link>
          </nav>
          <div className="lp-header-actions">
            <Link to="/login" className="btn btn-secondary">Log in</Link>
            <Link to="/register" className="btn btn-primary">Sign up</Link>
          </div>
        </div>
      </header>

      <div className="legal-content">
        <h1>{title}</h1>
        <p className="muted">Last updated: {lastUpdated}</p>
        {children}
      </div>

      <footer className="lp-footer">
        <div className="lp-footer-inner">
          <div className="nav-brand">
            Aura Ride<span className="dot">•</span>
          </div>
          <div className="lp-footer-links">
            <Link to="/terms">Terms & Conditions</Link>
            <Link to="/privacy">Privacy Policy</Link>
            <Link to="/login">Log in</Link>
            <Link to="/register">Sign up</Link>
          </div>
          <span className="muted lp-footer-copyright">© {new Date().getFullYear()} Aura Ride. All rights reserved.</span>
        </div>
      </footer>
    </div>
  );
}
