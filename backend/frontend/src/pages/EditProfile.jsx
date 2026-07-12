import { useEffect, useState } from "react";
import { getMyProfile, updateMyProfile } from "../api/users";
import PageHeader from "../components/PageHeader";
import bannerGps from "../assets/images/banner-gps.jpg";

const MINIMUM_AGE_YEARS = 18;

function maxDobForMinimumAge() {
  const d = new Date();
  d.setFullYear(d.getFullYear() - MINIMUM_AGE_YEARS);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function isAtLeast18(dob) {
  if (!dob) return true; // dob is optional on edit - only validate when the user sets/changes it
  const birth = new Date(dob);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const hasHadBirthdayThisYear =
    today.getMonth() > birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate());
  if (!hasHadBirthdayThisYear) age -= 1;
  return age >= MINIMUM_AGE_YEARS;
}

const ROLE_LABELS = { rolePassenger: "Passenger", roleDriver: "Driver", roleAdmin: "Admin" };

export default function EditProfile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getMyProfile()
      .then((res) => {
        setProfile(res.data);
        setForm({
          name: res.data.name ?? "",
          gender: res.data.gender ?? "",
          dob: res.data.dob ?? "",
          preferredLanguage: res.data.preferredLanguage ?? "en",
          profilePhotoUrl: res.data.profilePhotoUrl ?? ""
        });
      })
      .catch((err) => setLoadError(err?.message ?? "Could not load your profile."));
  }, []);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setSaved(false);

    if (!isAtLeast18(form.dob)) {
      setFieldErrors({ dob: `You must be at least ${MINIMUM_AGE_YEARS} years old.` });
      setError("Please fix the highlighted fields.");
      return;
    }

    setSaving(true);
    try {
      const res = await updateMyProfile({ ...form, dob: form.dob || null });
      setProfile(res.data);
      setSaved(true);
    } catch (err) {
      if (err?.fieldErrors?.length) {
        setFieldErrors(Object.fromEntries(err.fieldErrors.map((fe) => [fe.field, fe.message])));
        setError(err.message ?? "Please fix the highlighted fields.");
      } else {
        setError(err?.message ?? err?.error ?? "Could not save your profile.");
      }
    } finally {
      setSaving(false);
    }
  };

  if (loadError) {
    return (
      <div className="stack">
        <PageHeader image={bannerGps} title="Edit profile" description="Update your personal details." />
        <div className="error-text">{loadError}</div>
      </div>
    );
  }

  if (!form) {
    return (
      <div className="stack">
        <PageHeader image={bannerGps} title="Edit profile" description="Update your personal details." />
      </div>
    );
  }

  const activeRoles = Object.keys(ROLE_LABELS).filter((key) => profile[key]);

  return (
    <div className="stack">
      <PageHeader image={bannerGps} title="Edit profile" description="Update your personal details." />

      <div className="card">
        <div className="row" style={{ gap: 8, marginBottom: 16 }}>
          {activeRoles.map((key) => (
            <span key={key} className="badge badge-active">{ROLE_LABELS[key]}</span>
          ))}
          {profile.averageRating != null && (
            <span className="muted">★ {Number(profile.averageRating).toFixed(1)}</span>
          )}
        </div>

        <form onSubmit={handleSubmit} className="stack">
          <div className="field">
            <label>Email</label>
            <input value={profile.email} disabled />
          </div>
          <div className="field">
            <label>Mobile</label>
            <input value={profile.mobile ?? "Not set"} disabled />
          </div>

          <div className="field">
            <label htmlFor="name">Full name</label>
            <input id="name" value={form.name} onChange={update("name")} required />
            {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}
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
            <input id="dob" type="date" max={maxDobForMinimumAge()} value={form.dob} onChange={update("dob")} />
            {fieldErrors.dob && <div className="field-error">{fieldErrors.dob}</div>}
          </div>
          <div className="field">
            <label htmlFor="preferredLanguage">Preferred language</label>
            <select id="preferredLanguage" value={form.preferredLanguage} onChange={update("preferredLanguage")}>
              <option value="en">English</option>
              <option value="hi">Hindi</option>
              <option value="te">Telugu</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="profilePhotoUrl">Profile photo URL</label>
            <input id="profilePhotoUrl" value={form.profilePhotoUrl} onChange={update("profilePhotoUrl")} placeholder="https://…" />
            {fieldErrors.profilePhotoUrl && <div className="field-error">{fieldErrors.profilePhotoUrl}</div>}
          </div>

          {error && <div className="error-text">{error}</div>}
          {saved && !error && <div className="muted">Profile updated.</div>}
          <button className="btn btn-primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : "Save changes"}
          </button>
        </form>
      </div>
    </div>
  );
}
