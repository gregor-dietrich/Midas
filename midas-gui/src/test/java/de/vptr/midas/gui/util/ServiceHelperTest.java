package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ServiceHelperTest {

    private static final Logger LOG = Logger.getLogger(ServiceHelperTest.class);

    @Mock
    AuthService authService;

    @Mock
    Response response;

    @Test
    void executeWithResponse_shouldCloseResponseAfterSuccessfulRead() {
        when(this.response.getStatus()).thenReturn(200);
        when(this.response.readEntity(String.class)).thenReturn("payload");

        final var result =
                ServiceHelper.executeWithResponse(() -> this.response, this.authService, LOG, "reading payload",
                        currentResponse -> ServiceHelper.readEntity(currentResponse, String.class, "test payload"));

        assertThat(result).isEqualTo("payload");
        verify(this.response).close();
    }

    @Test
    void executeWithResponse_shouldHandleRaw401ResponseAndCloseIt() {
        when(this.response.getStatus()).thenReturn(401);

        assertThatThrownBy(() -> ServiceHelper.executeWithResponse(() -> this.response, this.authService, LOG,
                "reading payload", e -> "ignored")).isInstanceOf(AuthenticationException.class)
                .hasMessage("Session expired");

        verify(this.authService).logout();
        verify(this.response).close();
    }

    @Test
    void executeWithResponse_shouldTranslateWebApplicationExceptionWithBackendBody() {
        final var statusInfo = Mockito.mock(Response.StatusType.class);
        when(this.response.getStatus()).thenReturn(400);
        when(this.response.getStatusInfo()).thenReturn(statusInfo);
        when(statusInfo.getStatusCode()).thenReturn(400);
        when(this.response.hasEntity()).thenReturn(true);
        when(this.response.readEntity(String.class)).thenReturn("{\"message\":\"Invalid account payload\"}");

        assertThatThrownBy(() -> ServiceHelper.executeWithResponse(() -> {
            throw new WebApplicationException(this.response);
        }, this.authService, LOG, "creating account", "Failed to create account", e -> "ignored"))
                .isInstanceOf(ServiceException.class).hasMessage("Backend request failed");

        verify(this.response).bufferEntity();
        verify(this.response).close();
    }

    @Test
    void execute_shouldTranslateWebApplicationExceptionWithoutPrefixUsingBackendBody() {
        final var statusInfo = Mockito.mock(Response.StatusType.class);
        when(this.response.getStatus()).thenReturn(500);
        when(this.response.getStatusInfo()).thenReturn(statusInfo);
        when(statusInfo.getStatusCode()).thenReturn(500);
        when(this.response.hasEntity()).thenReturn(true);
        when(this.response.readEntity(String.class)).thenReturn("{\"message\":\"Page lookup failed\"}");

        assertThatThrownBy(() -> ServiceHelper.execute(() -> {
            throw new WebApplicationException(this.response);
        }, this.authService, LOG, "fetching page")).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");

        verify(this.response).bufferEntity();
        verify(this.response).close();
    }

    @Test
    void executeWithResponse_shouldCloseResponseWhenEntityParsingFails() {
        when(this.response.getStatus()).thenReturn(200);
        when(this.response.readEntity(String.class)).thenThrow(new IllegalStateException("invalid body"));

        assertThatThrownBy(
                () -> ServiceHelper.executeWithResponse(() -> this.response, this.authService, LOG, "reading payload",
                        currentResponse -> ServiceHelper.readEntity(currentResponse, String.class, "test payload")))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to read test payload");

        verify(this.response).close();
    }

    @Test
    void readEntity_shouldWrapGenericTypeParsingFailures() {
        final var entityType = new GenericType<List<String>>() {
        };
        when(this.response.readEntity(entityType)).thenThrow(new IllegalStateException("invalid list"));

        assertThatThrownBy(() -> ServiceHelper.readEntity(this.response, entityType, "string list"))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to read string list")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void execute_shouldWrapUnexpectedRuntimeException() {
        assertThatThrownBy(() -> ServiceHelper.execute(() -> {
            throw new IllegalStateException("boom");
        }, this.authService, LOG, "syncing payload")).isInstanceOf(ServiceException.class)
                .hasMessage("Unexpected error").hasCauseInstanceOf(IllegalStateException.class);
    }
}
