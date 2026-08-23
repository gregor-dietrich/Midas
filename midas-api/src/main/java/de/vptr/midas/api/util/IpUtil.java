package de.vptr.midas.api.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Nullable;

/** Utility for resolving client IP addresses from HTTP requests. */
public final class IpUtil {

    private IpUtil() {
        // Utility class
    }

    /**
     * Parses a comma-separated list of IP addresses into a Set.
     *
     * @param config
     *            the configuration string
     * @return a set of trusted proxy IP addresses
     */
    // The loopback defaults are IP literals by definition; this is the one place that
    // is allowed to name them.
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static Set<String> parseTrustedProxyIps(@Nullable final String config) {
        if (config == null || config.isBlank()) {
            return Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
        }
        return Arrays.stream(config.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * resolves the client IP address from the given request, respecting forwarded headers if trusted.
     *
     * @param request
     *            the HTTP request
     * @param trustedProxyIps
     *            a set of IP addresses trusted to provide X-Forwarded-For and X-Real-IP headers
     * @return the resolved IP address, or "unknown" if not resolvable
     */
    public static String resolveIp(final HttpServerRequest request, final Set<String> trustedProxyIps) {
        String ip = "unknown";
        final boolean trustForwardedFor =
                request.remoteAddress() != null && trustedProxyIps.contains(request.remoteAddress().hostAddress());
        final String forwardedFor = trustForwardedFor ? request.getHeader("X-Forwarded-For") : null;
        final String realIp = trustForwardedFor ? request.getHeader("X-Real-IP") : null;

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            final int commaIndex = forwardedFor.indexOf(',');
            ip = (commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
        } else if (realIp != null && !realIp.isBlank()) {
            ip = realIp.trim();
        } else if (request.remoteAddress() != null) {
            ip = request.remoteAddress().hostAddress();
        }
        return ip != null ? ip : "unknown";
    }
}
