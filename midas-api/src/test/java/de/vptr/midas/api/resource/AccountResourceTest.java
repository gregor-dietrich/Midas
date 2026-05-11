package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createAccountJson;
import static de.vptr.midas.api.util.TestDataBuilder.createDefaultAccountJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createPaymentJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueAccountJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUserAccountMetaJson;
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
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
class AccountResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/accounts";
    private static final long ADMIN_USER_ID = 1L; // admin user always created with id=1 in seed data
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Test
    void testGetAllUserAccounts_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserAccounts_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserAccounts_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL).then().statusCode(403);
    }

    @Test
    void testGetUserAccountById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetUserAccountById_authorizedWithExistingAccount() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetUserAccountById_authorizedWithNonExistentAccount() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetUserAccountById_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/1").then().statusCode(403);
    }

    @Test
    void testCreateUserAccount_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultAccountJson());
    }

    @Test
    void testCreateUserAccount_authorized() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createDefaultAccountJson());
    }

    @Test
    void testCreateUserAccount_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createDefaultAccountJson());
    }

    @Test
    void testCreateUserAccount_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateUserAccount_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateUserAccount_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateUserAccount_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createDefaultAccountJson());
    }

    @Test
    void testUpdateUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultAccountJson()).when().put(ENDPOINT_URL + "/1").then()
                .statusCode(403);
    }

    @Test
    void testPatchUserAccount_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createDefaultAccountJson());
    }

    @Test
    void testPatchUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultAccountJson()).when().patch(ENDPOINT_URL + "/1").then()
                .statusCode(403);
    }

    @Test
    void testPatchUserAccount_authorizedWithoutNamePreservesExistingName() {
        final String originalName = "Patch Account " + TestUtil.generateUniqueSuffix();
        final Integer accountId =
                authenticatedRequest().contentType(ContentType.JSON).body(createAccountJson(originalName)).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        authenticatedRequest().contentType(ContentType.JSON).body(createEmptyJson()).when()
                .patch(ENDPOINT_URL + "/" + accountId).then().statusCode(200).body("name", equalTo(originalName));
    }

    @Test
    void testDeleteUserAccount_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/1");
    }

    @Test
    void testDeleteUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().delete(ENDPOINT_URL + "/1").then().statusCode(403);
    }

    @Test
    void testDeleteUserAccount_authorized() {
        final Integer accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when()
                .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + accountId).then().statusCode(204);
    }

    @Test
    void testCreateMyAccountPayment_ignoresSpoofedUserId() {
        final String clientKey = uniqueClientKey();
        final long sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final long targetAccountId = this.createAdminAccessibleAccount(clientKey);

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, 2L, "100.00", "Spoofed payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments").then().statusCode(201)
                .body("userId", equalTo(1)).body("username", equalTo("admin"));
    }

    @Test
    void testUpdateMyAccountPayment_preservesExistingUserId() {
        final String clientKey = uniqueClientKey();
        final long sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final long targetAccountId = this.createAdminAccessibleAccount(clientKey);

        final Integer paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, 1L, "100.00", "Original payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments").then().statusCode(201).extract()
                .path("id");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, 2L, "150.00", "Updated payment",
                        "2024-01-02"))
                .when().put(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments/" + paymentId).then().statusCode(200)
                .body("userId", equalTo(1)).body("username", equalTo("admin"))
                .body("comment", equalTo("Updated payment"));
    }

    @Test
    void testCreateMyAccountPayment_allowsPaymentsBetweenAnyAccessibleAccounts() {
        final String clientKey = uniqueClientKey();
        final long scopedAccountId = this.createAdminAccessibleAccount(clientKey);
        final long sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final long targetAccountId = this.createAdminAccessibleAccount(clientKey);

        // Payment not involving the scoped account is now permitted as long as
        // the user has access to both source and target accounts.
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, 1L, "100.00", "Off-account payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments").then().statusCode(201);
    }

    @Test
    void testUpdateMyAccountPayment_allowsMovingPaymentToOtherAccessibleAccounts() {
        final String clientKey = uniqueClientKey();
        final long scopedAccountId = this.createAdminAccessibleAccount(clientKey);
        final long originalTargetAccountId = this.createAdminAccessibleAccount(clientKey);
        final long replacementSourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final long replacementTargetAccountId = this.createAdminAccessibleAccount(clientKey);

        final Integer paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(originalTargetAccountId, scopedAccountId, 1L, "100.00", "Original payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments").then().statusCode(201).extract()
                .path("id");

        // Updating to accounts not involving scopedAccountId is now permitted.
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(replacementTargetAccountId, replacementSourceAccountId, 2L, "150.00",
                        "Moved payment", "2024-01-02"))
                .when().put(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments/" + paymentId).then().statusCode(200);
    }

    @Test
    void testUpdateUserAccountMeta_setsIsNegativeFlag() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);

        // Set isNegative = true
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(createUserAccountMetaJson(true))
                .when().patch(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(204);

        // Verify flag is reflected in GET /accounts/{id}/users
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/" + accountId + "/users").then()
                .statusCode(200).body("[0].isNegative", equalTo(true));
    }

    @Test
    void testGetAccountByName_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/name/test-account");
    }

    @Test
    void testGetAccountByName_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/name/test").then().statusCode(403);
    }

    @Test
    void testGetAccountByName_authorized() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/name/nonexistent");
    }

    @Test
    void testSearchAccounts_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchAccounts_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/search?query=test").then().statusCode(403);
    }

    @Test
    void testSearchAccounts_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testGetAssociatedUsers_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1/users");
    }

    @Test
    void testGetAssociatedUsers_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/1/users").then().statusCode(403);
    }

    @Test
    void testGetAssociatedUsers_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/1/users");
    }

    @Test
    void testGetAccountPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1/payments");
    }

    @Test
    void testGetAccountPayments_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/1/payments").then().statusCode(403);
    }

    @Test
    void testGetAccountPayments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/1/payments");
    }

    @Test
    void testGetAccountBalance_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1/balance");
    }

    @Test
    void testGetAccountBalance_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/1/balance").then().statusCode(403);
    }

    @Test
    void testGetAccountBalance_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/1/balance");
    }

    @Test
    void testGetAccountsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/1");
    }

    @Test
    void testGetAccountsByUser_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/user/1").then().statusCode(403);
    }

    @Test
    void testGetAccountsByUser_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/1");
    }

    @Test
    void testGetMyAccountBalance_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/balance").then()
                .statusCode(200).body("balance", equalTo(0));
    }

    @Test
    void testGetMyAccountBalance_accessDenied() {
        forwardedAuthenticatedRequest("not-authorized").when().get(ENDPOINT_URL + "/my/999/balance").then()
                .statusCode(403);
    }

    @Test
    void testGetMyAccountPayments_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments").then()
                .statusCode(200);
    }

    @Test
    void testGetMyAccountOutgoingPayments_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments/outgoing")
                .then().statusCode(200);
    }

    @Test
    void testGetMyAccountIncomingPayments_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments/incoming")
                .then().statusCode(200);
    }

    @Test
    void testGetMyAccounts_authorized() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/my").then().statusCode(200);
    }

    @Test
    void testGetMyAccountsWithBalances_authorized() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/my/with-balances").then().statusCode(200);
    }

    @Test
    void testCheckMyAccountAccess_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/check").then()
                .statusCode(200).body("hasAccess", equalTo(true));
    }

    @Test
    void testUpdateUserAccount_authorized() {
        final Integer accountId = authenticatedRequest().contentType(ContentType.JSON).body(createDefaultAccountJson())
                .when().post(ENDPOINT_URL).then().statusCode(201).extract().path("id");
        authenticatedRequest().contentType(ContentType.JSON).body(createDefaultAccountJson()).when()
                .put(ENDPOINT_URL + "/" + accountId).then().statusCode(200);
    }

    @Test
    void testRemoveUserFromAccount_authorized() {
        final String clientKey = uniqueClientKey();
        final long accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when()
                .delete(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(204);
    }

    private long createAdminAccessibleAccount(final String clientKey) {
        final Integer accountId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createUniqueAccountJson()).when().post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).when()
                .post(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(201);

        return accountId;
    }

    private static RequestSpecification forwardedAuthenticatedRequest(final String clientKey) {
        return authenticatedRequest().header(FORWARDED_FOR_HEADER, clientKey);
    }

    private static String uniqueClientKey() {
        return "account-resource-" + TestUtil.generateUniqueSuffix();
    }
}
