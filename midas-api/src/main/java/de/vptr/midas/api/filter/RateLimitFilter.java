package de.vptr.midas.api.filter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.vptr.midas.api.exception.ApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.netty.util.NetUtil;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces per-endpoint read/write request rate limits before resource matching.
 */
@Provider
@ApplicationScoped
@PreMatching
@Priority(Priorities.AUTHENTICATION - 10)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String REAL_IP_HEADER = "X-Real-IP";

    private BucketStore readBuckets;
    private BucketStore writeBuckets;
    private BucketStore authBuckets;

    @Inject
    Instance<RoutingContext> routingContextInstance;

    @ConfigProperty(name = "midas.rate-limit.read.permits", defaultValue = "100")
    long readPermits;

    @ConfigProperty(name = "midas.rate-limit.write.permits", defaultValue = "60")
    long writePermits;

    @ConfigProperty(name = "midas.rate-limit.auth.permits", defaultValue = "10")
    long authPermits;

    @ConfigProperty(name = "midas.rate-limit.period-seconds", defaultValue = "60")
    long periodSeconds;

    @ConfigProperty(name = "midas.rate-limit.bucket.max-entries", defaultValue = "10000")
    int maxBucketEntries;

    @ConfigProperty(name = "rate-limit.trust-forwarded-for", defaultValue = "false")
    boolean trustForwardedFor;

    @PostConstruct
    void initBucketStores() {
        final int maxEntries = Math.max(1, this.maxBucketEntries);
        this.readBuckets = new BucketStore(maxEntries);
        this.writeBuckets = new BucketStore(maxEntries);
        this.authBuckets = new BucketStore(maxEntries);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final String clientKey = resolveClientKey(requestContext);
        final String requestPath = requestContext.getUriInfo().getPath();
        final String endpointGroup = resolveEndpointGroup(requestPath);
        final String method = requestContext.getMethod();

        if (isAuthEndpoint(requestPath)) {
            enforceBucket(requestContext, this.authBuckets, "AUTH:" + endpointGroup + ":" + clientKey, this.authPermits,
                    "Authentication request limit exceeded");
            return;
        }

        if (isWriteMethod(method)) {
            enforceBucket(requestContext, this.writeBuckets, "WRITE:" + endpointGroup + ":" + clientKey,
                    this.writePermits, "Write request limit exceeded");
            return;
        }

        enforceBucket(requestContext, this.readBuckets, "READ:" + endpointGroup + ":" + clientKey, this.readPermits,
                "Read request limit exceeded");
    }

    private boolean enforceBucket(final ContainerRequestContext requestContext, final BucketStore bucketStore,
            final String bucketKey, final long permits, final String message) {
        final Bucket bucket = bucketStore.getOrCreate(bucketKey, () -> createBucket(permits));
        if (!bucket.tryConsume(1)) {
            requestContext.abortWith(
                    Response.status(Response.Status.TOO_MANY_REQUESTS).header("Retry-After", this.periodSeconds)
                            .entity(new ApiErrorResponse(429, "Too Many Requests", message)).build());
            return false;
        }
        return true;
    }

    private Bucket createBucket(final long permits) {
        final var bandwidth = Bandwidth.builder().capacity(permits)
                .refillGreedy(permits, Duration.ofSeconds(this.periodSeconds)).build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private static boolean isWriteMethod(final String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private static boolean isAuthEndpoint(final String path) {
        final String normalized = normalizePath(path);
        return "auth".equals(normalized) || normalized.startsWith("auth/");
    }

    private static String resolveEndpointGroup(final String path) {
        final String normalized = normalizePath(path);
        if (normalized.isBlank()) {
            return "root";
        }
        final int slashIndex = normalized.indexOf('/');
        return slashIndex > 0 ? normalized.substring(0, slashIndex) : normalized;
    }

    private static String normalizePath(final String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.startsWith("api/v1/")) {
            normalized = normalized.substring("api/v1/".length());
        }
        return normalized;
    }

    String resolveClientKey(final ContainerRequestContext requestContext) {
        if (this.trustForwardedFor) {
            final String forwardedClientAddress =
                    resolveTrustedClientAddress(requestContext.getHeaderString(FORWARDED_FOR_HEADER),
                            requestContext.getHeaderString(REAL_IP_HEADER));
            if (forwardedClientAddress != null) {
                return forwardedClientAddress;
            }
        }

        final RoutingContext routingContext = resolveRoutingContext();
        final String remoteClientAddress = resolveRemoteClientAddress(routingContext);
        if (remoteClientAddress != null) {
            return remoteClientAddress;
        }

        final String connectionClientKey = resolveConnectionClientKey(routingContext);
        if (connectionClientKey != null) {
            return connectionClientKey;
        }

        return resolveRequestClientKey(requestContext);
    }

    @Nullable
    static String resolveTrustedClientAddress(@Nullable final String forwardedFor, @Nullable final String realIp) {
        final String forwardedClientAddress = extractForwardedForClientAddress(forwardedFor);
        if (forwardedClientAddress != null) {
            return forwardedClientAddress;
        }

        return sanitizeClientAddress(realIp);
    }

    @Nullable
    static String extractForwardedForClientAddress(@Nullable final String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        final int separatorIndex = forwardedFor.indexOf(',');
        final String firstHop = separatorIndex >= 0 ? forwardedFor.substring(0, separatorIndex) : forwardedFor;
        return sanitizeClientAddress(firstHop);
    }

    @Nullable
    static String sanitizeClientAddress(@Nullable final String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            return null;
        }

        final String trimmedAddress = clientAddress.trim();
        final String normalizedAddress = unwrapBracketedIpv6Literal(trimmedAddress);
        if (NetUtil.isValidIpV4Address(normalizedAddress) || NetUtil.isValidIpV6Address(normalizedAddress)) {
            return normalizedAddress;
        }
        return null;
    }

    @Nullable
    static String resolveRemoteClientAddress(@Nullable final RoutingContext routingContext) {
        if (routingContext == null || routingContext.request() == null) {
            return null;
        }

        return resolveSocketClientAddress(routingContext.request().remoteAddress());
    }

    @Nullable
    static String resolveConnectionClientKey(@Nullable final RoutingContext routingContext) {
        if (routingContext == null || routingContext.request() == null) {
            return null;
        }

        final HttpConnection connection = routingContext.request().connection();
        if (connection == null) {
            return null;
        }

        return "connection:" + Integer.toHexString(System.identityHashCode(connection));
    }

    static String resolveRequestClientKey(final ContainerRequestContext requestContext) {
        return "request:" + Integer.toHexString(System.identityHashCode(requestContext));
    }

    @Nullable
    private RoutingContext resolveRoutingContext() {
        if (this.routingContextInstance == null || !this.routingContextInstance.isResolvable()) {
            return null;
        }

        return this.routingContextInstance.get();
    }

    @Nullable
    private static String resolveSocketClientAddress(@Nullable final SocketAddress socketAddress) {
        if (socketAddress == null || socketAddress.hostAddress() == null) {
            return null;
        }

        return sanitizeClientAddress(socketAddress.hostAddress());
    }

    private static String unwrapBracketedIpv6Literal(final String clientAddress) {
        if (clientAddress.startsWith("[") && clientAddress.endsWith("]") && clientAddress.length() > 2) {
            return clientAddress.substring(1, clientAddress.length() - 1);
        }
        return clientAddress;
    }

    private static final class BucketStore {
        private final int maxEntries;
        private final Map<String, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true);

        private BucketStore(final int maxEntries) {
            this.maxEntries = maxEntries;
        }

        private synchronized Bucket getOrCreate(final String key, final Supplier<Bucket> bucketFactory) {
            Bucket bucket = this.buckets.get(key);
            if (bucket != null) {
                return bucket;
            }

            bucket = bucketFactory.get();
            this.buckets.put(key, bucket);
            if (this.buckets.size() > this.maxEntries) {
                final var iterator = this.buckets.entrySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }

            return bucket;
        }
    }
}
