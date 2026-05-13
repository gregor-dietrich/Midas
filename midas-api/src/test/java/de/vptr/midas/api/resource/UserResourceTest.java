package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultUserUpdateJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueUserJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueUserUpdateJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUserJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedGuestJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedGuestRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithExistingResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedDelete;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPatch;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class UserResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/users";

    @Test
    void testGetAllUsers_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllUsers_authorizedWithSufficientRole() {
        // Test assumes user has appropriate role (user:delete or user:edit)
        authenticatedRequest().when().get(ENDPOINT_URL).then().statusCode(200);
    }

    @Test
    void testGetAllUsers_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL).then().statusCode(403);
    }

    @Test
    void testGetCurrentUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/me");
    }

    @Test
    void testGetCurrentUser_authorized() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/me");
    }

    @Test
    void testCreateUser_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createUniqueUserJson());
    }

    @Test
    @TestTransaction
    void testCreateUser_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createUniqueUserJson());
    }

    @Test
    @TestTransaction
    void testCreateUser_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createUniqueUserJson());
    }

    @Test
    void testCreateUser_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateUser_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateUser_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testCreateUser_authorizedWithShortPassword() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL,
                createUserJson("shortpassworduser", "shortpassword@example.com", "12345"));
    }

    @Test
    void testGetUserByUsername_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/username/admin");
    }

    @Test
    void testGetUserByUsername_authorizedWithExistingUser() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/username/admin");
    }

    @Test
    void testGetUserByUsername_authorizedWithNonExistentUser() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/username/nonexistent_user");
    }

    @Test
    void testGetUserByUsername_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/username/admin").then().statusCode(403);
    }

    @Test
    void testGetUserByEmail_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/email?email=admin@example.com");
    }

    @Test
    void testGetUserByEmail_authorizedWithNonExistentEmail() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/email?email=nonexistent@example.com");
    }

    @Test
    void testGetUserByEmail_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/email?email=admin@example.com").then().statusCode(403);
    }

    @Test
    void testGetUserById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetUserById_authorizedWithExistingUser() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetUserById_authorizedWithNonExistentUser() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetUserById_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testUpdateUser_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createDefaultUserUpdateJson());
    }

    @Test
    void testUpdateUser_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultUserUpdateJson()).when()
                .put(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testUpdateUser_authorized() {
        final String userId = authenticatedJsonRequest().body(createUniqueUserJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");
        authenticatedJsonRequest().body(createUniqueUserJson()).when().put(ENDPOINT_URL + "/" + userId).then()
                .statusCode(200);
    }

    @Test
    void testPatchUser_unauthorized() {
        testUnauthorizedPatch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createDefaultUserUpdateJson());
    }

    @Test
    void testPatchUser_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultUserUpdateJson()).when()
                .patch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testPatchUser_authorized() {
        final String userId = authenticatedJsonRequest().body(createUniqueUserJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");
        authenticatedJsonRequest().body(createUniqueUserUpdateJson()).when().patch(ENDPOINT_URL + "/" + userId).then()
                .statusCode(200);
    }

    @Test
    void testDeleteUser_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testDeleteUser_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().delete(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testDeleteUser_authorized() {
        final String userId = authenticatedJsonRequest().body(createUniqueUserJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + userId).then().statusCode(204);
    }

    @Test
    void testSearchUsers_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchUsers_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/search?query=test").then().statusCode(403);
    }

    @Test
    void testSearchUsers_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testUserResource_Additional404Checks() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/999999").then().statusCode(404);
        authenticatedRequest().when().get(ENDPOINT_URL + "/username/nonexistent-" + TestUtil.generateUniqueSuffix())
                .then().statusCode(404);
        authenticatedRequest().when()
                .get(ENDPOINT_URL + "/email?email=nonexistent-" + TestUtil.generateUniqueSuffix() + "@example.com")
                .then().statusCode(404);
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
        authenticatedRequest().contentType("application/json").body(createUniqueUserJson()).when()
                .put(ENDPOINT_URL + "/999999").then().statusCode(404);
        authenticatedRequest().contentType("application/json").body("{\"username\":\"new\"}").when()
                .patch(ENDPOINT_URL + "/999999").then().statusCode(404);
    }

    @Test
    void testGetUserByEmail_authorizedWithExistingEmail() {
        final String email = "findme_" + TestUtil.generateUniqueSuffix() + "@example.com";
        final String jsonBody = createUserJson("findme_" + TestUtil.generateUniqueSuffix(), email, "validpass123");
        authenticatedJsonRequest().body(jsonBody).when().post(ENDPOINT_URL).then().statusCode(201);

        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/email?email=" + email);
    }

    @Test
    void testCreateUser_customRank() {
        final String jsonBody = createUserJson("customrankuser", "customrank@example.com", "validpass123");
        final String customJson = jsonBody.replace("}", ",\"rankPublicId\":\"01AN4Z07BY79KA1307SR9X4MV3\"}");
        authenticatedJsonRequest().body(customJson).when().post(ENDPOINT_URL).then().statusCode(201);
    }
}
