package de.vptr.midas.gui.util;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.service.AuthService;

/**
 * Base class for service tests providing common mock setup and utilities
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    @Mock
    protected AuthService authService;

    /**
     * Setup authentication header with default test header
     */
    protected void setupAuthHeader() {
        ServiceTestUtil.setupAuthHeader(this.authService);
    }

    /**
     * Setup authentication header with custom header
     */
    protected void setupAuthHeader(final String authHeader) {
        ServiceTestUtil.setupAuthHeader(this.authService, authHeader);
    }

    /**
     * Test that a method throws AuthenticationException when auth header is null
     */
    protected void testAuthenticationRequired(final Runnable methodCall) {
        ExceptionTestUtil.assertThrowsAuthenticationRequired(methodCall);
    }

    /**
     * Test that a method throws ServiceException when ProcessingException occurs
     */
    protected void testProcessingExceptionHandling(final Runnable clientCallSetup, final Runnable methodCall) {
        clientCallSetup.run();
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(methodCall);
    }

    /**
     * Test that a method throws AuthenticationException on 401 response and calls
     * logout
     */
    protected void test401ErrorHandling(final Runnable clientCallSetup, final Runnable methodCall) {
        clientCallSetup.run();
        ExceptionTestUtil.assertHandles401WithLogout(methodCall, this.authService);
    }

    /**
     * Test that a method returns empty on 404 response
     */
    protected void testReturnEmptyOn404(final Runnable clientCallSetup, final Supplier<Optional<?>> methodCall) {
        ServiceTestUtil.testReturnEmptyOn404(clientCallSetup, methodCall);
    }

    /**
     * Test that a method throws ServiceException on bad response
     */
    protected void testServiceExceptionOnBadResponse(final Runnable clientCallSetup, final Runnable methodCall) {
        ServiceTestUtil.testServiceExceptionOnBadResponse(clientCallSetup, methodCall);
    }
}
