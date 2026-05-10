package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserIdentityProviderTest {

    @Inject
    UserIdentityProvider userIdentityProvider;

    @Test
    void testGetRequestType() {
        assertEquals(UsernamePasswordAuthenticationRequest.class, this.userIdentityProvider.getRequestType());
    }

    @Test
    void testAuthenticateSuccess() {
        // given
        final var username = "admin";
        final var password = "admin";

        // when
        final var identity = this.userIdentityProvider.authenticateUser(username, password);

        // then
        assertFalse(identity.isAnonymous());
        assertEquals("admin", identity.getPrincipal().getName());
    }

    @Test
    void testAuthenticateUserNotFound() {
        // given
        final var username = "nonexistent";
        final var password = "password";

        // when / then
        final var exception =
                assertThrows(Exception.class, () -> this.userIdentityProvider.authenticateUser(username, password));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // given
        final var username = "admin";
        final var password = "wrongpassword";

        // when / then
        final var exception =
                assertThrows(Exception.class, () -> this.userIdentityProvider.authenticateUser(username, password));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testAuthenticateBannedUser() {
        // given
        final var username = "bannedUser";
        final var password = "bannedUser";

        // when / then
        final var exception =
                assertThrows(Exception.class, () -> this.userIdentityProvider.authenticateUser(username, password));
        assertEquals("User is banned", exception.getMessage());
    }

    @Test
    void testAuthenticateNotActivatedUser() {
        // given
        final var username = "notActivatedUser";
        final var password = "notActivatedUser";

        // when / then
        final var exception =
                assertThrows(Exception.class, () -> this.userIdentityProvider.authenticateUser(username, password));
        assertEquals("User is not activated", exception.getMessage());
    }
}
