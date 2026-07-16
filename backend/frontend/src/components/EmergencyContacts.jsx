const EMERGENCY_CONTACTS = [
  { icon: "🚓", label: "Police", number: "100", type: "emergency" },
  { icon: "🚒", label: "Fire", number: "101", type: "emergency" },
  { icon: "🚑", label: "Ambulance", number: "108", type: "emergency" },
  { icon: "☎️", label: "Aura Ride Support", number: "1800-123-4567", type: "internal" }
];

export default function EmergencyContacts({ className = "" }) {
  return (
    <div className={`ec-grid ${className}`.trim()}>
      {EMERGENCY_CONTACTS.map((c) => (
        <a
          key={c.label}
          href={`tel:${c.number.replace(/[^0-9+]/g, "")}`}
          className={`ec-item ec-item-${c.type}`}
        >
          <span className="ec-icon">{c.icon}</span>
          <span className="ec-label">{c.label}</span>
          <span className="ec-number mono">{c.number}</span>
        </a>
      ))}
    </div>
  );
}
