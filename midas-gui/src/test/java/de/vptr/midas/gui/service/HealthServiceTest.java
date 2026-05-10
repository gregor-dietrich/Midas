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
 * Unit tests for BackendAvailabilityService health check scenarios.
 *
 * <p>
 * Tests the core response status checks. Decorator behavior (@Retry, @Timeout, @Fallback) is tested via manual testing
 * in dev mode or integration tests.
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @RestClient
    @Inject
    @Mock
    BackendHealthClient backendHealthClient;

    @InjectMocks
    BackendAvailabilityService backendAvailabilityService;

    @Test
    void isBackendAvailable_shouldReturnTrue_whenHealthEndpointReturns200() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final var result = this.backendAvailabilityService.isBackendAvailable();

        assertThat(result).isTrue();
        verify(response).close();
    }

    @Test
    void isBackendAvailable_shouldReturnFalse_whenHealthEndpointReturns503() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(503);

        final var result = this.backendAvailabilityService.isBackendAvailable();

        assertThat(result).isFalse();
        verify(response).close();
    }

    @Test
    void isBackendAvailable_shouldReturnFalse_whenHealthEndpointReturns500() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        final var result = this.backendAvailabilityService.isBackendAvailable();

        assertThat(result).isFalse();
        verify(response).close();
    }
}
