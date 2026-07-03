import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", mobile: "", password: "", asDriver: false });
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);
    try {
      await register(form);
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
            <label htmlFor="password">Password</label>
            <input id="password" type="password" value={form.password} onChange={update("password")} required />
            {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}
          </div>
          <label className="checkbox-field">
            <input type="checkbox" checked={form.asDriver} onChange={update("asDriver")} />
            I also want to drive and publish rides
          </label>
          {error && <div className="error-text">{error}</div>}
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
