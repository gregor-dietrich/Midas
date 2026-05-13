package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.BackendHealthClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Unit tests for BackendAvailabilityService core logic.
 *
 * <p>
 * Note: SmallRye Fault Tolerance decorators (@Retry, @Timeout, @Fallback) require the Quarkus runtime and are not
 * testable in unit tests. Decorator behavior is tested via integration tests or manual testing in dev mode.
 */
@ExtendWith(MockitoExtension.class)
class BackendAvailabilityServiceTest {

    @Mock
    @Inject
    @RestClient
    BackendHealthClient backendHealthClient;

    @InjectMocks
    BackendAvailabilityService service;

    @Test
    void isBackendAvailable_shouldReturnTrue_whenBackendReturns200() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final var result = this.service.isBackendAvailable();

        assertThat(result).isTrue();
        verify(response).close();
    }

    @Test
    void isBackendAvailable_shouldReturnFalse_whenBackendReturns503() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(503);

        final var result = this.service.isBackendAvailable();

        assertThat(result).isFalse();
        verify(response).close();
    }
}
