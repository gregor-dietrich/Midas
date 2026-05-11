package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultUserGroupJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedUserGroupJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithExistingResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedDelete;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UserGroupResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/user-groups";

    @Test
    void testGetAllUserGroups_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserGroups_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetGroupById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetGroupById_authorizedWithExistingGroup() {
        Integer id = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");

        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/" + id);
    }

    @Test
    void testGetGroupById_authorizedWithNonExistentGroup() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetUsersInGroup_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1/users");
    }

    @Test
    void testGetUsersInGroup_authorizedWithExistingGroup() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/1/users");
    }

    @Test
    void testGetUsersInGroup_authorizedWithNonExistentGroup() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999/users");
    }

    @Test
    void testCreateGroup_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultUserGroupJson());
    }

    @Test
    void testCreateGroup_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createDefaultUserGroupJson());
    }

    @Test
    void testCreateGroup_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createDefaultUserGroupJson());
    }

    @Test
    void testCreateGroup_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateGroup_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateGroup_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateGroup_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUpdatedUserGroupJson());
    }

    @Test
    void testDeleteGroup_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/1");
    }

    @Test
    void testAddUserToGroup_unauthorized() {
        // @formatter:off
        given()
            .contentType(ContentType.JSON)
        .when()
            .post(ENDPOINT_URL + "/1/users/1")
        .then()
            .statusCode(401);
        // @formatter:on
    }

    @Test
    void testRemoveUserFromGroup_unauthorized() {
        // @formatter:off
        given()
        .when()
            .delete(ENDPOINT_URL + "/1/users/1")
        .then()
            .statusCode(401);
        // @formatter:on
    }

    @Test
    void testGetGroupByName_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/name/test-group");
    }

    @Test
    void testGetGroupsForUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/1");
    }

    @Test
    void testGetGroupsForUser_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/1");
    }

    @Test
    void testCheckUserInGroup_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/1/group/1/check");
    }

    @Test
    void testCheckUserInGroup_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/1/group/1/check");
    }

    @Test
    void testSearchGroups_authorized() {
        authenticatedJsonRequest().when().get(ENDPOINT_URL + "/search?query=test").then().statusCode(200);
    }

    @Test
    void testUpdateGroup_authorized() {
        Integer id = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");
        authenticatedJsonRequest().body(createUpdatedUserGroupJson()).when().put(ENDPOINT_URL + "/" + id).then()
                .statusCode(200);
    }

    @Test
    void testPatchGroup_authorized() {
        Integer id = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");
        authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().patch(ENDPOINT_URL + "/" + id).then()
                .statusCode(200);
    }

    @Test
    void testDeleteGroup_authorized() {
        Integer id = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");
        authenticatedJsonRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    @Test
    void testAddUserToGroup_authorized() {
        Integer groupId = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");
        authenticatedJsonRequest().when().post(ENDPOINT_URL + "/" + groupId + "/users/2").then().statusCode(201);
    }

    @Test
    void testRemoveUserFromGroup_authorized() {
        Integer groupId = authenticatedJsonRequest().body(createDefaultUserGroupJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("id");
        authenticatedJsonRequest().when().post(ENDPOINT_URL + "/" + groupId + "/users/2").then().statusCode(201);
        authenticatedJsonRequest().when().delete(ENDPOINT_URL + "/" + groupId + "/users/2").then().statusCode(204);
    }
}
