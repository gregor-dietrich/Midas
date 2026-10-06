package de.vptr.midas.api.resource;

import static de.vptr.midas.api.service.PrivilegeCeiling.GRANT_MESSAGE;
import static de.vptr.midas.api.service.PrivilegeCeiling.MANAGE_RANK_MESSAGE;
import static de.vptr.midas.api.service.PrivilegeCeiling.MANAGE_USER_MESSAGE;
import static de.vptr.midas.api.service.UserRankService.DEFAULT_RANK_MESSAGE;
import static de.vptr.midas.api.util.TestDataBuilder.createUserJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.generateUniqueEmail;
import static de.vptr.midas.api.util.TestUtil.generateUniqueSuffix;
import static de.vptr.midas.api.util.TestUtil.generateUniqueUsername;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests the privilege ceiling through the REST API. The caller is an editor whose rank holds the user and rank
 * management permissions but none of the content ones; the higher rank adds {@code pageAdd} to it, the demo Guest rank
 * holds no permission, and the default User rank holds {@code postCommentAdd}, which the editor lacks. Every account
 * and rank changed here is created for the test.
 */
@QuarkusTest
class PrivilegeCeilingResourceTest {

    private static final String USERS = "/api/v1/users";
    private static final String RANKS = "/api/v1/user-ranks";
    private static final String ADMIN_ID = "01AN4Z07BY79KA1307SR9X4MV7";
    private static final String GUEST_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV5";
    private static final String DEFAULT_RANK_ID = "01AN4Z07BY79KA1307SR9X4MV4";
    private static final String PASSWORD = "ValidPass123!";
    private static final String NEW_PASSWORD = "OtherPass456!";
    private static final String EDITOR_FLAGS = "\"adminView\":true,\"userAdd\":true,\"userDelete\":true,"
            + "\"userEdit\":true,\"userRankAdd\":true,\"userRankDelete\":true,\"userRankEdit\":true";
    private static final String HIGHER_FLAGS = EDITOR_FLAGS + ",\"pageAdd\":true";

    private record Account(String publicId, String username, String rankId) {
    }

    private static String rankJson(final String name, final String flags) {
        return "{\"name\":\"" + name + "\"" + (flags.isEmpty() ? "" : "," + flags) + "}";
    }

    private static String rankJson(final String flags) {
        return rankJson("rank_" + generateUniqueSuffix(), flags);
    }

    private static String createRank(final String flags) {
        return authenticatedJsonRequest().body(rankJson(flags)).when().post(RANKS).then().statusCode(201).extract()
                .path("publicId");
    }

    private static Account createAccount(final String rankId) {
        final String username = generateUniqueUsername("ceiling");
        final String publicId = authenticatedJsonRequest()
                .body(createUserJson(username, generateUniqueEmail("ceiling"), PASSWORD).replace("}",
                        ",\"rankPublicId\":\"" + rankId + "\",\"activated\":true}"))
                .when().post(USERS).then().statusCode(201).extract().path("publicId");
        return new Account(publicId, username, rankId);
    }

    private static Account createEditor() {
        return createAccount(createRank(EDITOR_FLAGS));
    }

    private static String user(final Account account) {
        return USERS + "/" + account.publicId();
    }

    private static String rank(final String rankId) {
        return RANKS + "/" + rankId;
    }

    private static String rankIdJson(final String rankId) {
        return "{\"rankPublicId\":\"" + rankId + "\"}";
    }

    private static void assertResponse(final Account caller, final String method, final String path, final String body,
            final int status) {
        authenticatedJsonRequest(caller.username(), PASSWORD).body(body).when().request(method, path).then()
                .statusCode(status);
    }

    private static void assertRefused(final Account caller, final String method, final String path, final String body,
            final String message) {
        authenticatedJsonRequest(caller.username(), PASSWORD).body(body).when().request(method, path).then()
                .statusCode(403).body("status", equalTo(403)).body("message", equalTo(message));
    }

    /** Asserts that the account still logs in with its original username and password, on its rank and unbanned. */
    private static void assertUnchanged(final Account account) {
        authenticatedJsonRequest(account.username(), PASSWORD).when().get(USERS + "/me").then().statusCode(200)
                .body("username", equalTo(account.username())).body("rankPublicId", equalTo(account.rankId()))
                .body("banned", equalTo(false)).body("activated", equalTo(true));
    }

    /** The rank as an administrator reads it, to compare before and after a refused change. */
    private static String rankState(final String rankId) {
        return authenticatedRequest().when().get(rank(rankId)).then().statusCode(200).extract().asString();
    }

    // The issue's reproducer: a rank holding only userEdit resets an administrator's password

    @Test
    void testPasswordOfAdministratorByUserEditOnlyIsRefused() {
        final Account editor = createAccount(createRank("\"userEdit\":true"));
        assertRefused(editor, "PATCH", USERS + "/" + ADMIN_ID, "{\"password\":\"" + NEW_PASSWORD + "\"}",
                MANAGE_USER_MESSAGE);
        authenticatedRequest().when().get(USERS + "/me").then().statusCode(200);
    }

    // Changes to a user whose rank holds a permission the caller lacks

    @Test
    void testPasswordOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        assertRefused(createEditor(), "PATCH", user(target), "{\"password\":\"" + NEW_PASSWORD + "\"}",
                MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testUsernameOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        assertRefused(createEditor(), "PATCH", user(target), "{\"username\":\"" + target.username() + "x\"}",
                MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testRankOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        assertRefused(createEditor(), "PATCH", user(target), rankIdJson(GUEST_RANK_ID), MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testBanOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        assertRefused(createEditor(), "PATCH", user(target), "{\"banned\":true}", MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testDeactivationOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        assertRefused(createEditor(), "PATCH", user(target), "{\"activated\":false}", MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testReplacementOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        final String body = createUserJson(target.username(), generateUniqueEmail("ceiling"), NEW_PASSWORD).replace("}",
                ",\"rankPublicId\":\"" + target.rankId() + "\",\"activated\":true}");
        assertRefused(createEditor(), "PUT", user(target), body, MANAGE_USER_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testDeleteOfHigherUserIsRefused() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        authenticatedJsonRequest(createEditor().username(), PASSWORD).when().delete(user(target)).then().statusCode(403)
                .body("message", equalTo(MANAGE_USER_MESSAGE));
        assertUnchanged(target);
    }

    // Giving a user a rank above the caller's

    @Test
    void testCreateUserWithHigherRankIsRefused() {
        final String username = generateUniqueUsername("ceiling");
        final String body = createUserJson(username, generateUniqueEmail("ceiling"), PASSWORD).replace("}",
                ",\"rankPublicId\":\"" + createRank(HIGHER_FLAGS) + "\"}");
        assertRefused(createEditor(), "POST", USERS, body, GRANT_MESSAGE);
        authenticatedRequest().when().get(USERS + "/username/" + username).then().statusCode(404);
    }

    @Test
    void testAssignHigherRankToLowerUserIsRefused() {
        final Account target = createAccount(GUEST_RANK_ID);
        assertRefused(createEditor(), "PATCH", user(target), rankIdJson(createRank(HIGHER_FLAGS)), GRANT_MESSAGE);
        assertUnchanged(target);
    }

    @Test
    void testAssignHigherRankToSelfIsRefused() {
        final Account editor = createEditor();
        assertRefused(editor, "PATCH", user(editor), rankIdJson(createRank(HIGHER_FLAGS)), GRANT_MESSAGE);
        assertUnchanged(editor);
    }

    @Test
    void testAssignDefaultRankOnUpdateIsRefused() {
        // Only a new account may get the default rank above the caller's; update and patch stay strict
        final Account editor = createEditor();
        final Account target = createAccount(GUEST_RANK_ID);
        assertRefused(editor, "PATCH", user(editor), rankIdJson(DEFAULT_RANK_ID), GRANT_MESSAGE);
        assertRefused(editor, "PATCH", user(target), rankIdJson(DEFAULT_RANK_ID), GRANT_MESSAGE);
        assertUnchanged(editor);
        assertUnchanged(target);
    }

    // Ranks that hold or would grant a permission the caller lacks

    @Test
    void testCreateRankWithPermissionCallerLacksIsRefused() {
        final String name = "rank_" + generateUniqueSuffix();
        assertRefused(createEditor(), "POST", RANKS, rankJson(name, "\"pageAdd\":true"), GRANT_MESSAGE);
        authenticatedRequest().when().get(RANKS + "/name/" + name).then().statusCode(404);
    }

    @Test
    void testGrantOwnRankPermissionCallerLacksIsRefused() {
        final Account editor = createEditor();
        final String before = rankState(editor.rankId());
        assertRefused(editor, "PATCH", rank(editor.rankId()), "{\"pageAdd\":true}", GRANT_MESSAGE);
        assertRefused(editor, "PUT", rank(editor.rankId()), rankJson(HIGHER_FLAGS), GRANT_MESSAGE);
        assertEquals(before, rankState(editor.rankId()));
    }

    @Test
    void testGrantLowerRankPermissionCallerLacksIsRefused() {
        final String lowerRank = createRank("\"userEdit\":true");
        final String before = rankState(lowerRank);
        assertRefused(createEditor(), "PATCH", rank(lowerRank), "{\"pageAdd\":true}", GRANT_MESSAGE);
        assertEquals(before, rankState(lowerRank));
    }

    @Test
    void testEditHigherRankIsRefused() {
        final String higherRank = createRank(HIGHER_FLAGS);
        final String before = rankState(higherRank);
        final Account editor = createEditor();
        assertRefused(editor, "PATCH", rank(higherRank), rankJson(""), MANAGE_RANK_MESSAGE);
        // Stripping the rank down to the caller's own permissions is a change to it as well
        assertRefused(editor, "PUT", rank(higherRank), rankJson("\"userEdit\":true"), MANAGE_RANK_MESSAGE);
        assertEquals(before, rankState(higherRank));
    }

    @Test
    void testDeleteHigherRankIsRefused() {
        final String higherRank = createRank(HIGHER_FLAGS);
        final String before = rankState(higherRank);
        authenticatedJsonRequest(createEditor().username(), PASSWORD).when().delete(rank(higherRank)).then()
                .statusCode(403).body("message", equalTo(MANAGE_RANK_MESSAGE));
        assertEquals(before, rankState(higherRank));
    }

    @Test
    void testDeleteDefaultRankIsRefusedEvenForAdministrator() {
        final String before = rankState(DEFAULT_RANK_ID);
        authenticatedRequest().when().delete(rank(DEFAULT_RANK_ID)).then().statusCode(422).body("message",
                equalTo(DEFAULT_RANK_MESSAGE));
        assertEquals(before, rankState(DEFAULT_RANK_ID));
    }

    // Allowed changes

    @Test
    void testEqualUserIsManaged() {
        final Account editor = createEditor();
        final Account peer = createAccount(createRank(EDITOR_FLAGS));
        assertResponse(editor, "PATCH", user(peer), "{\"password\":\"" + NEW_PASSWORD + "\",\"banned\":true}", 200);
        authenticatedJsonRequest(editor.username(), PASSWORD).when().delete(user(peer)).then().statusCode(204);
    }

    @Test
    void testLowerUserIsManaged() {
        final Account editor = createEditor();
        final Account target = createAccount(GUEST_RANK_ID);
        assertResponse(editor, "PATCH", user(target), "{\"username\":\"" + target.username() + "x\",\"password\":\""
                + NEW_PASSWORD + "\",\"activated\":false}", 200);
        assertResponse(editor, "PATCH", user(target), rankIdJson(editor.rankId()), 200);
        authenticatedJsonRequest(editor.username(), PASSWORD).when().delete(user(target)).then().statusCode(204);
    }

    @Test
    void testCreateUserWithDefaultRankIsAllowed() {
        // The editor lacks the default rank's postCommentAdd, but every new account may get the default rank
        final String username = generateUniqueUsername("ceiling");
        assertResponse(createEditor(), "POST", USERS,
                createUserJson(username, generateUniqueEmail("ceiling"), PASSWORD), 201);
        authenticatedRequest().when().get(USERS + "/username/" + username).then().statusCode(200).body("rankPublicId",
                equalTo(DEFAULT_RANK_ID));
    }

    @Test
    void testCreateUserByUserAddOnlyGetsDefaults() {
        // user:add alone cannot create an account it could log in as, nor pick its rank or status
        final String username = generateUniqueUsername("ceiling");
        assertResponse(createAccount(createRank("\"userAdd\":true")), "POST", USERS,
                createUserJson(username, generateUniqueEmail("ceiling"), PASSWORD).replace("}",
                        ",\"rankPublicId\":\"" + GUEST_RANK_ID + "\",\"banned\":true,\"activated\":true}"),
                201);
        authenticatedRequest().when().get(USERS + "/username/" + username).then().statusCode(200)
                .body("rankPublicId", equalTo(DEFAULT_RANK_ID)).body("banned", equalTo(false))
                .body("activated", equalTo(false));
        authenticatedJsonRequest(username, PASSWORD).when().get(USERS + "/me").then().statusCode(401);
    }

    @Test
    void testOwnAccountIsManaged() {
        final Account editor = createEditor();
        assertResponse(editor, "PATCH", user(editor), "{\"email\":\"" + generateUniqueEmail("ceiling") + "\"}", 200);
        assertResponse(editor, "POST", USERS + "/me/password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}", 200);
        authenticatedJsonRequest(editor.username(), NEW_PASSWORD).when().get(USERS + "/me").then().statusCode(200);
    }

    @Test
    void testRankWithinCeilingIsManaged() {
        final Account editor = createEditor();
        final String rank = authenticatedJsonRequest(editor.username(), PASSWORD).body(rankJson("\"userEdit\":true"))
                .when().post(RANKS).then().statusCode(201).extract().path("publicId");
        assertResponse(editor, "PATCH", rank(rank), "{\"userDelete\":true}", 200);
    }

    @Test
    void testEqualAndLowerUnusedRanksAreDeleted() {
        final Account editor = createEditor();
        for (final String unused : new String[] { createRank(EDITOR_FLAGS), createRank("\"userEdit\":true") }) {
            authenticatedJsonRequest(editor.username(), PASSWORD).when().delete(rank(unused)).then().statusCode(204);
        }
    }

    @Test
    void testAdministratorManagesEveryone() {
        final Account target = createAccount(createRank(HIGHER_FLAGS));
        authenticatedJsonRequest().body("{\"password\":\"" + NEW_PASSWORD + "\",\"banned\":true}").when()
                .patch(user(target)).then().statusCode(200);
        authenticatedRequest().when().delete(user(target)).then().statusCode(204);
    }
}
