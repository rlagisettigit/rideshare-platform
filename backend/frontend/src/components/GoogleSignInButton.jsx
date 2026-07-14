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
        google.accounts.id.renderButton(buttonRef.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          shape: "rectangular",
          text,
          width: 320
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