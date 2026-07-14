export default function PrivacyContent() {
  return (
    <>
      <h2>1. Information we collect</h2>
      <ul>
        <li><strong>Account details:</strong> name, email, mobile number, gender, and date of birth (used to confirm you're 18+).</li>
        <li><strong>Sign-in data:</strong> if you use Google or Apple sign-in, we receive your verified name and email from that provider - we never see your Google or Apple password.</li>
        <li><strong>Trip data:</strong> pickup/drop points, published or booked routes, and ride history.</li>
        <li><strong>Live location:</strong> a driver's real-time position during an active, booked trip, visible to matched passengers for that trip only.</li>
        <li><strong>Driver verification data:</strong> license and vehicle documents submitted for driver onboarding.</li>
        <li><strong>Payment references:</strong> payment and wallet transaction records needed to process fares, refunds, and payouts. We don't store full card numbers ourselves.</li>
        <li><strong>Ratings and messages:</strong> ratings you give or receive, and messages exchanged with a matched driver or passenger about a ride.</li>
        <li><strong>Emergency contacts:</strong> contact details you optionally save for quick access during a trip.</li>
      </ul>

      <h2>2. How we use your information</h2>
      <p>
        We use this information to operate the core service: matching passengers to rides along
        their route, verifying drivers, processing payments, showing live trip tracking, sending
        booking and ride notifications, and calculating ratings. We also use it to investigate
        safety reports and enforce our <a href="/terms">Terms & Conditions</a>.
      </p>

      <h2>3. What we share, and with whom</h2>
      <p>
        When you book or accept a ride, we share the minimum trip details needed to complete
        it - name, pickup/drop points, and live location during the trip - with the matched
        driver or passenger. We don't sell your personal information, and we don't share it with
        other users beyond what's needed for the specific ride you're part of.
      </p>

      <h2>4. Third-party service providers</h2>
      <ul>
        <li><strong>Mappls:</strong> route calculation, distance/duration estimates, and reverse geocoding (turning coordinates into place names) for route previews.</li>
        <li><strong>Google Maps:</strong> address autocomplete when you enter a pickup or drop location, and static map images embedded in some emails.</li>
        <li><strong>Google / Apple Sign-In:</strong> identity verification when you choose "Continue with Google" or "Continue with Apple" instead of a password.</li>
        <li><strong>Resend:</strong> delivery of transactional emails (booking confirmations, ride updates, account notices).</li>
      </ul>
      <p>Each provider only receives the data necessary to perform its specific function and is bound by its own privacy terms.</p>

      <h2>5. Data retention</h2>
      <p>
        We retain account and ride history for as long as your account is active, and for a
        reasonable period afterward to resolve disputes, honor legal obligations, and maintain
        platform safety records. Live location data is only retained for the duration needed to
        support the trip it relates to.
      </p>

      <h2>6. Your rights</h2>
      <p>
        You can review and update most of your profile information (name, gender, date of birth,
        preferred language, home/office locations) from your account settings at any time. To
        request a copy of your data, a correction, or account deletion, contact us through the
        app's support channel.
      </p>

      <h2>7. Data security</h2>
      <p>
        Access to the platform is protected by password hashing and short-lived signed access
        tokens. We restrict internal access to personal data to what's needed to operate and
        support the service.
      </p>

      <h2>8. Children's privacy</h2>
      <p>
        Waypoint is not intended for anyone under 18. We verify date of birth at registration and
        don't knowingly collect data from minors.
      </p>

      <h2>9. Cookies and local storage</h2>
      <p>
        We use browser local storage (not third-party tracking cookies) to keep you signed in
        between visits, storing your session tokens on your own device.
      </p>

      <h2>10. Changes to this policy</h2>
      <p>
        We may update this policy as the platform's features change. We'll update the "Last
        updated" date above whenever we do.
      </p>

      <h2>11. Contact</h2>
      <p>
        Questions about this policy or your data can be sent through the support channel within
        the app.
      </p>
    </>
  );
}
