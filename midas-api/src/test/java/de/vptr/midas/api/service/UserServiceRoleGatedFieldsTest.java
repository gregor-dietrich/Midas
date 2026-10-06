package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserDto;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserResponseDto;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Tests that a caller without {@code user-rank:edit} or {@code admin:view} cannot write back the rank or status it read
 * before a concurrent change: the service keeps them at their current values inside the change's transaction (#54). The
 * editor holds {@code user:edit} and {@code admin:view} but not {@code user-rank:edit}; an unknown caller holds
 * neither.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
class UserServiceRoleGatedFieldsTest {

    /** Demo ranks: User holds postCommentAdd, which the editor holds too; Guest holds no permission. */
    private static final String USER_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV4";
    private static final String GUEST_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV5";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    private UserResponseDto createUserWithRank(final String rankPublicId) {
        final var dto = createUniqueUserDto("protected", "protected");
        dto.rankPublicId = rankPublicId;
        return this.userService.createUser(dto);
    }

    /** Creates a caller whose rank holds user:edit and admin:view, but not user-rank:edit, and returns its name. */
    private String createEditor() {
        final var rank = new UserRankDto();
        rank.name = "editor_" + generateUniqueTestSuffix();
        rank.userEdit = true;
        rank.adminView = true;
        rank.postCommentAdd = true;
        return this.createUserWithRank(this.userRankService.createRank(rank).publicId).username;
    }

    /** The user as the caller read it, sent back unchanged. */
    private static UserDto staleDto(final UserResponseDto read) {
        return new UserDto(read.username, null, read.email, read.rankPublicId, read.banned, read.activated, null);
    }

    /** A user at the User rank that a concurrent change has since moved to Guest. */
    private UserResponseDto createDemotedUser() {
        final var stale = this.createUserWithRank(USER_RANK_ID);
        this.userService.patchUser(stale.publicId, new UserDto(null, null, null, GUEST_RANK_ID, null, null, null));
        return stale;
    }

    @Test
    @TestTransaction
    void testPatchWithoutRankRoleKeepsCurrentRankButSetsStatus() {
        final var stale = this.createDemotedUser();
        final var change = staleDto(stale);
        change.banned = true;

        final var patched = this.userService.patchUser(stale.publicId, change, this.createEditor());
        assertEquals(GUEST_RANK_ID, patched.rankPublicId);
        assertTrue(patched.banned);
    }

    @Test
    @TestTransaction
    void testUpdateWithoutRankRoleKeepsCurrentRankButSetsStatus() {
        final var stale = this.createDemotedUser();
        final var change = staleDto(stale);
        change.banned = true;

        final var updated = this.userService.updateUser(stale.publicId, change, this.createEditor());
        assertEquals(GUEST_RANK_ID, updated.rankPublicId);
        assertTrue(updated.banned);
    }

    @Test
    @TestTransaction
    void testPatchWithoutAdminViewKeepsCurrentStatus() {
        final var stale = this.createUserWithRank(GUEST_RANK_ID);
        this.userService.patchUser(stale.publicId, new UserDto(null, null, null, null, true, true, null));

        final var patched = this.userService.patchUser(stale.publicId, staleDto(stale), "no-such-user");
        assertTrue(patched.banned);
        assertTrue(patched.activated);
    }

    @Test
    @TestTransaction
    void testUpdateWithoutAdminViewKeepsCurrentStatus() {
        final var stale = this.createUserWithRank(GUEST_RANK_ID);
        this.userService.patchUser(stale.publicId, new UserDto(null, null, null, null, true, true, null));

        final var updated = this.userService.updateUser(stale.publicId, staleDto(stale), "no-such-user");
        assertTrue(updated.banned);
        assertTrue(updated.activated);
    }

    @Test
    @TestTransaction
    void testCallerWithBothRolesSetsRoleGatedFields() {
        final var created = this.createUserWithRank(USER_RANK_ID);
        final var patched = this.userService.patchUser(created.publicId,
                new UserDto(null, null, null, GUEST_RANK_ID, true, true, null), "admin");
        assertEquals(GUEST_RANK_ID, patched.rankPublicId);
        assertTrue(patched.banned);
        assertTrue(patched.activated);
    }
}
