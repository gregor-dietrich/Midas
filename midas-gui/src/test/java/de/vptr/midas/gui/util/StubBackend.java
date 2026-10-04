package de.vptr.midas.gui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import jakarta.annotation.Nullable;

/**
 * An in-process stub of the Midas API, used to pin GUI service behaviour against real HTTP rather than against a mocked
 * client interface. Because the assertions describe what the service does with a given HTTP response, they stay valid
 * when the client interfaces themselves are replaced.
 *
 * <p>
 * Routes are matched on HTTP method plus a path <em>suffix</em>, so a stub survives a change to the base path (for
 * example a client that moves from {@code /pages} to {@code /api/v1/pages}).
 */
public class StubBackend implements QuarkusTestResourceLifecycleManager {

    /** A request the stub received. */
    public record Recorded(String method, String path, @Nullable String query, @Nullable String authorization,
            String body) {

        private static final ObjectMapper JSON = new ObjectMapper();

        /** Returns the query parameters, decoded as received; empty when there is no query. */
        public Map<String, String> queryParams() {
            final String raw = this.query;
            return raw == null ? Map.of() : Arrays.stream(raw.split("&")).map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(pair -> pair[0], pair -> pair.length > 1 ? pair[1] : ""));
        }

        /** Returns the body parsed as JSON. */
        public JsonNode jsonBody() {
            try {
                return JSON.readTree(this.body);
            } catch (final JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private record Route(String method, String suffix) {
    }

    private record Stubbed(int status, @Nullable String body) {
    }

    private static final Map<Route, Stubbed> ROUTES = new ConcurrentHashMap<>();
    private static final List<Recorded> REQUESTS = new CopyOnWriteArrayList<>();

    @Nullable
    private static HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            final HttpServer created = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            created.createContext("/", StubBackend::handle);
            created.start();
            server = created;
            final String base = "http://" + created.getAddress().getAddress().getHostAddress() + ":"
                    + created.getAddress().getPort();
            // Both the rest-client base URL and its health variant point at the stub so that no
            // test accidentally reaches a real backend.
            return Map.of("quarkus.rest-client.midas-api.url", base, "quarkus.rest-client.midas-api-health.url", base);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not start stub backend", e);
        }
    }

    @Override
    public void stop() {
        final HttpServer running = server;
        if (running != null) {
            running.stop(0);
            server = null;
        }
    }

    /** Clears all stubbed routes and recorded requests. Call from {@code @BeforeEach}. */
    public static void reset() {
        ROUTES.clear();
        REQUESTS.clear();
    }

    /** Stubs a response for the given method and path suffix. */
    public static void stub(final String method, final String pathSuffix, final int status,
            @Nullable final String body) {
        ROUTES.put(new Route(method, pathSuffix), new Stubbed(status, body));
    }

    /** Returns the requests the stub received, in arrival order. */
    public static List<Recorded> requests() {
        return List.copyOf(REQUESTS);
    }

    /** Returns the single request received, failing if there was not exactly one. */
    public static Recorded onlyRequest() {
        final List<Recorded> seen = requests();
        if (seen.size() != 1) {
            throw new AssertionError("Expected exactly one request but got " + seen.size() + ": " + seen);
        }
        return seen.get(0);
    }

    private static void handle(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();
        final String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        REQUESTS.add(new Recorded(exchange.getRequestMethod(), path, exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"), body));

        final Stubbed stubbed = resolve(exchange.getRequestMethod(), path);
        if (stubbed == null) {
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
            return;
        }

        final byte[] payload = stubbed.body() == null ? new byte[0] : stubbed.body().getBytes(StandardCharsets.UTF_8);
        if (payload.length > 0) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(stubbed.status(), payload.length);
            exchange.getResponseBody().write(payload);
        } else {
            exchange.sendResponseHeaders(stubbed.status(), -1);
        }
        exchange.close();
    }

    /** Resolves the most specific matching route, preferring the longest suffix. */
    @Nullable
    private static Stubbed resolve(final String method, final String path) {
        final List<Map.Entry<Route, Stubbed>> matches = new ArrayList<>();
        for (final Map.Entry<Route, Stubbed> entry : ROUTES.entrySet()) {
            if (entry.getKey().method().equals(method) && path.endsWith(entry.getKey().suffix())) {
                matches.add(entry);
            }
        }
        return matches.stream().max(Comparator.comparingInt(e -> e.getKey().suffix().length())).map(Map.Entry::getValue)
                .orElse(null);
    }
}
