package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.BackendHealthClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class BackendHealthCheckTest {

    @RestClient
    @Inject
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

    // CPD-OFF
    @Test
    void call_shouldReturnDown_whenBackendReturnsNon200() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(503);

        final var result = this.backendHealthCheck.call();

        assertThat(result.getName()).isEqualTo("midas-api-backend");
        assertThat(result.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(result.getData()).isPresent();
        assertThat(result.getData().get()).containsEntry("status", 503L);
    }

    @Test
    void call_shouldReturnDown_whenBackendThrowsException() {
        when(this.backendHealthClient.checkHealth()).thenThrow(new ProcessingException("Connection refused"));

        final var result = this.backendHealthCheck.call();

        assertThat(result.getName()).isEqualTo("midas-api-backend");
        assertThat(result.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(result.getData()).isPresent();
        assertThat(result.getData().get()).containsEntry("error", "Connection refused");
    }
    // CPD-ON
}
