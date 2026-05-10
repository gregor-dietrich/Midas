package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Principal;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.SecurityContext;

class SecurityContextUtilTest {

    @Test
    void requireCurrentUsernameReturnsUsernameWhenPrincipalPresent() {
        final String username = SecurityContextUtil.requireCurrentUsername(new TestSecurityContext(() -> "admin"));
        assertEquals("admin", username);
    }

    @Test
    void requireCurrentUsernameRejectsMissingSecurityContext() {
        final WebApplicationException exception =
                assertThrows(WebApplicationException.class, () -> SecurityContextUtil.requireCurrentUsername(null));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    void requireCurrentUsernameRejectsMissingPrincipal() {
        final WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> SecurityContextUtil.requireCurrentUsername(new TestSecurityContext(null)));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    void requireCurrentUsernameRejectsPrincipalWithoutName() {
        final WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> SecurityContextUtil.requireCurrentUsername(new TestSecurityContext(() -> null)));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    void requireCurrentUsernameRejectsBlankUsername() {
        final WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> SecurityContextUtil.requireCurrentUsername(new TestSecurityContext(() -> "  ")));
        assertEquals(403, exception.getResponse().getStatus());
    }

    private static final class TestSecurityContext implements SecurityContext {
        private final Principal principal;

        private TestSecurityContext(final Principal principal) {
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return this.principal;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return "BASIC_AUTH";
        }
    }
}
