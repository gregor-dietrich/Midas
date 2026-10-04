package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ServiceHelperTest {

    private static final Logger LOG = Logger.getLogger(ServiceHelperTest.class);
    private static final String OPERATION = "testing";

    @Mock
    AuthService authService;

    /** A client failure with this status, whose body (if any) carries a structured error message. */
    private static WebApplicationException failure(final int status, @Nullable final String message) {
        final var statusInfo = mock(Response.StatusType.class);
        lenient().when(statusInfo.getStatusCode()).thenReturn(status);
        final var response = mock(Response.class);
        lenient().when(response.getStatusInfo()).thenReturn(statusInfo);
        lenient().when(response.getStatus()).thenReturn(status);
        lenient().when(response.hasEntity()).thenReturn(message != null);
        lenient().when(response.readEntity(String.class)).thenReturn("{\"message\":\"" + message + "\"}");
        return new WebApplicationException(response);
    }

    private static <T> Supplier<T> throwing(final RuntimeException exception) {
        return () -> {
            throw exception;
        };
    }

    @Test
    void execute_returnsValue() {
        assertThat(ServiceHelper.execute(() -> "value", this.authService, LOG, OPERATION)).isEqualTo("value");
    }

    @Test
    void execute_reportsConnectionFailure() {
        final var cause = new ProcessingException("refused");

        assertThatThrownBy(() -> ServiceHelper.execute(throwing(cause), this.authService, LOG, OPERATION))
                .isInstanceOf(ServiceException.class).hasMessage("Backend connection failed").hasCause(cause);
    }

    @Test
    void execute_logsOutAndThrows_on401() {
        assertThatThrownBy(() -> ServiceHelper.execute(throwing(failure(401, null)), this.authService, LOG, OPERATION))
                .isInstanceOf(AuthenticationException.class).hasMessage("Session expired");

        verify(this.authService).logout();
    }

    /** The headers factory throws before any request is sent when the session holds no credentials. */
    @Test
    void execute_passesAuthenticationExceptionThrough_withoutLoggingOut() {
        final var unauthenticated = new AuthenticationException("Authentication required");

        assertThatThrownBy(() -> ServiceHelper.execute(throwing(unauthenticated), this.authService, LOG, OPERATION))
                .isSameAs(unauthenticated);

        verify(this.authService, never()).logout();
    }

    @Test
    void execute_passesServiceExceptionThrough() {
        final var conflict = new ServiceException("Already there");

        assertThatThrownBy(() -> ServiceHelper.execute(throwing(conflict), this.authService, LOG, OPERATION))
                .isSameAs(conflict);
    }

    @Test
    void execute_wrapsUnexpectedRuntimeException() {
        assertThatThrownBy(() -> ServiceHelper.execute(throwing(new IllegalStateException("boom")), this.authService,
                LOG, OPERATION)).isInstanceOf(ServiceException.class).hasMessage("Unexpected error")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "prefix={0} body={1}")
    @CsvSource(nullValues = "null",
            value = { "Failed to save, name taken, Failed to save: name taken", "null, name taken, name taken",
                    "'', name taken, name taken", "null, null, Backend error: 422",
                    "Failed to save, null, Failed to save: HTTP 422" })
    void execute_reportsValidationMessage_on422(@Nullable final String prefix, @Nullable final String message,
            final String expected) {
        assertThatThrownBy(
                () -> ServiceHelper.execute(throwing(failure(422, message)), this.authService, LOG, OPERATION, prefix))
                .isInstanceOf(ServiceException.class).hasMessage(expected);
    }

    @Test
    void execute_reportsGenericFailure_onOtherStatus() {
        assertThatThrownBy(() -> ServiceHelper.execute(throwing(failure(500, "boom")), this.authService, LOG, OPERATION,
                "Failed to save")).isInstanceOf(ServiceException.class).hasMessage("Backend request failed");
    }

    @Test
    void executeOptional_wrapsValue() {
        assertThat(ServiceHelper.executeOptional(() -> "value", this.authService, LOG, OPERATION)).contains("value");
    }

    @Test
    void executeOptional_returnsEmpty_on404() {
        assertThat(ServiceHelper.executeOptional(throwing(failure(404, "gone")), this.authService, LOG, OPERATION))
                .isEmpty();
    }

    @Test
    void executeOptional_stillThrows_onOtherStatus() {
        assertThatThrownBy(
                () -> ServiceHelper.executeOptional(throwing(failure(500, null)), this.authService, LOG, OPERATION))
                .isInstanceOf(ServiceException.class).hasMessage("Backend request failed");
    }

    @Test
    void executeVoid_runsAction_andTranslatesFailures() {
        final var action = mock(Runnable.class);

        ServiceHelper.executeVoid(action, this.authService, LOG, OPERATION);

        verify(action).run();
        assertThatThrownBy(() -> ServiceHelper.executeVoid(() -> {
            throw failure(404, null);
        }, this.authService, LOG, OPERATION)).isInstanceOf(ServiceException.class).hasMessage("Backend request failed");
    }

    @Test
    void executeVoidWithNotFoundMessage_succeeds_whenActionReturns() {
        assertThatCode(() -> ServiceHelper.executeVoid(() -> {
        }, this.authService, LOG, OPERATION, "Thing not found")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "404, Thing not found", "500, Backend request failed" })
    void executeVoidWithNotFoundMessage_usesMessageOnlyFor404(final int status, final String expected) {
        assertThatThrownBy(() -> ServiceHelper.executeVoid(() -> {
            throw failure(status, null);
        }, this.authService, LOG, OPERATION, "Thing not found")).isInstanceOf(ServiceException.class)
                .hasMessage(expected);
    }
}
