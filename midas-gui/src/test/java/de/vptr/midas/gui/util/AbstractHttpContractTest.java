package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import de.vptr.midas.gui.client.MidasClientHeadersFactory;
import de.vptr.midas.gui.service.AuthService;
import io.quarkus.test.InjectMock;

/**
 * Base for tests that pin a GUI service's behaviour against real HTTP responses from {@link StubBackend}.
 *
 * <p>
 * The assertions describe what a service does with a given HTTP status and payload, not how it calls its REST client.
 * The generated clients take no Authorization parameter: {@link MidasClientHeadersFactory} adds the header from
 * {@link AuthService#requireAuthorizationHeader()}, which is mocked here to return {@link #AUTH_HEADER}. Subclasses
 * carry {@code @QuarkusTest} and
 * {@code @QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)}.
 */
@SuppressWarnings("NullAway")
public abstract class AbstractHttpContractTest {

    protected static final String AUTH_HEADER = "Basic dGVzdDp0ZXN0";

    @InjectMock
    protected AuthService authService;

    @BeforeEach
    void setUpStubBackend() {
        StubBackend.reset();
        when(this.authService.requireAuthorizationHeader()).thenReturn(AUTH_HEADER);
    }

    /**
     * Every request goes to the versioned API and carries the credentials the headers factory supplies. The routes in
     * {@link StubBackend} match on a path suffix, so this is where the {@code /api/v1/} prefix is pinned.
     */
    @AfterEach
    void everyRequestTargetsTheApiWithCredentials() {
        assertThat(StubBackend.requests()).allSatisfy(request -> {
            assertThat(request.path()).startsWith(StubBackend.API_PREFIX);
            assertThat(request.authorization()).isEqualTo(AUTH_HEADER);
        });
    }
}
