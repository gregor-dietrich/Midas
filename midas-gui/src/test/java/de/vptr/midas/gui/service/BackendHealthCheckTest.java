package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.BackendHealthClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class BackendHealthCheckTest {

    @Mock
    BackendHealthClient backendHealthClient;

    @InjectMocks
    BackendHealthCheck backendHealthCheck;

    @Test
    void call_shouldReturnUp_whenBackendReturns200() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final var result = this.backendHealthCheck.call();

        assertThat(result.getName()).isEqualTo("midas-api-backend");
        assertThat(result.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    void call_shouldReturnDown_whenBackendReturnsNon200() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(503);

        final var result = this.backendHealthCheck.call();

        assertHealthCheckDown(result, Map.of("status", 503L));
    }

    @Test
    void call_shouldReturnDown_whenBackendThrowsException() {
        when(this.backendHealthClient.checkHealth()).thenThrow(new ProcessingException("Connection refused"));

        final var result = this.backendHealthCheck.call();

        assertHealthCheckDown(result, Map.of("error", "Connection refused"));
    }

    private static void assertHealthCheckDown(final HealthCheckResponse result,
            final Map<String, Object> expectedData) {
        assertThat(result.getName()).isEqualTo("midas-api-backend");
        assertThat(result.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(result.getData()).isPresent();
        expectedData.forEach((key, value) -> assertThat(result.getData().get()).containsEntry(key, value));
    }
}
