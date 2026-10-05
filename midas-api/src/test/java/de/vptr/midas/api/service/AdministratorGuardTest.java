package de.vptr.midas.api.service;

import static de.vptr.midas.api.service.AdministratorGuard.LAST_ADMINISTRATOR_MESSAGE;
import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.security.UserRankIdentityAugmentor;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

/**
 * Tests the last-administrator guard through every service path it covers. Most tests run in a rolled-back transaction
 * that first leaves the demo {@code admin} as the only active administrator; the concurrency test commits its setup and
 * restores it.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
class AdministratorGuardTest {

    private static final String ADMIN_ID = "01AN4Z07BY79KA1307SR9X4MV7";
    private static final String USER_ID = "01AN4Z07BY79KA1307SR9X4MV8";
    private static final String ADMINISTRATOR_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV3";
    private static final String USER_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV4";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    /** Deactivates every user but the demo admin, which leaves it the only active administrator. */
    private static void leaveAdminAsLastAdministrator() {
        UserEntity.update("activated = false WHERE publicId <> ?1", ADMIN_ID);
    }

    private static void assertRefused(final Executable change) {
        assertEquals(LAST_ADMINISTRATOR_MESSAGE, assertThrows(ValidationException.class, change).getMessage());
    }

    private static UserDto userPatch(final Boolean banned, final Boolean activated, final String rankPublicId) {
        return new UserDto(null, null, null, rankPublicId, banned, activated, null);
    }

    /** A rank that grants every permission, as the Administrator rank does. */
    private static UserRankDto fullRank(final String name) throws ReflectiveOperationException {
        final var rank = new UserRankDto(name);
        for (final String flag : AdministratorGuard.PERMISSIONS) {
            UserRankDto.class.getField(flag).set(rank, true);
        }
        return rank;
    }

    private UserResponseDto createActiveAdministrator(final String rankPublicId) {
        final var dto = createUniqueUserDto("secondAdmin", "secondadmin");
        dto.rankPublicId = rankPublicId;
        dto.activated = true;
        return this.userService.createUser(dto);
    }

    @Test
    @TestTransaction
    void testDeleteLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        assertRefused(() -> this.userService.deleteUser(ADMIN_ID));
    }

    @Test
    @TestTransaction
    void testBanLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        assertRefused(() -> this.userService.patchUser(ADMIN_ID, userPatch(true, null, null)));
    }

    @Test
    @TestTransaction
    void testDeactivateLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        final var update = new UserDto("admin", null, null, ADMINISTRATOR_RANK_ID, false, false, null);
        assertRefused(() -> this.userService.updateUser(ADMIN_ID, update));
    }

    @Test
    @TestTransaction
    void testMoveLastAdministratorToRankWithoutAdministrationIsRefused() {
        leaveAdminAsLastAdministrator();
        assertRefused(() -> this.userService.patchUser(ADMIN_ID, userPatch(null, null, USER_RANK_ID)));
    }

    @Test
    @TestTransaction
    void testPatchRankOfLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        final var patch = new UserRankDto();
        patch.adminView = false;
        assertRefused(() -> this.userRankService.patchRank(ADMINISTRATOR_RANK_ID, patch));
    }

    @Test
    @TestTransaction
    void testStripUserEditFromRankOfLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        final var patch = new UserRankDto();
        patch.userEdit = false;
        assertRefused(() -> this.userRankService.patchRank(ADMINISTRATOR_RANK_ID, patch));
    }

    @Test
    @TestTransaction
    void testUpdateRankOfLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        final var update = new UserRankDto("Administrator");
        update.adminView = true;
        update.userEdit = true;
        assertRefused(() -> this.userRankService.updateRank(ADMINISTRATOR_RANK_ID, update));
    }

    @Test
    @TestTransaction
    void testBanAdministratorWhileAnotherRemainsIsAllowed() {
        leaveAdminAsLastAdministrator();
        this.createActiveAdministrator(ADMINISTRATOR_RANK_ID);
        assertTrue(this.userService.patchUser(ADMIN_ID, userPatch(true, null, null)).banned);
    }

    @Test
    @TestTransaction
    void testDeleteAdministratorWhileAnotherRemainsIsAllowed() {
        leaveAdminAsLastAdministrator();
        final var second = this.createActiveAdministrator(ADMINISTRATOR_RANK_ID);
        assertTrue(this.userService.deleteUser(second.publicId));
    }

    @Test
    @TestTransaction
    void testDemoteRankWhileAdministratorInAnotherRankRemainsIsAllowed() throws ReflectiveOperationException {
        leaveAdminAsLastAdministrator();
        this.createActiveAdministrator(this.userRankService.createRank(fullRank("Second Administrator")).publicId);

        final var patch = new UserRankDto();
        patch.userRankEdit = false;
        assertFalse(this.userRankService.patchRank(ADMINISTRATOR_RANK_ID, patch).userRankEdit);
    }

    @Test
    @TestTransaction
    void testStripAnyPermissionFromRankOfLastAdministratorIsRefused() {
        leaveAdminAsLastAdministrator();
        final var patch = new UserRankDto();
        patch.pageAdd = false;
        assertRefused(() -> this.userRankService.patchRank(ADMINISTRATOR_RANK_ID, patch));
    }

    /** Leaves the demo admin the last active administrator beside an active user holding only the former set. */
    private void leaveAdminBesidePartialAdministrator() {
        leaveAdminAsLastAdministrator();
        final var partial = new UserRankDto("Partial Administrator");
        partial.adminView = true;
        partial.userEdit = true;
        partial.userRankEdit = true;
        this.createActiveAdministrator(this.userRankService.createRank(partial).publicId);
    }

    @Test
    @TestTransaction
    void testDemoteLastAdministratorWhilePartialAdministratorRemainsIsRefused() {
        this.leaveAdminBesidePartialAdministrator();
        assertRefused(() -> this.userService.patchUser(ADMIN_ID, userPatch(null, null, USER_RANK_ID)));
    }

    @Test
    @TestTransaction
    void testDeleteLastAdministratorWhilePartialAdministratorRemainsIsRefused() {
        this.leaveAdminBesidePartialAdministrator();
        assertRefused(() -> this.userService.deleteUser(ADMIN_ID));
    }

    @Test
    @TestTransaction
    void testPermissionsAreEveryRankFlag() throws ReflectiveOperationException {
        // Panache turns the public entity fields into private ones with accessors
        assertEquals(
                Arrays.stream(UserRankEntity.class.getDeclaredFields())
                        .filter(field -> field.getType() == Boolean.class).map(Field::getName).sorted().toList(),
                AdministratorGuard.PERMISSIONS.stream().sorted().toList());
        final String full = this.userRankService.createRank(fullRank("Full Rank")).publicId;
        final UserRankEntity rank = UserRankEntity.find("publicId", full).firstResult();
        assertEquals(AdministratorGuard.PERMISSIONS.size(),
                UserRankIdentityAugmentor.buildRolesFromUserRank(rank).size());
    }

    @Test
    @TestTransaction
    void testEditNonAdministratorIsAllowed() {
        leaveAdminAsLastAdministrator();
        assertTrue(this.userService.patchUser(USER_ID, userPatch(true, null, null)).banned);
        final var other = this.userService.createUser(createUniqueUserDto("nonAdmin", "nonadmin"));
        assertTrue(this.userService.deleteUser(other.publicId));
    }

    @Test
    @TestTransaction
    void testEditWithoutAnyAdministratorIsAllowed() {
        UserEntity.update("activated = false");
        assertTrue(this.userService.patchUser(USER_ID, userPatch(true, null, null)).banned);
    }

    /**
     * Two transactions each ban a different one of the last two active administrators at the same time. Each keeps its
     * transaction open after its change until the other has made its own, for up to two seconds. Without the lock both
     * would count two administrators before and one after their own change, and both would commit.
     */
    @Test
    void testConcurrentBansOfLastTwoAdministratorsLeaveOne() throws InterruptedException {
        final List<Long> others = QuarkusTransaction.requiringNew().call(() -> {
            final List<Long> ids = UserEntity.<UserEntity>find("activated = true AND publicId <> ?1", ADMIN_ID).stream()
                    .map(user -> user.id).toList();
            leaveAdminAsLastAdministrator();
            return ids;
        });
        final String second = QuarkusTransaction.requiringNew()
                .call(() -> this.createActiveAdministrator(ADMINISTRATOR_RANK_ID).publicId);
        try {
            final var changed = new CountDownLatch(2);
            final List<Future<Boolean>> bans;
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                bans = Stream.of(ADMIN_ID, second)
                        .map(publicId -> executor.submit(() -> QuarkusTransaction.requiringNew().call(() -> {
                            this.userService.patchUser(publicId, userPatch(true, null, null));
                            changed.countDown();
                            return changed.await(2, TimeUnit.SECONDS);
                        }))).toList();
            }
            int refused = 0;
            for (final Future<Boolean> ban : bans) {
                try {
                    assertFalse(ban.get(), "Both bans were made before either committed");
                } catch (final ExecutionException e) {
                    assertEquals(LAST_ADMINISTRATOR_MESSAGE, e.getCause().getMessage());
                    refused++;
                }
            }
            assertEquals(1, refused);
            assertEquals(1L, QuarkusTransaction.requiringNew()
                    .call(() -> UserEntity.count(AdministratorGuard.ACTIVE_ADMINISTRATOR)));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                UserEntity.delete("publicId", second);
                UserEntity.update("banned = false, activated = true WHERE publicId = ?1", ADMIN_ID);
                if (!others.isEmpty()) {
                    UserEntity.update("activated = true WHERE id IN ?1", others);
                }
            });
        }
    }
}
