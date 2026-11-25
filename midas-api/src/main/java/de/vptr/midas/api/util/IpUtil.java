package de.vptr.midas.api.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import io.vertx.core.http.HttpServerRequest;

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
        final String remoteHost = request.remoteAddress() != null ? request.remoteAddress().hostAddress() : null;
        if (remoteHost != null && trustedProxyIps.contains(remoteHost)) {
            final String forwarded = resolveForwardedHeader(request);
            if (forwarded != null) {
                return forwarded;
            }
        }
        return remoteHost != null ? remoteHost : "unknown";
    }

    @Nullable
    private static String resolveForwardedHeader(final HttpServerRequest request) {
        final String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            final int commaIndex = forwardedFor.indexOf(',');
            return (commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
        }
        final String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return null;
    }
}
