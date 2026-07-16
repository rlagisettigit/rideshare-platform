import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { updateMyProfile } from "../api/users";
import LegalModal from "../components/legal/LegalModal";
import carpoolFriends from "../assets/images/carpool-friends.jpg";

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
  const [openLegalDoc, setOpenLegalDoc] = useState(null); // null | "terms" | "privacy"
  const [viewedTerms, setViewedTerms] = useState(false);
  const [viewedPrivacy, setViewedPrivacy] = useState(false);
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const bothViewed = viewedTerms && viewedPrivacy;

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const openLegal = (doc) => {
    setOpenLegalDoc(doc);
    if (doc === "terms") setViewedTerms(true);
    else setViewedPrivacy(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    const clientErrors = {};
    if (!form.gender) clientErrors.gender = "Please select an option.";
    if (!isAtLeast18(form.dob)) {
      clientErrors.dob = `You must be at least ${MINIMUM_AGE_YEARS} years old to use Aura Ride.`;
    }
    if (!agreedToTerms) {
      clientErrors.terms = "Please read and agree to the Terms & Conditions and Privacy Policy.";
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
    <div className="auth-split-shell">
      <div className="auth-split-visual">
        <img src={carpoolFriends} alt="" className="auth-split-visual-bg" aria-hidden="true" />
        <div className="auth-split-visual-overlay" />

        <div className="auth-split-visual-content">
          <Link to="/" className="nav-brand" style={{ textDecoration: "none" }}>
            Aura Ride<span className="dot">•</span>
          </Link>
        </div>

        <div className="auth-split-visual-content">
          <div className="auth-split-visual-quote">
            <p style={{ marginBottom: "var(--space-2)" }}>You're almost in.</p>
            <div style={{ fontFamily: "var(--font-body)", fontSize: "var(--text-base)", opacity: 0.9 }}>
              A few more details and you're ready to book a seat or publish your first ride.
            </div>
          </div>
        </div>
      </div>

      <div className="auth-split-form-panel">
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
              <span className="muted">You must be at least {MINIMUM_AGE_YEARS} to use Aura Ride.</span>
              {fieldErrors.dob && <div className="field-error">{fieldErrors.dob}</div>}
            </div>

            <div className="field" style={{ marginTop: 8 }}>
              <label className="checkbox-field terms-agree-label" title={bothViewed ? undefined : "Open both links first"}>
                <input
                  type="checkbox"
                  checked={agreedToTerms}
                  disabled={!bothViewed}
                  onChange={(e) => { setAgreedToTerms(e.target.checked); setFieldErrors((fe) => ({ ...fe, terms: undefined })); }}
                />
                <span>
                  I agree to the{" "}
                  <button type="button" className="link-button" onClick={() => openLegal("terms")}>Terms & Conditions</button>
                  {" "}and{" "}
                  <button type="button" className="link-button" onClick={() => openLegal("privacy")}>Privacy Policy</button>
                </span>
              </label>
              {!bothViewed && (
                <span className="muted" style={{ fontSize: "var(--text-xs)" }}>
                  Open both links above to enable this checkbox.
                </span>
              )}
              {fieldErrors.terms && <div className="field-error">{fieldErrors.terms}</div>}
            </div>

            {error && <div className="error-text">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading || !agreedToTerms}>
              {loading ? "Saving…" : "Continue"}
            </button>
          </form>
        </div>
      </div>

      {openLegalDoc && <LegalModal doc={openLegalDoc} onClose={() => setOpenLegalDoc(null)} />}
    </div>
  );
}
