import { useEffect, useRef, useState } from "react";
import { loadGoogleIdentity } from "../lib/googleIdentity";

/** Renders the official "Sign in with Google" button and hands the resulting ID token to onCredential. */
export default function GoogleSignInButton({ onCredential, text = "continue_with" }) {
  const buttonRef = useRef(null);
  const [unavailable, setUnavailable] = useState(false);

  useEffect(() => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!clientId) {
      setUnavailable(true);
      return;
    }

    let cancelled = false;
    loadGoogleIdentity()
      .then((google) => {
        if (cancelled || !buttonRef.current) return;
        google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => onCredential(response.credential)
        });
        // Google's button takes a fixed pixel width (no %/auto support), so size it to the
        // available container width instead of a flat 320 - otherwise it overflows narrow
        // mobile cards. Clamped to Google's supported range (200-400).
        const containerWidth = buttonRef.current.clientWidth || 320;
        google.accounts.id.renderButton(buttonRef.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          shape: "rectangular",
          text,
          logo_alignment: "center",
          width: Math.min(400, Math.max(200, containerWidth))
        });
      })
      .catch(() => setUnavailable(true));

    return () => {
      cancelled = true;
    };
  }, [onCredential, text]);

  if (unavailable) return null;
  return <div ref={buttonRef} className="google-signin-button" />;
}