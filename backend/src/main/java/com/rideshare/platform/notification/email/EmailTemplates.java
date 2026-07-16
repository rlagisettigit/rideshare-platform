package com.rideshare.platform.notification.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds each notification email's subject + HTML. The copy (subject, heading, CTA/badge
 * labels, body sentences) lives in {@code resources/templates/email/} - see
 * {@link EmailTemplateRenderer} - and is editable without touching this class. What stays here
 * is structural, reusable presentation: the route/map card, buttons, badges, and the mapping
 * from domain data (Ride/Booking fields) to the {{token}} values those templates expect.
 *
 * Visual language mirrors the frontend's "Aura Ride" design tokens (frontend/src/styles/index.css) -
 * route-indigo / signal-amber / asphalt-ink - so an email and the in-app UI read as one product.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplates {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a");

    private static final String INK = "#14171f";
    private static final String ROUTE = "#2f3c7e";
    private static final String SIGNAL = "#f2a93b";
    private static final String MOSS = "#3f7a5d";
    private static final String RUST = "#b5443a";
    private static final String MUTED = "#6b6f7b";
    private static final String LINE = "#e4e0d8";

    private final EmailTemplateRenderer renderer;

    @Value("${GOOGLE_STATIC_MAPS_API_KEY:}")
    private String staticMapsApiKey;

    public record EmailContent(String subject, String html) {}

    public record RouteInfo(String originAddress, double originLat, double originLng,
                             String destinationAddress, double destLat, double destLng) {}

    public EmailContent bookingRequested(String driverName, String passengerName, RouteInfo route,
                                          LocalDateTime departureAt, int seats, BigDecimal fare) {
        Map<String, String> fields = baseFields();
        fields.put("driverName", esc(driverName));
        fields.put("passengerName", esc(passengerName));
        fields.put("seatsLabel", seatsLabel(seats));
        fields.put("routeCard", routeCard(route, departureAt, seats, fare, null));
        fields.put("cta", ctaButton(renderer.copy("booking-requested", "cta", fields), SIGNAL, INK));
        return build("booking-requested", fields);
    }

    public EmailContent bookingConfirmed(String passengerName, String driverName, String vehicleModel, RouteInfo route,
                                          LocalDateTime departureAt, int seats, BigDecimal fare) {
        Map<String, String> fields = baseFields();
        fields.put("passengerName", esc(passengerName));
        fields.put("driverName", esc(driverName));
        fields.put("vehicleClause", vehicleModel != null ? " (driving a " + esc(vehicleModel) + ")" : "");
        fields.put("routeCard", routeCard(route, departureAt, seats, fare, badge(renderer.copy("booking-confirmed", "badge", fields), MOSS)));
        fields.put("cta", ctaButton(renderer.copy("booking-confirmed", "cta", fields), ROUTE, "#ffffff"));
        return build("booking-confirmed", fields);
    }

    public EmailContent bookingRejected(String passengerName, RouteInfo route, LocalDateTime departureAt, int seats) {
        Map<String, String> fields = baseFields();
        fields.put("passengerName", esc(passengerName));
        fields.put("routeCard", routeCard(route, departureAt, seats, null, badge(renderer.copy("booking-rejected", "badge", fields), RUST)));
        fields.put("cta", ctaButton(renderer.copy("booking-rejected", "cta", fields), ROUTE, "#ffffff"));
        return build("booking-rejected", fields);
    }

    public EmailContent bookingCancelled(String recipientName, String cancelledBy, String reason, RouteInfo route,
                                          LocalDateTime departureAt, int seats) {
        Map<String, String> fields = baseFields();
        String byKey = "DRIVER".equals(cancelledBy) ? "driver" : "PASSENGER".equals(cancelledBy) ? "passenger" : "system";
        fields.put("recipientName", esc(recipientName));
        fields.put("whoCancelled", renderer.copy("booking-cancelled", "by." + byKey, fields));
        fields.put("reasonClause", reason != null && !reason.isBlank()
                ? " Reason: <em>" + esc(reason) + "</em>." : "");
        fields.put("routeCard", routeCard(route, departureAt, seats, null, badge(renderer.copy("booking-cancelled", "badge", fields), RUST)));
        return build("booking-cancelled", fields);
    }

    public EmailContent rideStarted(String passengerName, String driverName, RouteInfo route, LocalDateTime departureAt) {
        Map<String, String> fields = baseFields();
        fields.put("passengerName", esc(passengerName));
        fields.put("driverName", esc(driverName));
        fields.put("routeCard", routeCard(route, departureAt, null, null, badge(renderer.copy("ride-started", "badge", fields), SIGNAL)));
        fields.put("cta", ctaButton(renderer.copy("ride-started", "cta", fields), ROUTE, "#ffffff"));
        return build("ride-started", fields);
    }

    public EmailContent rideCompleted(String passengerName, RouteInfo route, LocalDateTime departureAt, BigDecimal fare) {
        Map<String, String> fields = baseFields();
        fields.put("passengerName", esc(passengerName));
        fields.put("routeCard", routeCard(route, departureAt, null, fare, badge(renderer.copy("ride-completed", "badge", fields), MOSS)));
        fields.put("cta", ctaButton(renderer.copy("ride-completed", "cta", fields), SIGNAL, INK));
        return build("ride-completed", fields);
    }

    private EmailContent build(String event, Map<String, String> fields) {
        return new EmailContent(renderer.subject(event, fields), renderer.render(event, fields));
    }

    private Map<String, String> baseFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("pStyle", pStyle());
        return fields;
    }

    // ---- structural building blocks (not copy - safe to keep in code) ----

    private String seatsLabel(int seats) {
        return seats + " seat" + (seats == 1 ? "" : "s");
    }

    private String routeCard(RouteInfo route, LocalDateTime departureAt, Integer seats, BigDecimal fare, String badgeHtml) {
        String map = mapImageTag(route);
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#ffffff;border:1px solid " + LINE + ";border-radius:14px;margin:20px 0;\">"
                + "<tr><td style=\"padding:20px 24px;\">"
                + (badgeHtml != null ? badgeHtml + "<div style=\"height:8px;\"></div>" : "")
                + "<div style=\"font-family:'JetBrains Mono',monospace;font-size:14px;color:" + INK + ";\">"
                + esc(route.originAddress()) + " &nbsp;&rarr;&nbsp; " + esc(route.destinationAddress())
                + "</div>"
                + "<div style=\"color:" + MUTED + ";font-size:13px;margin-top:6px;\">" + esc(departureAt.format(WHEN)) + "</div>"
                + (seats != null || fare != null ? "<div style=\"color:" + MUTED + ";font-size:13px;margin-top:2px;\">"
                    + (seats != null ? seatsLabel(seats) : "")
                    + (seats != null && fare != null ? " &middot; " : "")
                    + (fare != null ? "₹" + fare.setScale(0, RoundingMode.HALF_UP) : "")
                    + "</div>" : "")
                + "</td></tr>"
                + (map != null ? "<tr><td>" + map + "</td></tr>" : "")
                + "</table>";
    }

    private String mapImageTag(RouteInfo route) {
        if (staticMapsApiKey == null || staticMapsApiKey.isBlank()) return null;
        String url = "https://maps.googleapis.com/maps/api/staticmap?size=600x260&scale=2&maptype=roadmap"
                + "&markers=" + enc("color:0x2f3c7e|label:A|" + route.originLat() + "," + route.originLng())
                + "&markers=" + enc("color:0xf2a93b|label:B|" + route.destLat() + "," + route.destLng())
                + "&path=" + enc("color:0x2f3c7ecc|weight:4|" + route.originLat() + "," + route.originLng()
                        + "|" + route.destLat() + "," + route.destLng())
                + "&key=" + enc(staticMapsApiKey);
        return "<img src=\"" + url + "\" width=\"600\" height=\"260\" alt=\"Route map\" "
                + "style=\"display:block;width:100%;height:auto;border-radius:0 0 14px 14px;\" />";
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String badge(String text, String color) {
        if (text == null || text.isBlank()) return null;
        return "<span style=\"display:inline-block;font-size:11px;font-weight:700;letter-spacing:0.04em;"
                + "text-transform:uppercase;color:" + color + ";background:" + color + "1f;padding:3px 10px;border-radius:999px;\">"
                + esc(text) + "</span>";
    }

    private String ctaButton(String label, String bg, String fg) {
        if (label == null || label.isBlank()) return "";
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:8px;\"><tr><td "
                + "style=\"background:" + bg + ";border-radius:8px;\">"
                + "<span style=\"display:inline-block;padding:12px 22px;font-family:Inter,Arial,sans-serif;"
                + "font-size:14px;font-weight:600;color:" + fg + ";\">" + esc(label) + " &rarr;</span>"
                + "</td></tr></table>";
    }

    private String pStyle() {
        return "font-family:Inter,Arial,sans-serif;font-size:15px;line-height:1.55;color:" + INK + ";margin:0 0 12px;";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
