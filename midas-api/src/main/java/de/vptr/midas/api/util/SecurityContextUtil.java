package de.vptr.midas.api.util;

import java.security.Principal;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** Shared security-context helpers for authenticated resource methods. */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    /**
     * Returns the current username or fails closed when the auth context is incomplete.
     */
    public static String requireCurrentUsername(@Nullable final SecurityContext securityContext) {
        if (securityContext == null) {
            throw missingAuthenticatedUser();
        }
        final Principal principal = securityContext.getUserPrincipal();
        if (principal == null) {
            throw missingAuthenticatedUser();
        }
        final String username = principal.getName();
        if (username == null || username.isBlank()) {
            throw missingAuthenticatedUser();
        }
        return username;
    }

    private static WebApplicationException missingAuthenticatedUser() {
        return new WebApplicationException("Authenticated user missing", Response.Status.FORBIDDEN);
    }
}
