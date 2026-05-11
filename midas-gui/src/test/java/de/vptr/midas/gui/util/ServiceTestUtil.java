package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.function.Supplier;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Utility class for common service testing patterns in GUI tests
 */
public class ServiceTestUtil {

    private ServiceTestUtil() {
        // Utility class
    }

    /**
     * Test that a 401 exception is handled with logout
     */
    public static void testHandles401WithLogout(final AuthService authService, final Object client,
            final Runnable serviceMethodCall, final Runnable clientMethodCall) {
        // Given
        setupAuthHeader(authService);
        final WebApplicationException webAppException = new WebApplicationException(401);
        @SuppressWarnings("CheckReturnValue")
        final var _ = doThrow(webAppException).when(client);
        clientMethodCall.run();

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(serviceMethodCall, authService);
    }

    /**
     * Test that a method throws AuthenticationException when auth header is null
     */
    public static void testAuthenticationRequired(final Runnable methodCall, final String expectedMessage) {
        assertThatThrownBy(methodCall::run).isInstanceOf(AuthenticationException.class).hasMessage(expectedMessage);
    }

    /**
     * Test that a method throws AuthenticationException when auth header is null with default message
     */
    public static void testAuthenticationRequired(final Runnable methodCall) {
        testAuthenticationRequired(methodCall, "Authentication required");
    }

    /**
     * Test that a method throws ServiceException when ProcessingException occurs
     */
    public static void testProcessingExceptionHandling(final Runnable clientCallSetup, final Runnable methodCall) {
        // Setup the client to throw ProcessingException
        clientCallSetup.run();

        assertThatThrownBy(methodCall::run).isInstanceOf(ServiceException.class)
                .hasMessage("Backend connection failed");
    }

    /**
     * Test that a method throws AuthenticationException on 401 response and calls logout
     */
    public static void test401ErrorHandling(final Runnable clientCallSetup, final Runnable methodCall,
            final AuthService authService) {
        // Setup the client to throw WebApplicationException with 401
        clientCallSetup.run();

        assertThatThrownBy(methodCall::run).isInstanceOf(AuthenticationException.class).hasMessage("Session expired");

        verify(authService).logout();
    }

    /**
     * Test that a method returns empty Optional on 404 response
     */
    public static void testReturnEmptyOn404(final Runnable clientCallSetup, final Supplier<Optional<?>> methodCall) {
        clientCallSetup.run();

        final var result = methodCall.get();

        assertThat(result).isEmpty();
    }

    /**
     * Setup mock response for testing
     */
    public static Response createMockResponse(final int status) {
        return Response.status(status).build();
    }

    /**
     * Setup mock response with entity for testing
     */
    public static Response createMockResponse(final int status, final Object entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Setup WebApplicationException for 401 testing
     */
    public static WebApplicationException create401Exception() {
        return new WebApplicationException(Response.status(401).build());
    }

    /**
     * Setup ProcessingException for connection failure testing
     */
    public static ProcessingException createConnectionException() {
        return new ProcessingException("Connection failed");
    }

    /**
     * Common pattern for testing service method that throws ServiceException on bad response
     */
    public static void testServiceExceptionOnBadResponse(final Runnable clientCallSetup, final Runnable methodCall) {
        clientCallSetup.run();

        assertThatThrownBy(methodCall::run).isInstanceOf(ServiceException.class).hasMessage("Unexpected error");
    }

    /**
     * Common pattern for testing successful service calls with verification
     */
    public static <T> void testSuccessfulServiceCall(final T expectedResult, final Supplier<T> methodCall,
            final Runnable verificationCall) {
        final T result = methodCall.get();

        assertThat(result).isEqualTo(expectedResult);
        verificationCall.run();
    }

    /**
     * Setup authentication header in AuthService mock
     */
    public static void setupAuthHeader(final AuthService authService, final String authHeader) {
        lenient().when(authService.getAuthorizationHeader()).thenReturn(authHeader);
        lenient().when(authService.requireAuthorizationHeader()).thenReturn(authHeader);
    }

    /**
     * Setup authentication header with default test header
     */
    public static void setupAuthHeader(final AuthService authService) {
        setupAuthHeader(authService, TestUtil.TEST_AUTH_HEADER);
    }
}
