package com.linkshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The input rules. Every rule is a plain check, so the rules are easy to read and to test.
 */
@Component
public class UrlValidator {

    static final int MAX_URL_LENGTH = 2048;
    private static final Pattern ALIAS = Pattern.compile("[a-z0-9][a-z0-9-]{2,31}");
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Set<String> RESERVED_ALIASES =
            Set.of("api", "admin", "health", "login", "metrics", "root", "static", "urlshortner", "v1");

    /**
     * Checks a long address and returns it in normal form.
     * Only http and https are allowed, and addresses that point inside the server's own network are refused,
     * so the shortener cannot be used to probe internal services.
     */
    public String requireTargetUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("url is required");
        }
        // Checked before trimming, because trim() would silently remove control characters at the ends.
        if (raw.chars().anyMatch(Character::isISOControl)) {
            throw ApiException.badRequest("url contains control characters");
        }
        String url = raw.trim();
        if (url.length() > MAX_URL_LENGTH) {
            throw ApiException.badRequest("url is longer than " + MAX_URL_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw ApiException.badRequest("url is not a valid web address");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw ApiException.badRequest("only http and https addresses are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw ApiException.badRequest("addresses with a user name or password are not allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw ApiException.badRequest("url has no host name");
        }
        if (isInternalHost(host)) {
            throw ApiException.badRequest("url host is not allowed");
        }
        return uri.toString();
    }

    /**
     * Checks a custom alias such as "my-docs" and returns it in lower case.
     */
    public String normaliseAlias(String raw) {
        String alias = raw.trim().toLowerCase(Locale.ROOT);
        if (!ALIAS.matcher(alias).matches()) {
            throw ApiException.badRequest("alias must be 3 to 32 letters, digits or hyphens, and start with a letter or digit");
        }
        if (RESERVED_ALIASES.contains(alias)) {
            throw ApiException.conflict("alias is reserved");
        }
        return alias;
    }

    /** Localhost, private and link-local addresses, and single-word host names such as "intranet". */
    static boolean isInternalHost(String rawHost) {
        String host = rawHost.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.contains(":")) {
            // IPv6 address: loopback, unique-local (fc00::/7) and link-local (fe80::/10)
            return host.equals("::1") || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80");
        }
        if (IPV4.matcher(host).matches()) {
            return isInternalIpv4(host);
        }
        return host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || !host.contains(".");
    }

    private static boolean isInternalIpv4(String host) {
        String[] parts = host.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 0 || first == 10 || first == 127
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }
}
