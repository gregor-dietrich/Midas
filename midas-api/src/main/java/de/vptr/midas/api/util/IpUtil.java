package de.vptr.midas.api.util;

import io.vertx.core.http.HttpServerRequest;

/** Utility for resolving client IP addresses from HTTP requests. */
public final class IpUtil {

    private IpUtil() {
        // Utility class
    }

    /**
     * resolves the client IP address from the given request, respecting forwarded headers if trusted.
     *
     * @param request
     *            the HTTP request
     * @param trustForwardedFor
     *            whether to trust X-Forwarded-For and X-Real-IP headers
     * @return the resolved IP address, or "unknown" if not resolvable
     */
    public static String resolveIp(final HttpServerRequest request, final boolean trustForwardedFor) {
        String ip = "unknown";
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
