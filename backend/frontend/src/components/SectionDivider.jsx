export default function SectionDivider() {
  return (
    <div className="lp-divider" aria-hidden="true">
      <span className="lp-divider-line" />
      <svg width="30" height="30" viewBox="0 0 30 30" className="lp-divider-motif">
        <g transform="translate(15,15)">
          {[0, 45, 90, 135, 180, 225, 270, 315].map((deg) => (
            <ellipse key={deg} cx="0" cy="-6.5" rx="3.1" ry="6.5" transform={`rotate(${deg})`} className="lp-divider-petal" />
          ))}
          <circle r="3.4" className="lp-divider-core" />
        </g>
      </svg>
      <span className="lp-divider-line" />
    </div>
  );
}
