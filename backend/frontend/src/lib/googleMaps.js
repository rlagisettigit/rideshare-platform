let loadPromise;

export function loadGoogleMaps() {
  if (loadPromise) return loadPromise;

  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
  if (!apiKey) {
    return Promise.reject(new Error("VITE_GOOGLE_MAPS_API_KEY is not set"));
  }

  if (window.google?.maps?.places) {
    loadPromise = Promise.resolve(window.google);
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    window.__initGoogleMaps = () => resolve(window.google);

    const script = document.createElement("script");
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&libraries=places&loading=async&callback=__initGoogleMaps`;
    script.async = true;
    script.defer = true;
    script.onerror = () => reject(new Error("Failed to load the Google Maps script"));
    document.head.appendChild(script);
  });

  return loadPromise;
}
