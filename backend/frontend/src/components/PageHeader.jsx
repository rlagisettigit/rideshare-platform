export default function PageHeader({ image, title, description, eyebrow, kicker, actions }) {
  return (
    <div className="page-banner-wrap">
      {eyebrow}
      <div className="page-banner">
        <img src={image} alt="" aria-hidden="true" className="page-banner-bg" />
        <div className="page-banner-overlay" />
        <div className="page-banner-content">
          <div>
            {kicker && <span className="page-banner-kicker">{kicker}</span>}
            <h1>{title}</h1>
            {description && <p>{description}</p>}
          </div>
          {actions && <div className="page-banner-actions row">{actions}</div>}
        </div>
      </div>
    </div>
  );
}
