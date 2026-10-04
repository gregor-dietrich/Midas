package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.MidasClientHeadersFactory;
import de.vptr.midas.gui.dto.AuthResultDto;
import de.vptr.midas.gui.util.StubBackend;
import de.vptr.midas.gui.util.UiAsyncExecutor;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Pins the credential flow end to end with the real {@link AuthService}: the login check against the auth endpoint, and
 * the header {@link MidasClientHeadersFactory} adds to a generated client's call from a worker thread.
 *
 * <p>
 * Kept apart from the {@code AbstractHttpContractTest} subclasses, which replace {@link AuthService} with a mock.
 */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class AuthServiceHttpContractTest {

    private static final String USERNAME = "member";
    private static final String PASSWORD = "s3cret";
    private static final String CREDENTIALS =
            "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    @Inject
    AuthService authService;

    @Inject
    AccountService accountService;

    private final Map<String, Object> attributes = new HashMap<>();

    /** Held here because VaadinSession.setCurrent keeps only a weak reference to it. */
    private VaadinSession session;

    @BeforeEach
    void setUpSession() {
        StubBackend.reset();
        this.attributes.clear();
        this.session = mock(VaadinSession.class);
        when(this.session.getAttribute(anyString()))
                .thenAnswer(call -> this.attributes.get(call.<String>getArgument(0)));
        doAnswer(call -> this.attributes.put(call.getArgument(0), call.getArgument(1))).when(this.session)
                .setAttribute(anyString(), any());
        VaadinSession.setCurrent(this.session);
    }

    @AfterEach
    void clearSession() {
        VaadinSession.setCurrent(null);
        assertThat(StubBackend.requests())
                .allSatisfy(request -> assertThat(request.path()).startsWith(StubBackend.API_PREFIX));
    }

    @Test
    void authenticate_validatesTheEnteredCredentialsAtTheAuthEndpoint() {
        StubBackend.stub("HEAD", "/auth", 200, null);

        assertThat(this.authService.authenticate(USERNAME, PASSWORD).getStatus())
                .isEqualTo(AuthResultDto.Status.SUCCESS);

        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo("HEAD");
        assertThat(request.path()).isEqualTo("/api/v1/auth");
        assertThat(request.authorization()).isEqualTo(CREDENTIALS);
    }

    /**
     * Calls a service the way {@link UiAsyncExecutor} does: on a worker thread that has the caller's session made
     * current, while the caller's thread has none. The header can only be found if the headers factory runs on that
     * worker thread, as {@code VaadinSession.getCurrent()} is thread-local.
     */
    @Test
    void generatedClient_sendsTheSessionsCredentials_fromWorkerThread() throws Exception {
        StubBackend.stub("HEAD", "/auth", 200, null);
        StubBackend.stub("GET", "/accounts/my", 200, "[]");
        assertThat(this.authService.authenticate(USERNAME, PASSWORD).getStatus())
                .isEqualTo(AuthResultDto.Status.SUCCESS);

        final var session = VaadinSession.getCurrent();
        final var tccl = Thread.currentThread().getContextClassLoader();
        VaadinSession.setCurrent(null);
        try (var worker = Executors.newSingleThreadExecutor()) {
            final var accounts = CompletableFuture.supplyAsync(() -> {
                VaadinSession.setCurrent(session);
                Thread.currentThread().setContextClassLoader(tccl);
                try {
                    return this.accountService.getMyAccounts();
                } finally {
                    VaadinSession.setCurrent(null);
                }
            }, worker).get(UiAsyncExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(accounts).isEmpty();
        }

        assertThat(StubBackend.requests()).extracting(StubBackend.Recorded::method, StubBackend.Recorded::path,
                StubBackend.Recorded::authorization).containsExactly(tuple("HEAD", "/api/v1/auth", CREDENTIALS),
                        tuple("GET", "/api/v1/accounts/my", CREDENTIALS));
    }
}
