package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import de.vptr.midas.gui.service.AuthService;
import io.quarkus.test.InjectMock;

/**
 * Base for tests that pin a GUI service's behaviour against real HTTP responses from {@link StubBackend}.
 *
 * <p>
 * The assertions describe what a service does with a given HTTP status and payload, not how it calls its REST client,
 * so they remain meaningful when the client interfaces are regenerated and the Authorization header moves out of the
 * method signature. Subclasses carry {@code @QuarkusTest} and
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
        when(this.authService.getAuthorizationHeader()).thenReturn(AUTH_HEADER);
    }

    /** The invariant that must survive the Authorization header moving out of the client method signatures. */
    @AfterEach
    void everyRequestSendsCredentials() {
        assertThat(StubBackend.requests())
                .allSatisfy(request -> assertThat(request.authorization()).isEqualTo(AUTH_HEADER));
    }
}
