package de.vptr.midas.api.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.vptr.midas.api.exception.ApiErrorResponse;
import de.vptr.midas.api.util.IpUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

/**
 * Blocks Basic-auth requests for identities currently locked by failed-attempt limits.
 */
@ApplicationScoped
public class AuthFailureRateLimitFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final ApiErrorResponse TOO_MANY_REQUESTS_RESPONSE =
            new ApiErrorResponse(429, "Too Many Requests", "Too many invalid authentication attempts");
    private static final String TOO_MANY_REQUESTS_BODY = Json.encode(TOO_MANY_REQUESTS_RESPONSE);

    @Inject
    AuthFailureRateLimitService authFailureRateLimitService;

    @ConfigProperty(name = "midas.rate-limit.period-seconds", defaultValue = "60")
    long periodSeconds;

    @ConfigProperty(name = "app.security.trusted-proxy-ips", defaultValue = "127.0.0.1,::1,0:0:0:0:0:0:0:1")
    @Nullable
    String trustedProxyIpsConfig;

    private Set<String> trustedProxyIps;

    @PostConstruct
    void initConfig() {
        this.trustedProxyIps = IpUtil.parseTrustedProxyIps(this.trustedProxyIpsConfig);
    }

    void init(@Observes final Router router) {
        router.route().order(Integer.MIN_VALUE).handler(this::enforceAuthFailureRateLimit);
    }

    void enforceAuthFailureRateLimit(final RoutingContext routingContext) {
        final var authorizationHeader = routingContext.request().getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader != null && authorizationHeader.startsWith(BASIC_PREFIX)) {
            final var username = extractUsername(authorizationHeader);
            if (username != null) {
                final String ip = IpUtil.resolveIp(routingContext.request(), this.trustedProxyIps);

                if (Vertx.currentContext() != null) {
                    Vertx.currentContext().putLocal("midas.client-ip", ip);
                }

                final String key = username + ":" + ip;
                if (this.authFailureRateLimitService.isBlocked(key)) {
                    routingContext.response().setStatusCode(429)
                            .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                            .putHeader(HttpHeaders.RETRY_AFTER, String.valueOf(this.periodSeconds))
                            .end(TOO_MANY_REQUESTS_BODY);
                    return;
                }
            }
        }
        routingContext.next();
    }

    @Nullable
    private static String extractUsername(final String authorizationHeader) {
        final String base64Credentials = authorizationHeader.substring(BASIC_PREFIX.length());
        try {
            final var decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            final var separator = decoded.indexOf(':');
            return separator > 0 ? decoded.substring(0, separator) : null;
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
