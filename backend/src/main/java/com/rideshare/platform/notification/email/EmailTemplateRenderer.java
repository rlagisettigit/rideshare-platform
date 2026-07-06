package com.rideshare.platform.notification.email;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and renders the {@code {{token}}}-templated files under
 * {@code resources/templates/email/} - one HTML fragment per event plus a shared
 * {@code layout.html}, with all subject/heading/CTA/badge copy in {@code email-content.properties}
 * keyed by <event-name>.<field>. Keeps the actual English copy editable without touching Java, the
 * way {@link EmailTemplates} composes the reusable structural pieces (route card, buttons, map).
 */
@Component
class EmailTemplateRenderer {

    private static final String BASE = "/templates/email/";
    private static final Pattern TOKEN = Pattern.compile("\\{\\{(\\w+)}}");

    private final Properties content;
    private final String layout;
    private final Map<String, String> fragmentCache = new ConcurrentHashMap<>();

    EmailTemplateRenderer() {
        this.content = loadProperties(BASE + "email-content.properties");
        this.layout = loadResource(BASE + "layout.html");
    }

    /** A single piece of copy for this event, e.g. copyOf("booking-confirmed", "cta") ->
     *  "View your booking". Tokens inside it are substituted from {@code fields}. */
    String copy(String event, String field, Map<String, String> fields) {
        return substitute(content.getProperty(event + "." + field, ""), fields);
    }

    String subject(String event, Map<String, String> fields) {
        return copy(event, "subject", fields);
    }

    /** Renders the full email (layout + the event's body fragment) with every {{token}} in
     *  both substituted from {@code fields}. */
    String render(String event, Map<String, String> fields) {
        String fragment = fragmentCache.computeIfAbsent(event, e -> loadResource(BASE + e + ".html"));
        String bodyContent = substitute(fragment, fields);

        Map<String, String> layoutFields = new java.util.HashMap<>(fields);
        layoutFields.put("heading", copy(event, "heading", fields));
        layoutFields.put("preheader", copy(event, "preheader", fields));
        layoutFields.put("bodyContent", bodyContent);
        return substitute(layout, layoutFields);
    }

    private String substitute(String template, Map<String, String> fields) {
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = fields.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing email template resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Properties loadProperties(String path) {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing email template resource: " + path);
            props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }
}
