package de.vptr.midas.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.SecurityContext;

// NOTE: This is an intentional plain JUnit test, not a @QuarkusTest.
// It tests field-level validation logic via direct instantiation without requiring the full CDI container.
class UserResourceProtectedFieldTest {

    @Test
    void updateUser_shouldKeepProtectedFields_whenRolesMissing() {
        final var resource = new UserResource();
        final var stubUserService = new StubUserService(existingUser("rank-2", false, true));
        resource.setUserServiceForTest(stubUserService);
        resource.securityContext = new TestSecurityContext("editor", Set.of("user:edit"));

        final var dto = new UserDto();
        dto.username = "updated-user";
        dto.rankPublicId = "rank-99";
        dto.banned = true;
        dto.activated = false;

        resource.updateUser("user-1", dto);

        final UserDto updatedUser = Objects.requireNonNull(stubUserService.lastUpdatedUser);
        assertEquals("rank-2", updatedUser.rankPublicId);
        assertEquals(false, updatedUser.banned);
        assertEquals(true, updatedUser.activated);
    }

    @Test
    void patchUser_shouldKeepProtectedFields_whenRolesMissing() {
        final var resource = new UserResource();
        final var stubUserService = new StubUserService(existingUser("rank-3", true, false));
        resource.setUserServiceForTest(stubUserService);
        resource.securityContext = new TestSecurityContext("editor", Set.of("user:edit"));

        final var dto = new UserDto();
        dto.rankPublicId = "rank-100";
        dto.banned = false;
        dto.activated = true;

        resource.patchUser("user-1", dto);

        final UserDto patchedUser = Objects.requireNonNull(stubUserService.lastPatchedUser);
        assertEquals("rank-3", patchedUser.rankPublicId);
        assertEquals(true, patchedUser.banned);
        assertEquals(false, patchedUser.activated);
    }

    private static UserResponseDto existingUser(final String rankPublicId, final Boolean banned,
            final Boolean activated) {
        final var existing = new UserResponseDto();
        existing.rankPublicId = rankPublicId;
        existing.banned = banned;
        existing.activated = activated;
        return existing;
    }

    private static final class StubUserService extends UserService {
        private final UserResponseDto existingUser;
        @Nullable
        private UserDto lastUpdatedUser;
        @Nullable
        private UserDto lastPatchedUser;

        private StubUserService(final UserResponseDto existingUser) {
            this.existingUser = existingUser;
        }

        @Override
        public Optional<UserResponseDto> findById(final String publicId) {
            return Optional.of(this.existingUser);
        }

        @Override
        public UserResponseDto updateUser(final String publicId, final UserDto userDto) {
            this.lastUpdatedUser = userDto;
            return this.existingUser;
        }

        @Override
        public UserResponseDto patchUser(final String publicId, final UserDto userDto) {
            this.lastPatchedUser = userDto;
            return this.existingUser;
        }
    }

    private static final class TestSecurityContext implements SecurityContext {
        private final Principal principal;
        private final Set<String> roles;

        private TestSecurityContext(final String username, final Set<String> roles) {
            this.principal = () -> username;
            this.roles = roles;
        }

        @Override
        public Principal getUserPrincipal() {
            return this.principal;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return this.roles.contains(role);
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
