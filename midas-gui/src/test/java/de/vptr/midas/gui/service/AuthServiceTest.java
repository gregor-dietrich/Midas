package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.dto.AuthResultDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@SuppressWarnings({ "NullAway", "PMD.LinguisticNaming" })
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @RestClient
    @Inject
    @Mock
    AuthClient authClient;

    @Mock
    UserRankService userRankService;

    @Mock
    SessionAuthorizationStore sessionAuthorizationStore;

    @InjectMocks
    AuthService authService;

    // CPD-OFF
    private VaadinSession vaadinSession;
    private MockedStatic<VaadinSession> vaadinSessionMock;

    @BeforeEach
    void setUp() {
        this.vaadinSession = Mockito.mock(VaadinSession.class);
        this.vaadinSessionMock = mockStatic(VaadinSession.class);
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(this.vaadinSession);
    }

    @AfterEach
    void tearDown() {
        this.vaadinSessionMock.close();
    }
    // CPD-ON

    @Test
    void authenticate_shouldReturnSuccess_whenCredentialsAreValid() {
        // Given
        final String username = "testuser";
        final String password = "testpass";
        final String oldSessionId = "old-session";
        final String newSessionId = "new-session";
        final String expectedHeader = "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn(oldSessionId);
        when(this.sessionAuthorizationStore.createSession(expectedHeader)).thenReturn(newSessionId);

        // When
        final AuthResultDto result = this.authService.authenticate(username, password);

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.SUCCESS);
        verify(this.sessionAuthorizationStore).revokeSession(oldSessionId);
        verify(this.sessionAuthorizationStore).createSession(expectedHeader);
        verify(this.vaadinSession).setAttribute("authenticated.username", username);
        verify(this.vaadinSession).setAttribute("authenticated.session-id", newSessionId);
        verify(this.vaadinSession).setAttribute("authenticated.status", true);
    }

    @Test
    void authenticate_shouldReturnInvalidInput_whenUsernameIsEmpty() {
        // When
        final AuthResultDto result = this.authService.authenticate("", "password");

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_INPUT);
    }

    @Test
    void authenticate_shouldReturnInvalidInput_whenPasswordIsEmpty() {
        // When
        final AuthResultDto result = this.authService.authenticate("username", "");

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_INPUT);
    }

    @Test
    void authenticate_shouldReturnInvalidInput_whenUsernameIsNull() {
        // When
        final AuthResultDto result = this.authService.authenticate(null, "password");

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_INPUT);
    }

    @Test
    void authenticate_shouldReturnInvalidInput_whenPasswordIsNull() {
        // When
        final AuthResultDto result = this.authService.authenticate("username", null);

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_INPUT);
    }

    @Test
    void authenticate_shouldReturnInvalidCredentials_when401Received() {
        // Given
        final String username = "testuser";
        final String password = "wrongpass";

        final Response response = Mockito.mock(Response.class);
        final Response.StatusType statusType = Mockito.mock(Response.StatusType.class);

        when(response.getStatus()).thenReturn(401);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getStatusCode()).thenReturn(401);

        final WebApplicationException exception = new WebApplicationException(response);
        doThrow(exception).when(this.authClient).validateCredentials(anyString());

        // When
        final AuthResultDto result = this.authService.authenticate(username, password);

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_CREDENTIALS);
    }

    // CPD-OFF
    @Test
    void authenticate_shouldReturnBackendUnavailable_whenProcessingExceptionWithConnectException() {
        // Given
        final String username = "testuser";
        final String password = "testpass";

        final ProcessingException exception =
                new ProcessingException("Connection failed", new ConnectException("Connection refused"));
        doThrow(exception).when(this.authClient).validateCredentials(anyString());

        // When
        final AuthResultDto result = this.authService.authenticate(username, password);

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.BACKEND_UNAVAILABLE);
        assertThat(result.getMessage()).isEqualTo("Backend service unavailable. Please try again later.");
    }

    @Test
    void authenticate_shouldReturnBackendUnavailable_whenProcessingException() {
        // Given
        final String username = "testuser";
        final String password = "testpass";

        final ProcessingException exception = new ProcessingException("Network error");
        doThrow(exception).when(this.authClient).validateCredentials(anyString());

        // When
        final AuthResultDto result = this.authService.authenticate(username, password);

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.BACKEND_UNAVAILABLE);
        assertThat(result.getMessage()).isEqualTo("Backend service unavailable. Please try again later.");
    }
    // CPD-ON

    @Test
    void authenticate_shouldReturnBackendUnavailable_whenSessionUnavailableAfterValidation() {
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(null);

        final AuthResultDto result = this.authService.authenticate("testuser", "testpass");

        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.BACKEND_UNAVAILABLE);
        assertThat(result.getMessage()).isEqualTo("Backend service unavailable. Please try again later.");
        verify(this.sessionAuthorizationStore, never()).createSession(anyString());
    }

    @Test
    void authenticate_shouldReturnBackendUnavailable_whenUnexpectedRuntimeExceptionOccurs() {
        doThrow(new IllegalStateException("secret backend detail")).when(this.authClient)
                .validateCredentials(anyString());

        final AuthResultDto result = this.authService.authenticate("testuser", "testpass");

        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.BACKEND_UNAVAILABLE);
        assertThat(result.getMessage()).isEqualTo("Backend service unavailable. Please try again later.");
    }

    @Test
    void logout_shouldCloseSession() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");

        // When
        this.authService.logout();

        // Then - Verify that user rank cache is cleared and session is closed
        verify(this.userRankService).clearCachedUserRank();
        verify(this.sessionAuthorizationStore).revokeSession("session-1");
        verify(this.vaadinSession).setAttribute("authenticated.username", null);
        verify(this.vaadinSession).setAttribute("authenticated.session-id", null);
        verify(this.vaadinSession).setAttribute("authenticated.status", false);
        verify(this.vaadinSession).close();
    }

    @Test
    void isAuthenticated_shouldReturnTrue_whenSessionHasAuthenticatedStatus() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");
        when(this.sessionAuthorizationStore.resolveAuthorization("session-1")).thenReturn("Basic abc");

        // When
        final boolean result = this.authService.isAuthenticated();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAuthenticated_shouldReturnFalse_whenSessionHasNoAuthenticatedStatus() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(null);

        // When
        final boolean result = this.authService.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_shouldReturnFalseAndClearState_whenAuthorizationMappingIsMissing() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");
        when(this.sessionAuthorizationStore.resolveAuthorization("session-1")).thenReturn(null);

        // When
        final boolean result = this.authService.isAuthenticated();

        // Then
        assertThat(result).isFalse();
        verify(this.vaadinSession).setAttribute("authenticated.username", null);
        verify(this.vaadinSession).setAttribute("authenticated.session-id", null);
        verify(this.vaadinSession).setAttribute("authenticated.status", false);
    }

    @Test
    void isAuthenticated_shouldReturnFalseAndClearState_whenSessionIdIsMissing() {
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn(null);

        final boolean result = this.authService.isAuthenticated();

        assertThat(result).isFalse();
        verify(this.sessionAuthorizationStore, never()).resolveAuthorization(anyString());
        verify(this.vaadinSession).setAttribute("authenticated.username", null);
        verify(this.vaadinSession).setAttribute("authenticated.session-id", null);
        verify(this.vaadinSession).setAttribute("authenticated.status", false);
    }

    @Test
    void isAuthenticated_shouldReturnFalse_whenSessionHasAuthenticatedStatusFalse() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(false);

        // When
        final boolean result = this.authService.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getAuthorizationHeader_shouldReturnNull_whenNotAuthenticated() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(false);

        // When
        final String result = this.authService.getAuthorizationHeader();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getAuthorizationHeader_shouldReturnNull_whenHeaderIsMissing() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");
        when(this.sessionAuthorizationStore.resolveAuthorization("session-1")).thenReturn(null);

        // When
        final String result = this.authService.getAuthorizationHeader();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getAuthorizationHeader_shouldReturnStoredHeader_whenAuthenticated() {
        // Given
        final String expectedHeader = "Basic dGVzdHVzZXI6dGVzdHBhc3M=";
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");
        when(this.sessionAuthorizationStore.resolveAuthorization("session-1")).thenReturn(expectedHeader);

        // When
        final String result = this.authService.getAuthorizationHeader();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedHeader);
    }

    @Test
    void requireAuthorizationHeader_shouldReturnStoredHeader_whenAuthenticated() {
        final String expectedHeader = "Basic dGVzdHVzZXI6dGVzdHBhc3M=";
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(true);
        when(this.vaadinSession.getAttribute("authenticated.session-id")).thenReturn("session-1");
        when(this.sessionAuthorizationStore.resolveAuthorization("session-1")).thenReturn(expectedHeader);

        final String result = this.authService.requireAuthorizationHeader();

        assertThat(result).isEqualTo(expectedHeader);
    }

    @Test
    void requireAuthorizationHeader_shouldThrowAuthenticationException_whenAuthorizationMissing() {
        when(this.vaadinSession.getAttribute("authenticated.status")).thenReturn(false);

        Assertions.assertThatThrownBy(() -> this.authService.requireAuthorizationHeader())
                .isInstanceOf(AuthenticationException.class).hasMessage("Authentication required");
    }

    @Test
    void getUsername_shouldReturnStoredUsername() {
        // Given
        final String expectedUsername = "testuser";
        when(this.vaadinSession.getAttribute("authenticated.username")).thenReturn(expectedUsername);

        // When
        final String result = this.authService.getUsername();

        // Then
        assertThat(result).isEqualTo(expectedUsername);
    }

    @Test
    void getUsername_shouldReturnNull_whenStoredUsernameTypeIsUnexpected() {
        // Given
        when(this.vaadinSession.getAttribute("authenticated.username")).thenReturn(Boolean.TRUE);

        // When
        final String result = this.authService.getUsername();

        // Then
        assertThat(result).isNull();
        verify(this.vaadinSession).setAttribute("authenticated.username", null);
    }
}
