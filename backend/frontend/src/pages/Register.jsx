import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import GoogleSignInButton from "../components/GoogleSignInButton";

const MINIMUM_AGE_YEARS = 18;

function maxDobForMinimumAge() {
  const d = new Date();
  d.setFullYear(d.getFullYear() - MINIMUM_AGE_YEARS);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function isAtLeast18(dob) {
  if (!dob) return false;
  const birth = new Date(dob);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const hasHadBirthdayThisYear =
    today.getMonth() > birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate());
  if (!hasHadBirthdayThisYear) age -= 1;
  return age >= MINIMUM_AGE_YEARS;
}

export default function Register() {
  const { register, login, refreshProfileStatus } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "", email: "", mobile: "", password: "", confirmPassword: "",
    gender: "", dob: "", asDriver: false
  });
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const handleGoogleCredential = async (idToken) => {
    setError(null);
    setLoading(true);
    try {
      await login({ mode: "GOOGLE", idToken });
      const complete = await refreshProfileStatus();
      navigate(complete === false ? "/complete-profile" : "/search");
    } catch (err) {
      setError(err?.message ?? "Google sign-up failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    const clientErrors = {};
    if (form.password !== form.confirmPassword) {
      clientErrors.confirmPassword = "Passwords don't match.";
    }
    if (!isAtLeast18(form.dob)) {
      clientErrors.dob = `You must be at least ${MINIMUM_AGE_YEARS} years old to register.`;
    }
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setError("Please fix the highlighted fields.");
      return;
    }

    setLoading(true);
    try {
      const { confirmPassword, ...payload } = form;
      await register(payload);
      navigate("/search");
    } catch (err) {
      if (err?.fieldErrors?.length) {
        setFieldErrors(Object.fromEntries(err.fieldErrors.map((fe) => [fe.field, fe.message])));
        setError(err.message ?? "Please fix the highlighted fields.");
      } else {
        setError(err?.message ?? err?.error ?? "Registration failed. Please check your details.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h2>Create your account</h2>
        <p className="muted">Passwords need 8+ characters with a letter and a digit.</p>
        <GoogleSignInButton onCredential={handleGoogleCredential} text="signup_with" />
        <div className="auth-divider">or sign up with email</div>
        {error && <div className="error-text">{error}</div>}
        <form onSubmit={handleSubmit} className="stack">
          <div className="field">
            <label htmlFor="name">Full name</label>
            <input id="name" value={form.name} onChange={update("name")} required />
            {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}
          </div>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={form.email} onChange={update("email")} required />
            {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}
          </div>
          <div className="field">
            <label htmlFor="mobile">Mobile</label>
            <input id="mobile" value={form.mobile} onChange={update("mobile")} placeholder="+15551234567" required />
            {fieldErrors.mobile && <div className="field-error">{fieldErrors.mobile}</div>}
          </div>
          <div className="field">
            <label htmlFor="gender">Gender</label>
            <select id="gender" value={form.gender} onChange={update("gender")}>
              <option value="">Prefer not to say</option>
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="OTHER">Other</option>
            </select>
            {fieldErrors.gender && <div className="field-error">{fieldErrors.gender}</div>}
          </div>
          <div className="field">
            <label htmlFor="dob">Date of birth</label>
            <input id="dob" type="date" max={maxDobForMinimumAge()} value={form.dob} onChange={update("dob")} required />
            <span className="muted">You must be at least {MINIMUM_AGE_YEARS} to use Waypoint.</span>
            {fieldErrors.dob && <div className="field-error">{fieldErrors.dob}</div>}
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" value={form.password} onChange={update("password")} required />
            {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}
          </div>
          <div className="field">
            <label htmlFor="confirmPassword">Confirm password</label>
            <input id="confirmPassword" type="password" value={form.confirmPassword} onChange={update("confirmPassword")} required />
            {fieldErrors.confirmPassword && <div className="field-error">{fieldErrors.confirmPassword}</div>}
          </div>
          <label className="checkbox-field">
            <input type="checkbox" checked={form.asDriver} onChange={update("asDriver")} />
            I also want to drive and publish rides
          </label>
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? "Creating account…" : "Create account"}
          </button>
        </form>
        <p className="muted" style={{ marginTop: 16 }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
