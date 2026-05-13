package de.vptr.midas.api.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;

@SuppressWarnings("unchecked")
class RateLimitFilterClientKeyTest {

    @Test
    void testResolveTrustedClientAddressUsesForwardedForFirstHop() {
        assertEquals("198.51.100.31",
                RateLimitFilter.resolveTrustedClientAddress("198.51.100.31, 10.0.0.5", "198.51.100.99"));
    }

    @Test
    void testResolveTrustedClientAddressFallsBackToValidatedRealIp() {
        assertEquals("2001:db8::32", RateLimitFilter.resolveTrustedClientAddress("spoofed-hop", "[2001:db8::32]"));
    }

    @Test
    void testResolveTrustedClientAddressRejectsMalformedForwardedHeaders() {
        assertNull(RateLimitFilter.resolveTrustedClientAddress("198.51.100.31:8443", "bad-real-ip"));
    }

    @Test
    void testResolveClientKeyUsesRemoteAddressWhenForwardedHeadersInvalid() {
        final RateLimitFilter filter = new RateLimitFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Instance<RoutingContext> routingContextInstance = mock(Instance.class);
        final RoutingContext routingContext = mock(RoutingContext.class);
        final HttpServerRequest request = mock(HttpServerRequest.class);
        final SocketAddress remoteAddress = mock(SocketAddress.class);

        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("spoofed-hop");
        when(requestContext.getHeaderString("X-Real-IP")).thenReturn("198.51.100.31:8443");
        when(routingContextInstance.isResolvable()).thenReturn(true);
        when(routingContextInstance.get()).thenReturn(routingContext);
        when(routingContext.request()).thenReturn(request);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.hostAddress()).thenReturn("203.0.113.70");

        filter.setRoutingContextInstanceForTest(routingContextInstance);

        assertEquals("203.0.113.70", filter.resolveClientKey(requestContext));
    }

    @Test
    void testResolveClientKeyFallsBackToConnectionIdentityWhenRemoteAddressMissing() {
        final RateLimitFilter filter = new RateLimitFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Instance<RoutingContext> routingContextInstance = mock(Instance.class);
        final RoutingContext routingContext = mock(RoutingContext.class);
        final HttpServerRequest request = mock(HttpServerRequest.class);
        final HttpConnection connection = mock(HttpConnection.class);

        when(requestContext.getHeaderString(anyString())).thenReturn(null);
        when(routingContextInstance.isResolvable()).thenReturn(true);
        when(routingContextInstance.get()).thenReturn(routingContext);
        when(routingContext.request()).thenReturn(request);
        when(request.remoteAddress()).thenReturn(null);
        when(request.connection()).thenReturn(connection);

        filter.setRoutingContextInstanceForTest(routingContextInstance);

        assertEquals("connection:" + Integer.toHexString(System.identityHashCode(connection)),
                filter.resolveClientKey(requestContext));
    }

    @Test
    void testResolveClientKeyFallsBackToRequestIdentityWhenRoutingContextUnavailable() {
        final RateLimitFilter filter = new RateLimitFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Instance<RoutingContext> routingContextInstance = mock(Instance.class);

        when(requestContext.getHeaderString(anyString())).thenReturn(null);
        when(routingContextInstance.isResolvable()).thenReturn(false);

        filter.setRoutingContextInstanceForTest(routingContextInstance);

        final String clientKey = filter.resolveClientKey(requestContext);

        assertTrue(clientKey.startsWith("request:"));
        assertNotEquals("unknown", clientKey);
    }
}
