let loadPromise;

export function loadGoogleIdentity() {
  if (loadPromise) return loadPromise;

  if (window.google?.accounts?.id) {
    loadPromise = Promise.resolve(window.google);
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve(window.google);
    script.onerror = () => reject(new Error("Failed to load the Google Identity Services script"));
    document.head.appendChild(script);
  });

  return loadPromise;
}