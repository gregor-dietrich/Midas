package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * Utility class for common exception testing patterns
 */
public class ExceptionTestUtil {

    private ExceptionTestUtil() {
        // Utility class
    }

    /**
     * Assert that an exception is thrown with "User is not authenticated" message
     */
    public static void assertThrowsUserNotAuthenticated(final Runnable methodCall) {
        assertThatThrownBy(methodCall::run)
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authentication required");
    }

    /**
     * Assert that method throws AuthenticationException with "Authentication
     * required"
     */
    public static void assertThrowsAuthenticationException(final Runnable methodCall, final String expectedMessage) {
        assertThatThrownBy(methodCall::run)
                .isInstanceOf(AuthenticationException.class)
                .hasMessage(expectedMessage);
    }

    /**
     * Test that a method throws ServiceException with specific message
     */
    public static void assertThrowsServiceException(final Runnable methodCall, final String expectedMessage) {
        assertThatThrownBy(methodCall::run)
                .isInstanceOf(ServiceException.class)
                .hasMessage(expectedMessage);
    }

    /**
     * Test that a method throws AuthenticationException when auth is required
     */
    public static void assertThrowsAuthenticationRequired(final Runnable methodCall) {
        assertThrowsAuthenticationException(methodCall, "Authentication required");
    }

    /**
     * Test that a method throws ServiceException on backend connection failure
     */
    public static void assertThrowsBackendConnectionFailed(final Runnable methodCall) {
        assertThrowsServiceException(methodCall, "Backend connection failed");
    }

    /**
     * Test that a method throws AuthenticationException on session expired
     */
    public static void assertThrowsSessionExpired(final Runnable methodCall) {
        assertThrowsAuthenticationException(methodCall, "Session expired");
    }

    /**
     * Test that a method throws ServiceException on unexpected error
     */
    public static void assertThrowsUnexpectedError(final Runnable methodCall) {
        assertThrowsServiceException(methodCall, "Unexpected error");
    }

    /**
     * Test that a method throws ServiceException on unexpected error occurred
     */
    public static void assertThrowsUnexpectedErrorOccurred(final Runnable methodCall) {
        assertThrowsServiceException(methodCall, "Unexpected error occurred");
    }

    /**
     * Test 401 error handling with logout verification
     */
    public static void assertHandles401WithLogout(final Runnable methodCall, final AuthService authService) {
        assertThrowsSessionExpired(methodCall);
        verify(authService).logout();
    }

    /**
     * Create a ProcessingException for testing connection failures
     */
    public static ProcessingException createProcessingException() {
        return createProcessingException("Connection failed");
    }

    /**
     * Create a ProcessingException with custom message
     */
    public static ProcessingException createProcessingException(final String message) {
        return new ProcessingException(message);
    }

    /**
     * Create a WebApplicationException for 401 testing
     */
    public static WebApplicationException create401Exception() {
        return new WebApplicationException(ResponseTestUtil.createUnauthorizedResponse());
    }

    /**
     * Create a WebApplicationException with specific status
     */
    public static WebApplicationException createWebApplicationException(final int status) {
        return new WebApplicationException(ResponseTestUtil.createMockResponse(status));
    }

    public static void assertThrowsBackendError(final Runnable executable, final int expectedStatus) {
        assertThatThrownBy(executable::run)
                .isInstanceOf(ServiceException.class)
                .hasMessage("Backend error: " + expectedStatus);
    }
}
