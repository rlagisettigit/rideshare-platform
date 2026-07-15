import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import GoogleSignInButton from "../components/GoogleSignInButton";
import authDriverNav from "../assets/images/auth-driver-nav.jpg";

export default function Login() {
  const { login, refreshProfileStatus } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login({ mode: "EMAIL_PASSWORD", email, password });
      navigate("/search");
    } catch (err) {
      // AUTH_003 covers both a genuinely wrong password and an email that only has a Google/Apple
      // account (no password set at all - see AuthService.provisionSocialUser). The backend can't
      // distinguish these in the message without leaking which accounts exist, so the hint below
      // is shown unconditionally on this error rather than being a confirmed diagnosis.
      if (err?.errorCode === "AUTH_003") {
        setError('Invalid email or password. If you originally signed up with Google, use "Continue with Google" above instead of a password.');
      } else {
        setError(err?.message ?? "Unable to sign in. Check your details and try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleCredential = async (idToken) => {
    setError(null);
    setLoading(true);
    try {
      await login({ mode: "GOOGLE", idToken });
      const complete = await refreshProfileStatus();
      navigate(complete === false ? "/complete-profile" : "/search");
    } catch (err) {
      setError(err?.message ?? "Google sign-in failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-split-shell">
      <div className="auth-split-visual">
        <img src={authDriverNav} alt="" className="auth-split-visual-bg" aria-hidden="true" />
        <div className="auth-split-visual-overlay" />

        <div className="auth-split-visual-content">
          <Link to="/" className="nav-brand" style={{ textDecoration: "none" }}>
            Waypoint<span className="dot">•</span>
          </Link>
        </div>

        <div className="auth-split-visual-content">
          <div className="auth-split-visual-quote">
            <p style={{ marginBottom: "var(--space-2)" }}>Welcome back to the road.</p>
            <div style={{ fontFamily: "var(--font-body)", fontSize: "var(--text-base)", opacity: 0.9 }}>
              Sign in to track your ride live, message your driver, and manage upcoming bookings.
            </div>
          </div>
        </div>
      </div>

      <div className="auth-split-form-panel">
        <div className="auth-card">
          <h2>Sign in to Waypoint</h2>
          <p className="muted">Book a seat or publish a ride along your route.</p>
          <GoogleSignInButton onCredential={handleGoogleCredential} text="signin_with" />
          <div className="auth-divider">or sign in with email</div>
          {error && <div className="error-text">{error}</div>}
          <form onSubmit={handleSubmit} className="stack">
            <div className="field">
              <label htmlFor="email">Email</label>
              <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </div>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? "Signing in…" : "Sign in"}
            </button>
          </form>
          <p className="muted" style={{ marginTop: 16 }}>
            New here? <Link to="/register">Create an account</Link>
          </p>
          <p className="muted" style={{ marginTop: 4, fontSize: "var(--text-xs)" }}>
            By continuing, you agree to our <Link to="/terms">Terms & Conditions</Link> and{" "}
            <Link to="/privacy">Privacy Policy</Link>.
          </p>
        </div>
      </div>
    </div>
  );
}
