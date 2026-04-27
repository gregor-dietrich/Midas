package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserDto;
import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUserUpdateDto;
import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ServiceTestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class UserServiceTest {
    @Inject
    UserService userService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.userService);
    }

    @Test
    void testGetAllUsers() {
        final var users = this.userService.getAllUsers();
        assertNotNull(users);
    }

    @Test
    @TestTransaction
    void testCreateUser() {
        final var newUser = createUniqueUserDto("newTestUser", "newtest");
        final var createdUser = this.userService.createUser(newUser);

        assertNotNull(createdUser);
        assertNotNull(createdUser.id);
        assertEquals(newUser.username, createdUser.username);
        assertEquals(newUser.email, createdUser.email);
        // Password is not returned in response DTO
        assertNotNull(createdUser.created);
        assertNotNull(createdUser.lastLogin);
        assertFalse(createdUser.activated); // Default should be false
        assertFalse(createdUser.banned); // Default should be false
    }

    @Test
    @TestTransaction
    void testCreateUserWithExistingUsername() {
        // First create a user
        final var firstUser = createUniqueUserDto("duplicateUser", "first");
        this.userService.createUser(firstUser);

        // Try to create another user with the same username
        final var secondUser = createUniqueUserDto();
        secondUser.username = firstUser.username; // Use same username
        secondUser.email = createUniqueUserDto("second", "second").email; // Different email

        final ValidationException exception = assertThrows(ValidationException.class, () -> {
            this.userService.createUser(secondUser);
        });

        // Verify that the exception message contains details about the duplicate
        // username
        assertTrue(exception.getMessage().contains("Username"));
        assertTrue(exception.getMessage().contains("already taken"));
    }

    @Test
    @TestTransaction
    void testUpdateUser() {
        // First create a user
        final var newUser = createUniqueUserDto("updateTestUser", "update");
        final var createdUser = this.userService.createUser(newUser);

        // Update the user
        final var updateDto = createUserUpdateDto("testUser", "updated");

        final var updatedUser = this.userService.updateUser(createdUser.id, updateDto);

        assertNotNull(updatedUser);
        assertEquals(updateDto.email, updatedUser.email);
        assertTrue(updatedUser.activated);
    }

    @Test
    @TestTransaction
    void testDeleteUser() {
        // First create a user
        final var newUser = createUniqueUserDto("deleteTestUser", "delete");
        final var createdUser = this.userService.createUser(newUser);

        final Long userId = createdUser.id;

        final var deleted = this.userService.deleteUser(userId);

        assertTrue(deleted);
        // UserEntity.findById returns null if not found
        final var deletedUser = UserEntity.findById(userId);
        assertNull(deletedUser);
    }

    @Test
    void testDeleteNonExistentUser() {
        final var deleted = this.userService.deleteUser(999999L);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindByUsername() {
        // First create a user
        final var newUser = createUniqueUserDto("findByUsernameUser", "findbyusername");
        this.userService.createUser(newUser);

        final var foundUser = this.userService.findByUsername(newUser.username);

        assertTrue(foundUser.isPresent());
        assertEquals(newUser.username, foundUser.get().username);
    }

    @Test
    void testFindByUsernameNonExistent() {
        final var foundUser = this.userService.findByUsername("nonExistentUser");
        assertTrue(foundUser.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByEmail() {
        // First create a user
        final var newUser = createUniqueUserDto("findByEmailUser", "findbyemail");
        this.userService.createUser(newUser);

        final var foundUser = this.userService.findByEmail(newUser.email);

        assertTrue(foundUser.isPresent());
        assertEquals(newUser.email, foundUser.get().email);
    }

    @Test
    void testFindByEmailNonExistent() {
        final var foundUser = this.userService.findByEmail("nonexistent@example.com");
        assertTrue(foundUser.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindActiveUsers() {
        // Create active and inactive users
        final var activeUser = createUniqueUserDto("activeUser", "active");
        activeUser.activated = true;
        activeUser.banned = false;
        this.userService.createUser(activeUser);

        final var bannedUser = createUniqueUserDto("bannedUser", "banned");
        bannedUser.activated = true;
        bannedUser.banned = true;
        this.userService.createUser(bannedUser);

        // Flush using Panache to ensure users are persisted and visible to queries
        UserEntity.getEntityManager().flush();

        final var activeUsers = this.userService.findActiveUsers();
        assertNotNull(activeUsers);
        assertTrue(activeUsers.stream().anyMatch(u -> activeUser.username.equals(u.username)));
        assertFalse(activeUsers.stream().anyMatch(u -> bannedUser.username.equals(u.username)));
    }

    @Test
    @TestTransaction
    void testCreateUserWithExistingEmail() {
        // First create a user
        final var firstUser = createUniqueUserDto("duplicateEmail", "first");
        this.userService.createUser(firstUser);

        // Try to create another user with the same email
        final var secondUser = createUniqueUserDto("second", "second");
        secondUser.email = firstUser.email; // Use same email
        secondUser.username = createUniqueUserDto("secondUser", "secondUser").username; // Different username

        final ValidationException exception = assertThrows(ValidationException.class, () -> {
            this.userService.createUser(secondUser);
        });

        // Verify that the exception message contains details about the duplicate email
        assertTrue(exception.getMessage().contains("Email"));
        assertTrue(exception.getMessage().contains("already in use"));
    }

    @Test
    void testSearchUsersWithNull() {
        final var expected = this.userService.getAllUsers();

        final var results = this.userService.searchUsers(null);

        assertEquals(expected.stream().map(user -> user.id).toList(),
                results.stream().map(user -> user.id).toList());
    }

    @Test
    void testSearchUsersWithBlankQueryUsesDefaultListing() {
        final var expected = this.userService.getAllUsers();

        final var results = this.userService.searchUsers("   ");

        assertEquals(expected.stream().map(user -> user.id).toList(),
                results.stream().map(user -> user.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchUsersWithQuery() {
        final var dto = createUniqueUserDto("searchTestUser", "searchtest");
        this.userService.createUser(dto);

        final var results = this.userService.searchUsers(dto.username);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchUsersEscapesLikeWildcards() {
        final String token = "user-search-" + generateUniqueTestSuffix();
        final var literalMatch = createUniqueUserDto("literalUser", "literal");
        literalMatch.username = token + "_literal";
        literalMatch.email = token + "_literal@example.com";

        final var wildcardOnlyMatch = createUniqueUserDto("wildcardUser", "wildcard");
        wildcardOnlyMatch.username = token + "xliteral";
        wildcardOnlyMatch.email = token + "xliteral@example.com";

        final var createdLiteralMatch = this.userService.createUser(literalMatch);
        this.userService.createUser(wildcardOnlyMatch);

        final var results = this.userService.searchUsers(token + "_");

        assertEquals(List.of(createdLiteralMatch.id), results.stream().map(user -> user.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchUsersAppliesSharedResultLimit() {
        final String token = "user-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var user = createUniqueUserDto("limitUser", "limit");
            user.username = token + "-" + i;
            user.email = token + "-" + i + "@example.com";
            this.userService.createUser(user);
        }

        final var results = this.userService.searchUsers(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    @TestTransaction
    void testPatchUserUsername() {
        final var dto = createUniqueUserDto("patchUser", "patch");
        final var created = this.userService.createUser(dto);

        final var patchDto = new UserDto();
        patchDto.username = "patchedUsername_" + ServiceTestUtil.generateUniqueTestSuffix();

        final var patched = this.userService.patchUser(created.id, patchDto);
        assertNotNull(patched);
        assertEquals(patchDto.username, patched.username);
    }

    @Test
    @TestTransaction
    void testPatchUserEmail() {
        final var dto = createUniqueUserDto("patchEmailUser", "patchemail");
        final var created = this.userService.createUser(dto);

        final var patchDto = new UserDto();
        patchDto.email = "patched_" + ServiceTestUtil.generateUniqueTestSuffix()
                + "@example.com";

        final var patched = this.userService.patchUser(created.id, patchDto);
        assertNotNull(patched);
        assertEquals(patchDto.email, patched.email);
    }

    @Test
    @TestTransaction
    void testPatchUserBannedFlag() {
        final var dto = createUniqueUserDto("patchBannedUser", "patchbanned");
        final var created = this.userService.createUser(dto);
        assertFalse(created.banned);

        final var patchDto = new UserDto();
        patchDto.banned = true;

        final var patched = this.userService.patchUser(created.id, patchDto);
        assertTrue(patched.banned);
    }

    @Test
    @TestTransaction
    void testPatchUserActivatedFlag() {
        final var dto = createUniqueUserDto("patchActivatedUser", "patchactivated");
        final var created = this.userService.createUser(dto);
        assertFalse(created.activated);

        final var patchDto = new UserDto();
        patchDto.activated = true;

        final var patched = this.userService.patchUser(created.id, patchDto);
        assertTrue(patched.activated);
    }

    @Test
    void testPatchUserNotFound() {
        final var patchDto = new UserDto();
        patchDto.username = "test";
        assertThrows(WebApplicationException.class, () -> this.userService.patchUser(999999L, patchDto));
    }

    @Test
    @TestTransaction
    void testPatchUserDuplicateUsername() {
        final var user1 = createUniqueUserDto("patchDup1", "patchdup1");
        this.userService.createUser(user1);

        final var user2 = createUniqueUserDto("patchDup2", "patchdup2");
        final var created2 = this.userService.createUser(user2);

        final var patchDto = new UserDto();
        patchDto.username = user1.username;

        assertThrows(ValidationException.class, () -> this.userService.patchUser(created2.id, patchDto));
    }

    @Test
    @TestTransaction
    void testPatchUserPassword() {
        final var dto = createUniqueUserDto("patchPwUser", "patchpw");
        final var created = this.userService.createUser(dto);

        final var patchDto = new UserDto();
        patchDto.password = "newPassword123";

        final var patched = this.userService.patchUser(created.id, patchDto);
        assertNotNull(patched);
    }

    @Test
    @TestTransaction
    void testUpdateUserWithPassword() {
        final var dto = createUniqueUserDto("updatePwUser", "updatepw");
        final var created = this.userService.createUser(dto);

        final var updateDto = createUserUpdateDto("updatedPw", "updatedpw");
        updateDto.password = "newPassword456";

        final var updated = this.userService.updateUser(created.id, updateDto);
        assertNotNull(updated);
    }

    @Test
    void testUpdateUserNotFound() {
        final var updateDto = createUserUpdateDto("noUser", "nouser");
        assertThrows(WebApplicationException.class, () -> this.userService.updateUser(999999L, updateDto));
    }

    @Test
    @TestTransaction
    void testCreateUserWithNullEmail() {
        final var dto = createUniqueUserDto("nullEmailUser", "nullemail");
        dto.email = null;
        final var created = this.userService.createUser(dto);
        assertNotNull(created);
        assertNull(created.email);
    }

    @Test
    @TestTransaction
    void testCreateUserWithEmptyEmail() {
        final var dto = createUniqueUserDto("emptyEmailUser", "emptyemail");
        dto.email = "";
        final var created = this.userService.createUser(dto);
        assertNotNull(created);
        assertNull(created.email);
    }

    @Test
    @TestTransaction
    void testFindById() {
        final var dto = createUniqueUserDto("findByIdUser", "findbyid");
        final var created = this.userService.createUser(dto);

        final var found = this.userService.findById(created.id);
        assertTrue(found.isPresent());
        assertEquals(created.username, found.get().username);
    }

    @Test
    void testFindByIdNonExistent() {
        final var found = this.userService.findById(999999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void testGetAllUsersWithPagination() {
        final var users = this.userService.getAllUsers(0, 5);
        assertNotNull(users);
    }
}
