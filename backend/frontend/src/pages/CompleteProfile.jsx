import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { updateMyProfile } from "../api/users";

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

export default function CompleteProfile() {
  const { refreshProfileStatus } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ mobile: "", gender: "", dob: "" });
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    const clientErrors = {};
    if (!form.gender) clientErrors.gender = "Please select an option.";
    if (!isAtLeast18(form.dob)) {
      clientErrors.dob = `You must be at least ${MINIMUM_AGE_YEARS} years old to use Waypoint.`;
    }
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setError("Please fix the highlighted fields.");
      return;
    }

    setLoading(true);
    try {
      await updateMyProfile(form);
      await refreshProfileStatus();
      navigate("/search");
    } catch (err) {
      if (err?.fieldErrors?.length) {
        setFieldErrors(Object.fromEntries(err.fieldErrors.map((fe) => [fe.field, fe.message])));
        setError(err.message ?? "Please fix the highlighted fields.");
      } else {
        setError(err?.message ?? err?.error ?? "Could not save your details. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h2>Just a few more details</h2>
        <p className="muted">
          Your Google account didn't include these - we need them before you can book or publish rides.
        </p>
        <form onSubmit={handleSubmit} className="stack">
          <div className="field">
            <label htmlFor="mobile">Mobile</label>
            <input id="mobile" value={form.mobile} onChange={update("mobile")} placeholder="+15551234567" required />
            {fieldErrors.mobile && <div className="field-error">{fieldErrors.mobile}</div>}
          </div>
          <div className="field">
            <label htmlFor="gender">Gender</label>
            <select id="gender" value={form.gender} onChange={update("gender")} required>
              <option value="" disabled>Select one</option>
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
          {error && <div className="error-text">{error}</div>}
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? "Saving…" : "Continue"}
          </button>
        </form>
      </div>
    </div>
  );
}
