package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.UserRankResponseDto;
import de.vptr.midas.api.util.AbstractIntegrationTest;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import de.vptr.midas.api.util.ServiceTestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserRankServiceTest extends AbstractIntegrationTest {
    @Inject
    UserRankService userRankService;

    @Inject
    UserService userService;

    @BeforeEach
    void setUp() {
        runInIsolatedTransaction(() -> setupTestUser(this.userService));
    }

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.userRankService);
    }

    @Test
    void testGetAllRanks() {
        final var ranks = this.userRankService.getAllRanks();
        assertNotNull(ranks);
    }

    @Test
    @TestTransaction
    void testCreateRank() {
        // args: name, userAdd, userEdit, userDelete, postAdd, postEdit, postDelete
        final var newRank = ServiceTestDataBuilder.createUserRankDto(ServiceTestUtil.createUniqueTestName("Test Rank"),
                true, true, false, true, false, false);
        // Override some permissions for testing
        newRank.userDelete = false;
        newRank.postEdit = false;
        newRank.postDelete = false;

        final var createdRank = this.userRankService.createRank(newRank);

        assertNotNull(createdRank);
        assertNotNull(createdRank.id);
        assertEquals(newRank.name, createdRank.name);
        assertTrue(Boolean.TRUE.equals(createdRank.userAdd));
        assertTrue(Boolean.TRUE.equals(createdRank.userEdit));
        assertFalse(Boolean.TRUE.equals(createdRank.userDelete));
        assertTrue(Boolean.TRUE.equals(createdRank.postAdd));
        assertFalse(Boolean.TRUE.equals(createdRank.postEdit));
        assertFalse(Boolean.TRUE.equals(createdRank.postDelete));
    }

    @Test
    @TestTransaction
    void testCreateRankWithExistingName() {
        // First create a rank
        final var firstRank = ServiceTestDataBuilder.createUserRankDto(
                ServiceTestUtil.createUniqueTestName("Test Rank"), false, false, false, true, true, true);
        firstRank.userAdd = false;
        firstRank.userEdit = false;
        firstRank.userDelete = false;
        final var createdRank = this.userRankService.createRank(firstRank);

        // Try to create another rank with the same name
        final var secondRank = ServiceTestDataBuilder.createUserRankDto(Objects.requireNonNull(createdRank.name), true,
                true, true, false, false, false);

        assertThrows(Exception.class, () -> {
            this.userRankService.createRank(secondRank);
        });
    }

    @Test
    @TestTransaction
    void testUpdateRank() {
        // First create a rank
        final var newRank =
                ServiceTestDataBuilder.createUserRankDto("Update Test Rank", false, false, false, false, false, false);
        final var createdRank = this.userRankService.createRank(newRank);

        // Update the rank
        final var updateDto = ServiceTestDataBuilder.createUserRankUpdateDto("Updated Rank Name", true, // userAdd
                true, // userEdit
                false, // userDelete
                true, // postAdd
                false, // postEdit
                false // postDelete
        );

        final var updatedRank = this.userRankService.updateRank(Objects.requireNonNull(createdRank.id), updateDto);

        assertNotNull(updatedRank);
        assertEquals("Updated Rank Name", updatedRank.name);
        assertTrue(Boolean.TRUE.equals(updatedRank.userAdd));
        assertTrue(Boolean.TRUE.equals(updatedRank.userEdit));
        assertTrue(Boolean.TRUE.equals(updatedRank.postAdd));
        assertFalse(Boolean.TRUE.equals(updatedRank.userDelete));
        assertFalse(Boolean.TRUE.equals(updatedRank.postEdit));
        assertFalse(Boolean.TRUE.equals(updatedRank.postDelete));
    }

    @Test
    @TestTransaction
    void testDeleteRank() {
        // First create a rank
        final var newRank =
                ServiceTestDataBuilder.createUserRankDto("Delete Test Rank", false, false, false, false, false, false);
        final var createdRank = this.userRankService.createRank(newRank);

        final Long rankId = Objects.requireNonNull(createdRank.id);

        final var deleted = this.userRankService.deleteRank(rankId);

        assertTrue(deleted);
        final var deletedRank = this.userRankService.findById(rankId);
        assertTrue(deletedRank.isEmpty());
    }

    @Test
    void testDeleteNonExistentRank() {
        final var deleted = this.userRankService.deleteRank(999999L);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create a rank
        final var newRank =
                ServiceTestDataBuilder.createUserRankDto("Find By ID Rank", true, false, false, false, false, false);
        final var createdRank = this.userRankService.createRank(newRank);

        final var foundRank = this.userRankService.findById(Objects.requireNonNull(createdRank.id));

        assertTrue(foundRank.isPresent());
        assertEquals("Find By ID Rank", foundRank.get().name);
        assertTrue(Boolean.TRUE.equals(foundRank.get().userAdd));
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundRank = this.userRankService.findById(999999L);
        assertTrue(foundRank.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByName() {
        // First create a rank
        final var newRank =
                ServiceTestDataBuilder.createUserRankDto("Find By Name Rank", false, true, false, false, false, false);
        this.userRankService.createRank(newRank);

        final var foundRank = this.userRankService.findByName("Find By Name Rank");

        assertTrue(foundRank.isPresent());
        assertEquals("Find By Name Rank", foundRank.get().name);
        assertFalse(Boolean.TRUE.equals(foundRank.get().userAdd));
        assertTrue(Boolean.TRUE.equals(foundRank.get().userEdit));
    }

    @Test
    void testFindByNameNonExistent() {
        final var foundRank = this.userRankService.findByName("Non Existent Rank");
        assertTrue(foundRank.isEmpty());
    }

    @Test
    void testSearchRanksWithNullOrBlankQueryUsesDefaultListing() {
        final var expected = this.userRankService.getAllRanks();

        final var nullResults = this.userRankService.searchRanks(null);
        final var blankResults = this.userRankService.searchRanks("   ");

        assertEquals(expected.stream().map(rank -> rank.id).toList(),
                nullResults.stream().map(rank -> rank.id).toList());
        assertEquals(expected.stream().map(rank -> rank.id).toList(),
                blankResults.stream().map(rank -> rank.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchRanksAppliesSharedResultLimit() {
        final String token = "rank-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var rank =
                    ServiceTestDataBuilder.createUserRankDto(token + "-" + i, false, false, false, false, false, false);
            this.userRankService.createRank(rank);
        }

        final var results = this.userRankService.searchRanks(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    @TestTransaction
    void testGetRanksWithUserPermissions() {
        // Create ranks with different user permissions
        final var userRank =
                ServiceTestDataBuilder.createUserRankDto("User Permission Rank", true, true, true, false, false, false);
        this.userRankService.createRank(userRank);

        final var noUserRank = ServiceTestDataBuilder.createUserRankDto("No User Permission Rank", false, false, false,
                true, true, true);
        this.userRankService.createRank(noUserRank);

        final var userPermissionRanks =
                this.userRankService.getAllRanks().stream().filter(rank -> Boolean.TRUE.equals(rank.userAdd)
                        || Boolean.TRUE.equals(rank.userEdit) || Boolean.TRUE.equals(rank.userDelete)).toList();

        assertNotNull(userPermissionRanks);
        // Verify all returned ranks have at least one user permission
        for (final UserRankResponseDto rank : userPermissionRanks) {
            assertTrue(Boolean.TRUE.equals(rank.userAdd) || Boolean.TRUE.equals(rank.userEdit)
                    || Boolean.TRUE.equals(rank.userDelete));
        }
    }

    @Test
    @TestTransaction
    void testGetRanksWithPostPermissions() {
        // Create ranks with different post permissions
        final var postRank =
                ServiceTestDataBuilder.createUserRankDto("Post Permission Rank", false, false, false, true, true, true);
        this.userRankService.createRank(postRank);

        final var noPostRank = ServiceTestDataBuilder.createUserRankDto("No Post Permission Rank", true, true, true,
                false, false, false);
        this.userRankService.createRank(noPostRank);

        final var postPermissionRanks =
                this.userRankService.getAllRanks().stream().filter(rank -> Boolean.TRUE.equals(rank.postAdd)
                        || Boolean.TRUE.equals(rank.postEdit) || Boolean.TRUE.equals(rank.postDelete)).toList();

        assertNotNull(postPermissionRanks);
        // Verify all returned ranks have at least one post permission
        for (final UserRankResponseDto rank : postPermissionRanks) {
            assertTrue(Boolean.TRUE.equals(rank.postAdd) || Boolean.TRUE.equals(rank.postEdit)
                    || Boolean.TRUE.equals(rank.postDelete));
        }
    }
}
