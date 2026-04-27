package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

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
    }

    @Test
    void isBackendAvailable_shouldReturnFalse_whenHealthEndpointReturns503() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(503);

        final var result = this.backendAvailabilityService.isBackendAvailable();

        assertThat(result).isFalse();
    }

    @Test
    void isBackendAvailable_shouldReturnFalse_whenHealthEndpointReturns500() {
        final var response = Mockito.mock(Response.class);
        when(this.backendHealthClient.checkHealth()).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        final var result = this.backendAvailabilityService.isBackendAvailable();

        assertThat(result).isFalse();
    }

    @Test
    void isBackendAvailable_shouldPropagateProcessingException_whenConnectionFails() {
        when(this.backendHealthClient.checkHealth()).thenThrow(new ProcessingException("Connection refused"));

        assertThatThrownBy(() -> this.backendAvailabilityService.isBackendAvailable())
                .isInstanceOf(ProcessingException.class)
                .hasMessage("Connection refused");
    }

    @Test
    void isBackendAvailable_shouldPropagateRuntimeException_whenUnexpectedExceptionOccurs() {
        when(this.backendHealthClient.checkHealth()).thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> this.backendAvailabilityService.isBackendAvailable())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error");
    }
}
