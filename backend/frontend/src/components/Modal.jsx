export default function Modal({ title, onClose, children }) {
  return (
    <div
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(20, 23, 31, 0.5)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000
      }}
    >
      <div onClick={(e) => e.stopPropagation()} className="card" style={{ maxWidth: 420, width: "90%" }}>
        <div className="between" style={{ marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{title}</h3>
          <button className="btn btn-secondary" onClick={onClose}>Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}
