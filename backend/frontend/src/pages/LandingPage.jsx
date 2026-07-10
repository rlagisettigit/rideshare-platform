import { useState } from "react";
import { Link } from "react-router-dom";
import EmergencyContacts from "../components/EmergencyContacts";
import SectionDivider from "../components/SectionDivider";
import heroRoad from "../assets/images/hero-road.jpg";
import carHatchback from "../assets/images/car-hatchback.jpg";
import carSedan from "../assets/images/car-sedan.jpg";
import carSuv from "../assets/images/car-suv.jpg";
import carRecurring from "../assets/images/car-recurring.jpg";
import driverCta from "../assets/images/driver-cta.jpg";
import appMockup from "../assets/images/app-mockup.jpg";
import testimonialAnanya from "../assets/images/testimonial-ananya.jpg";
import testimonialPriya from "../assets/images/testimonial-priya.jpg";
import testimonialVikram from "../assets/images/testimonial-vikram.jpg";

const ROUTE_COVERAGE = ["Hyderabad", "Suryapet", "Khammam", "Vijayawada", "Ongole", "Nellore", "Tirupati"];

const COST_COMPARISON = [
  { mode: "Taxi", price: 6500 },
  { mode: "Bus", price: 900 },
  { mode: "Train", price: 750 },
  { mode: "Shared Ride", price: 550, highlight: true }
];

const POPULAR_ROUTES = [
  { from: "Hyderabad", to: "Bengaluru", distance: "570 km", price: "from ₹650" },
  { from: "Hyderabad", to: "Vijayawada", distance: "275 km", price: "from ₹350" },
  { from: "Pune", to: "Mumbai", distance: "150 km", price: "from ₹250" },
  { from: "Chennai", to: "Bengaluru", distance: "345 km", price: "from ₹400" },
  { from: "Delhi", to: "Jaipur", distance: "280 km", price: "from ₹380" },
  { from: "Hyderabad", to: "Tirupati", distance: "560 km", price: "from ₹620" }
];

const RIDE_CATEGORIES = [
  { image: carHatchback, title: "Hatchback", desc: "Budget-friendly rides for solo travelers or pairs." },
  { image: carSedan, title: "Sedan", desc: "Extra comfort and boot space for longer trips." },
  { image: carSuv, title: "SUV", desc: "Room for groups traveling together with luggage." },
  { image: carRecurring, title: "Recurring", desc: "Book a seat on the same commute, every day it runs." }
];

const FEATURES = [
  { icon: "🧭", title: "Route-aware matching", desc: "We match you to rides whose path passes near your pickup and drop, not just exact city-to-city trips." },
  { icon: "📍", title: "Live driver tracking", desc: "See your driver's live position on the map from the moment they start toward your pickup point." },
  { icon: "🧾", title: "Verified drivers", desc: "Every driver completes vehicle and identity verification before they can publish a ride." },
  { icon: "⭐", title: "Two-way ratings", desc: "Drivers and passengers rate each other after every ride, building trust on both sides." },
  { icon: "🔁", title: "Recurring rides", desc: "Publish or book a daily commute once, tracked as a single running thread." },
  { icon: "🗺️", title: "Turn-by-turn handoff", desc: "Open your exact leg of the trip in Google Maps, Apple Maps, or Mappls with one tap." }
];

const TESTIMONIALS = [
  { name: "Ananya R.", role: "Daily commuter, Hyderabad", photo: testimonialAnanya, quote: "I book the same Suryapet to Hyderabad seat every weekday now. It's cheaper than the bus and the driver's already someone I trust." },
  { name: "Vikram S.", role: "Driver, Bengaluru", photo: testimonialVikram, quote: "I was driving to work alone anyway. Now three seats are filled most days and it covers my fuel for the week." },
  { name: "Priya M.", role: "Passenger, Vijayawada", photo: testimonialPriya, quote: "The live tracking meant I wasn't standing at the pickup point guessing. I could see the car approaching in real time." }
];

const STATISTICS = [
  { value: "12,000+", label: "Rides completed" },
  { value: "3,400+", label: "Verified drivers" },
  { value: "48", label: "Cities covered" },
  { value: "4.7 / 5", label: "Average rating" }
];

const CITIES = [
  "Hyderabad", "Bengaluru", "Chennai", "Pune", "Mumbai", "Delhi", "Vijayawada",
  "Nellore", "Tirupati", "Khammam", "Jaipur", "Coimbatore"
];

const FAQS = [
  {
    q: "How does route matching work?",
    a: "When you search, we look at every published ride whose path passes near your pickup and drop points - not just rides that start and end in your exact cities. A ride from Hyderabad to Chennai can pick you up in Nellore even though Nellore isn't the ride's final stop."
  },
  {
    q: "Are drivers verified?",
    a: "Yes. Every driver submits identity and vehicle documents, which are reviewed before they're allowed to publish rides."
  },
  {
    q: "Can I book a recurring commute instead of booking every day?",
    a: "Yes. Drivers can publish a recurring ride for a set of weekdays, and passengers book it once - every date is tracked as a single running thread instead of separate one-off bookings."
  },
  {
    q: "Can I track my driver before pickup?",
    a: "Once your booking is confirmed, you can see your driver's live position on the map as they approach your pickup point, and open the exact route in Google Maps, Apple Maps, or Mappls."
  },
  {
    q: "What if I need to cancel?",
    a: "You can cancel from My Bookings up until the ride starts. Refund timing depends on how close to departure you cancel."
  }
];

function RidesAlongYourRoute() {
  return (
    <section className="lp-section" id="rides-along-route">
      <div className="lp-section-head">
        <h2>🚗 Rides Along Your Route</h2>
        <p>You don't need an exact match - if a driver's route passes through your pickup or drop point, you'll see it in your search.</p>
      </div>
      <div className="lp-match-card">
        <div className="lp-match-query">
          Searching: <strong>Hyderabad → Tirupati</strong>
        </div>
        <div className="lp-match-results">
          <div className="lp-match-result">
            <span className="lp-match-check">✔</span>
            <span><strong>Hyderabad → Nellore</strong> — stops at Tirupati</span>
          </div>
          <div className="lp-match-result">
            <span className="lp-match-check">✔</span>
            <span><strong>Hyderabad → Madurai</strong> — passes through Tirupati</span>
          </div>
          <div className="lp-match-result">
            <span className="lp-match-check">✔</span>
            <span><strong>Hyderabad → Chennai</strong> — drops near Tirupati</span>
          </div>
        </div>
      </div>
    </section>
  );
}

function RouteCoverage() {
  return (
    <section className="lp-section" id="route-coverage">
      <div className="lp-section-head">
        <h2>🛣️ Route Coverage</h2>
      </div>
      <div className="lp-stepper">
        {ROUTE_COVERAGE.map((stop, i) => (
          <div className="lp-stepper-item" key={stop}>
            <div className="lp-stepper-marker">
              <span className={"lp-stepper-dot" + (i === 0 ? " start" : i === ROUTE_COVERAGE.length - 1 ? " end" : "")} />
              {i < ROUTE_COVERAGE.length - 1 && <span className="lp-stepper-line" />}
            </div>
            <span className="lp-stepper-label">{stop}</span>
          </div>
        ))}
      </div>
      <p className="lp-caption">Passengers can book from or to any supported point on the route.</p>
    </section>
  );
}

function CostComparison() {
  const maxPrice = Math.max(...COST_COMPARISON.map((c) => c.price));
  return (
    <section className="lp-section" id="cost-comparison">
      <div className="lp-section-head">
        <h2>💰 Cost Comparison</h2>
        <p>Hyderabad → Tirupati, one seat, one-way.</p>
      </div>
      <div className="lp-cost-table">
        {COST_COMPARISON.map((row) => (
          <div className={"lp-cost-row" + (row.highlight ? " highlight" : "")} key={row.mode}>
            <span className="lp-cost-mode">{row.mode}</span>
            <div className="lp-cost-bar-track">
              <div className="lp-cost-bar" style={{ width: `${(row.price / maxPrice) * 100}%` }} />
            </div>
            <span className="lp-cost-price">₹{row.price.toLocaleString("en-IN")}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

function EnvironmentalImpact() {
  const lines = [
    "Travel Together.",
    "Reduce Traffic.",
    "Reduce Fuel Consumption.",
    "Lower Carbon Emissions."
  ];
  return (
    <section className="lp-section lp-env" id="environmental-impact">
      <div className="lp-section-head">
        <h2>🌱 Environmental Impact</h2>
      </div>
      <div className="lp-env-lines">
        {lines.map((line) => (
          <span className="lp-env-line" key={line}>{line}</span>
        ))}
      </div>
    </section>
  );
}

function FaqItem({ q, a, isOpen, onToggle }) {
  return (
    <div className="lp-faq-item">
      <button type="button" className="lp-faq-question" onClick={onToggle} aria-expanded={isOpen}>
        <span>{q}</span>
        <span className="lp-faq-caret">{isOpen ? "−" : "+"}</span>
      </button>
      {isOpen && <p className="lp-faq-answer">{a}</p>}
    </div>
  );
}

export default function LandingPage() {
  const [openFaq, setOpenFaq] = useState(0);

  return (
    <div className="lp-root">
      {/* Header */}
      <header className="lp-header">
        <div className="lp-header-inner">
          <div className="nav-brand">
            Waypoint<span className="dot">•</span>
          </div>
          <nav className="lp-header-links">
            <a href="#how-it-works">How it works</a>
            <a href="#safety">Safety</a>
            <a href="#faq">FAQ</a>
          </nav>
          <div className="lp-header-actions">
            <Link to="/login" className="btn btn-secondary">Log in</Link>
            <Link to="/register" className="btn btn-primary">Sign up</Link>
          </div>
        </div>
      </header>

      {/* Hero Search Section */}
      <section className="lp-hero">
        <img src={heroRoad} alt="" className="lp-hero-bg" aria-hidden="true" />
        <div className="lp-hero-overlay" />
        <div className="lp-hero-inner">
          <div className="lp-hero-copy">
            <h1>Share the ride. Split the cost.</h1>
            <p>
              Waypoint matches passengers and drivers along shared routes across India -
              book a seat, publish a ride, or set up a recurring commute in minutes.
            </p>
          </div>
          <form className="lp-hero-search card" onSubmit={(e) => e.preventDefault()}>
            <div className="field-row">
              <div className="field">
                <label htmlFor="lp-from">From</label>
                <input id="lp-from" type="text" placeholder="Leaving from" />
              </div>
              <div className="field">
                <label htmlFor="lp-to">To</label>
                <input id="lp-to" type="text" placeholder="Going to" />
              </div>
            </div>
            <div className="field">
              <label htmlFor="lp-date">Date</label>
              <input id="lp-date" type="date" />
            </div>
            <Link to="/login" className="btn btn-primary lp-hero-cta">Search rides</Link>
          </form>
        </div>
      </section>

      {/* Why Choose Us */}
      <section className="lp-section" id="why-choose-us">
        <div className="lp-section-head">
          <h2>Why Choose Us</h2>
        </div>
        <div className="lp-grid-3">
          <div className="card lp-icon-card">
            <span className="lp-icon">🧭</span>
            <h3>Smarter matching</h3>
            <p>Route-aware search finds rides that pass near you, not just exact city pairs.</p>
          </div>
          <div className="card lp-icon-card">
            <span className="lp-icon">🛡️</span>
            <h3>Verified community</h3>
            <p>Every driver is identity and vehicle verified before publishing a ride.</p>
          </div>
          <div className="card lp-icon-card">
            <span className="lp-icon">💸</span>
            <h3>Real savings</h3>
            <p>Shared rides cost a fraction of a taxi and often beat the bus or train.</p>
          </div>
        </div>
      </section>

      <SectionDivider />

      <RidesAlongYourRoute />
      <RouteCoverage />

      {/* Popular Routes */}
      <section className="lp-section" id="popular-routes">
        <div className="lp-section-head">
          <h2>Popular Routes</h2>
        </div>
        <div className="lp-grid-3">
          {POPULAR_ROUTES.map((r) => (
            <div className="card" key={`${r.from}-${r.to}`}>
              <div className="route-line">
                <span className="stop">{r.from}</span>
                <span className="path" />
                <span className="stop">{r.to}</span>
              </div>
              <div className="between">
                <span className="muted">{r.distance}</span>
                <span style={{ fontWeight: 600 }}>{r.price}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* How It Works */}
      <section className="lp-section" id="how-it-works">
        <div className="lp-section-head">
          <h2>How It Works</h2>
        </div>
        <div className="lp-grid-4">
          <div className="lp-step">
            <span className="lp-step-num">1</span>
            <h3>Search or publish</h3>
            <p>Passengers search a route and date; drivers publish seats on a trip they're already making.</p>
          </div>
          <div className="lp-step">
            <span className="lp-step-num">2</span>
            <h3>Request a seat</h3>
            <p>Send a booking request with your pickup and drop points along the route.</p>
          </div>
          <div className="lp-step">
            <span className="lp-step-num">3</span>
            <h3>Ride together</h3>
            <p>Track your driver live and get turn-by-turn directions to your exact pickup.</p>
          </div>
          <div className="lp-step">
            <span className="lp-step-num">4</span>
            <h3>Rate each other</h3>
            <p>After the ride, driver and passenger leave a rating for each other.</p>
          </div>
        </div>
      </section>

      <SectionDivider />

      <CostComparison />
      <EnvironmentalImpact />

      {/* Become a Driver */}
      <section className="lp-section lp-driver-cta" id="become-a-driver">
        <img src={driverCta} alt="" className="lp-driver-cta-bg" aria-hidden="true" />
        <div className="lp-driver-cta-copy">
          <h2>Become a Driver</h2>
          <p>
            Already making the trip? Publish your route, set your price per seat, and let verified
            passengers book alongside you - covering your fuel and tolls along the way.
          </p>
          <Link to="/register" className="btn btn-primary">Start driving</Link>
        </div>
      </section>

      {/* Safety */}
      <section className="lp-section" id="safety">
        <div className="lp-section-head">
          <h2>Safety</h2>
        </div>
        <div className="lp-grid-3">
          <div className="card lp-icon-card">
            <span className="lp-icon">🪪</span>
            <h3>ID & vehicle checks</h3>
            <p>Drivers verify their identity and vehicle documents before publishing rides.</p>
          </div>
          <div className="card lp-icon-card">
            <span className="lp-icon">📍</span>
            <h3>Live trip tracking</h3>
            <p>Every ride's position is visible on the map from pickup to drop-off.</p>
          </div>
          <div className="card lp-icon-card">
            <span className="lp-icon">⭐</span>
            <h3>Two-way ratings</h3>
            <p>Ratings from both sides keep accountability visible across the community.</p>
          </div>
        </div>
      </section>

      {/* Emergency Contacts */}
      <section className="lp-section" id="emergency-contacts">
        <div className="lp-section-head">
          <h2>Emergency Contacts</h2>
          <p>Save these before you travel - they're one tap away from any phone.</p>
        </div>
        <EmergencyContacts className="ec-grid-lp" />
      </section>

      <SectionDivider />

      {/* Ride Categories */}
      <section className="lp-section" id="ride-categories">
        <div className="lp-section-head">
          <h2>Ride Categories</h2>
        </div>
        <div className="lp-grid-4">
          {RIDE_CATEGORIES.map((c) => (
            <div className="lp-photo-card" key={c.title}>
              <img src={c.image} alt={c.title} className="lp-photo-card-img" />
              <div className="lp-photo-card-body">
                <h3>{c.title}</h3>
                <p>{c.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="lp-section" id="features">
        <div className="lp-section-head">
          <h2>Features</h2>
        </div>
        <div className="lp-grid-3">
          {FEATURES.map((f) => (
            <div className="card lp-icon-card" key={f.title}>
              <span className="lp-icon">{f.icon}</span>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Testimonials */}
      <section className="lp-section" id="testimonials">
        <div className="lp-section-head">
          <h2>Testimonials</h2>
        </div>
        <div className="lp-grid-3">
          {TESTIMONIALS.map((t) => (
            <div className="card lp-testimonial" key={t.name}>
              <span className="lp-quote-mark">"</span>
              <p className="lp-quote">"{t.quote}"</p>
              <div className="row">
                <img src={t.photo} alt={t.name} className="lp-avatar-photo" />
                <div>
                  <strong>{t.name}</strong>
                  <div className="muted">{t.role}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      <SectionDivider />

      {/* Statistics */}
      <section className="lp-section lp-stats" id="statistics">
        <div className="lp-grid-4">
          {STATISTICS.map((s) => (
            <div className="lp-stat" key={s.label}>
              <div className="lp-stat-value">{s.value}</div>
              <div className="lp-stat-label">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Cities Covered */}
      <section className="lp-section" id="cities-covered">
        <div className="lp-section-head">
          <h2>Cities Covered</h2>
        </div>
        <div className="lp-city-tags">
          {CITIES.map((c) => (
            <span className="lp-city-tag" key={c}>{c}</span>
          ))}
        </div>
      </section>

      <SectionDivider />

      {/* FAQ */}
      <section className="lp-section" id="faq">
        <div className="lp-section-head">
          <h2>FAQ</h2>
        </div>
        <div className="lp-faq">
          {FAQS.map((f, i) => (
            <FaqItem
              key={f.q}
              q={f.q}
              a={f.a}
              isOpen={openFaq === i}
              onToggle={() => setOpenFaq(openFaq === i ? -1 : i)}
            />
          ))}
        </div>
      </section>

      {/* Download App */}
      <section className="lp-section lp-download" id="download-app">
        <div className="lp-download-copy">
          <h2>Take Waypoint with you</h2>
          <p>Our mobile app is on the way. Sign up now and we'll let you know the moment it lands.</p>
          <Link to="/register" className="btn btn-primary">Get notified</Link>
        </div>
        <img src={appMockup} alt="Waypoint app preview on a phone" className="lp-download-image" />
      </section>

      {/* Footer */}
      <footer className="lp-footer">
        <div className="lp-footer-inner">
          <div className="nav-brand">
            Waypoint<span className="dot">•</span>
          </div>
          <div className="lp-footer-links">
            <a href="#how-it-works">How it works</a>
            <a href="#safety">Safety</a>
            <a href="#faq">FAQ</a>
            <Link to="/login">Log in</Link>
            <Link to="/register">Sign up</Link>
          </div>
          <span className="muted lp-footer-copyright">© {new Date().getFullYear()} Waypoint. All rights reserved.</span>
        </div>
      </footer>
    </div>
  );
}
