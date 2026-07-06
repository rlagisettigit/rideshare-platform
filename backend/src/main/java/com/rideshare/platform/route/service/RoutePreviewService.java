package com.rideshare.platform.route.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.route.PolylineCodec;
import com.rideshare.platform.route.dto.RouteOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

/**
 * Fetches real driving-route alternatives (fastest, alternates) from the Mappls Directions
 * API for driver-facing route preview at ride-publish time. Major cities/places along a
 * specific route are resolved on demand (see placesForRoute) rather than eagerly for every
 * option, to keep the default preview to a single Mappls call.
 *
 * This is a best-effort integration against Mappls' documented route_adv / rev_geocode
 * endpoints - field names below (routes[].geometry/distance/duration, results[].locality
 * etc.) should be verified against a live API key and Mappls' current docs, and adjusted
 * here if the actual response differs. Failures are logged rather than silently swallowed
 * so a schema mismatch is easy to spot once tested.
 *
 * Also backs RouteService's default publish-time route (see fastestRouteSafe): if a driver
 * publishes without previewing/selecting a route themselves, RouteService still fetches one
 * real Mappls route automatically - the same single-call cost as the preview button - and
 * only falls back to MapplsRoutingProvider's local straight-line interpolation if that call
 * itself fails (missing credentials, Mappls outage, no route found).
 */
@Service
public class RoutePreviewService {

    private static final Logger log = LoggerFactory.getLogger(RoutePreviewService.class);
    private static final int MAX_PLACES_PER_ROUTE = 3;
    private static final int PLACE_SAMPLE_POINTS = 2;

    /** Despite the name, Mappls issues this as an OAuth client_id, not a standalone REST key. */
    @Value("${MAPPLS_API_KEY:}")
    private String clientId;

    @Value("${MAPPLS_CLIENT_SECRET:}")
    private String clientSecret;

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt;

    /** Reverse-geocode results are cached (keyed by ~1km-rounded coordinates) to conserve
     *  Mappls' daily/hourly rate limit across overlapping routes and repeated previews. */
    private final Map<String, Optional<String>> geocodeCache = new ConcurrentHashMap<>();

    /** Circuit breaker: once rev_geocode fails once (e.g. daily quota exhausted), stop
     *  hammering it with more calls that are certain to fail for a while - this is what
     *  keeps a single preview from making up to a dozen slow, doomed network calls. */
    private static final Duration GEOCODE_BACKOFF = Duration.ofMinutes(10);
    private volatile Instant geocodeBackoffUntil = Instant.EPOCH;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://apis.mappls.com")
            .build();

    private final RestClient authClient = RestClient.builder()
            .baseUrl("https://outpost.mappls.com")
            .build();

    /**
     * Returns route alternatives with a single Mappls call - no automatic reverse geocoding,
     * no separate toll-free request. Both were dropped as default behavior: geocoding alone
     * was up to 12 calls per preview, and the dedicated toll-free request wasn't proving
     * useful (it returned an identical route to the fastest option in testing, likely because
     * Mappls' route_adv doesn't honor avoid=tollway, or this corridor has no toll segment).
     * City names are fetched on demand per route via {@link #placesForRoute}.
     */
    public List<RouteOption> previewRoutes(double originLat, double originLng, double destLat, double destLng) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw ApiException.externalService("ROUTE_PREVIEW_001",
                    "Route preview requires Mappls credentials (set MAPPLS_API_KEY as the client_id and MAPPLS_CLIENT_SECRET).");
        }

        List<MapplsRoute> routes = fetchRoutes(originLat, originLng, destLat, destLng, false);
        if (routes.isEmpty()) {
            throw ApiException.externalService("ROUTE_PREVIEW_002",
                    "Mappls did not return any route for this origin/destination.");
        }

        List<RouteOption> options = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            String label = i == 0 ? "FASTEST" : "ALTERNATE_" + i;
            MapplsRoute route = routes.get(i);
            options.add(new RouteOption(label, (int) route.distance(), (int) route.duration(), false, List.of(), route.geometry()));
        }
        return options;
    }

    /**
     * Best-effort single-route fetch for ride publish when the driver didn't preview/select
     * one themselves. Unlike {@link #previewRoutes}, this never throws - any failure (missing
     * credentials, Mappls outage, no routes found) returns empty so the caller can fall back
     * to the local straight-line interpolation instead of blocking the publish.
     */
    public Optional<RouteOption> fastestRouteSafe(double originLat, double originLng, double destLat, double destLng) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return Optional.empty();
        }
        List<MapplsRoute> routes = fetchRoutes(originLat, originLng, destLat, destLng, false);
        if (routes.isEmpty()) {
            return Optional.empty();
        }
        MapplsRoute route = routes.get(0);
        return Optional.of(new RouteOption("FASTEST", (int) route.distance(), (int) route.duration(), false, List.of(), route.geometry()));
    }

    /** On-demand: resolves the major cities/towns along one specific previewed route. */
    public List<String> placesForRoute(String encodedPolyline) {
        List<double[]> decoded = PolylineCodec.decode(encodedPolyline);
        return samplePlaces(decoded);
    }

    /** OAuth client_credentials token exchange, cached until shortly before it expires. */
    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        MapplsTokenResponse response = authClient.post()
                .uri("/api/security/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MapplsTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw ApiException.externalService("ROUTE_PREVIEW_003",
                    "Mappls did not return an access token for the configured client_id/client_secret.");
        }

        cachedAccessToken = response.accessToken();
        long expiresInSeconds = response.expiresIn() != null ? response.expiresIn() : 3600;
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresInSeconds - 60));
        return cachedAccessToken;
    }

    private List<MapplsRoute> fetchRoutes(double oLat, double oLng, double dLat, double dLng, boolean avoidTolls) {
        try {
            String path = String.format(
                    "/advancedmaps/v1/%s/route_adv/driving/%f,%f;%f,%f?geometries=polyline&overview=full&alternatives=true&steps=false%s",
                    getAccessToken(), oLng, oLat, dLng, dLat, avoidTolls ? "&avoid=tollway" : "");
            MapplsRouteResponse response = restClient.get().uri(path).retrieve().body(MapplsRouteResponse.class);
            if (response == null || response.routes() == null) {
                log.warn("Mappls route_adv (avoidTolls={}) returned no routes for ({},{}) -> ({},{})",
                        avoidTolls, oLat, oLng, dLat, dLng);
                return List.of();
            }
            return response.routes();
        } catch (Exception e) {
            log.warn("Mappls route_adv call failed (avoidTolls={}) for ({},{}) -> ({},{}): {}",
                    avoidTolls, oLat, oLng, dLat, dLng, e.getMessage());
            return List.of();
        }
    }

    /** Reverse-geocodes a handful of evenly spaced interior points along the route to name the cities/towns passed through. */
    private List<String> samplePlaces(List<double[]> decodedPoints) {
        Set<String> places = new LinkedHashSet<>();
        if (decodedPoints.size() < 3) {
            return List.of();
        }
        for (int i = 1; i <= PLACE_SAMPLE_POINTS; i++) {
            int index = (int) ((double) i / (PLACE_SAMPLE_POINTS + 1) * (decodedPoints.size() - 1));
            double[] point = decodedPoints.get(index);
            reverseGeocode(point[0], point[1]).ifPresent(places::add);
            if (places.size() >= MAX_PLACES_PER_ROUTE) break;
        }
        return List.copyOf(places);
    }

    private Optional<String> reverseGeocode(double lat, double lng) {
        String cacheKey = String.format(Locale.ROOT, "%.2f,%.2f", lat, lng); // ~1km grid
        Optional<String> cached = geocodeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        if (Instant.now().isBefore(geocodeBackoffUntil)) {
            return Optional.empty(); // known-broken right now (e.g. quota exhausted); don't waste a round trip
        }

        Optional<String> result = doReverseGeocode(lat, lng);
        geocodeCache.put(cacheKey, result);
        return result;
    }

    private Optional<String> doReverseGeocode(double lat, double lng) {
        try {
            String path = String.format("/advancedmaps/v1/%s/rev_geocode?lat=%f&lng=%f", getAccessToken(), lat, lng);
            MapplsRevGeocodeResponse response = restClient.get().uri(path).retrieve().body(MapplsRevGeocodeResponse.class);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }
            MapplsRevGeocodeResult result = response.results().getFirst();
            // Prefer the district - in AP/Telangana it's named after its headquarters town
            // (Kurnool district -> Kurnool, Prakasam district -> Ongole), which reads as the
            // recognizable "major city" a driver expects, unlike locality/village which name
            // whatever hamlet is nearest to the exact sampled point on the road.
            String place = firstNonBlank(result.district(), result.city(), result.locality(), result.subLocality(), result.village());
            return Optional.ofNullable(place).map(this::stripDistrictSuffix);
        } catch (Exception e) {
            geocodeBackoffUntil = Instant.now().plus(GEOCODE_BACKOFF);
            log.warn("Mappls rev_geocode call failed for ({},{}); backing off geocoding for {} min: {}",
                    lat, lng, GEOCODE_BACKOFF.toMinutes(), e.getMessage());
            return Optional.empty();
        }
    }

    private String stripDistrictSuffix(String place) {
        return place.replaceAll("(?i)\\s+district$", "").trim();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapplsTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapplsRouteResponse(List<MapplsRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapplsRoute(String geometry, double distance, double duration) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapplsRevGeocodeResponse(List<MapplsRevGeocodeResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapplsRevGeocodeResult(String locality, String subLocality, String village, String district, String city) {}
}
