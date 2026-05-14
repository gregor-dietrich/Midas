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
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@SuppressWarnings("PMD.TooManyMethods")
class AccountResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/accounts";
    private static final String ADMIN_USER_ID = "01AN4Z07BY79KA1307SR9X4MV7"; // admin user public ID
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
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetUserAccountById_authorizedWithExistingAccount() {
        final String accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/" + accountId);
    }

    @Test
    void testGetUserAccountById_authorizedWithNonExistentAccount() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetUserAccountById_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
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
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createDefaultAccountJson());
    }

    @Test
    void testUpdateUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultAccountJson()).when()
                .put(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testPatchUserAccount_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createDefaultAccountJson());
    }

    @Test
    void testPatchUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createDefaultAccountJson()).when()
                .patch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testPatchUserAccount_authorizedWithoutNamePreservesExistingName() {
        final String originalName = "Patch Account " + TestUtil.generateUniqueSuffix();
        final String accountId =
                authenticatedRequest().contentType(ContentType.JSON).body(createAccountJson(originalName)).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("publicId");

        authenticatedRequest().contentType(ContentType.JSON).body(createEmptyJson()).when()
                .patch(ENDPOINT_URL + "/" + accountId).then().statusCode(200).body("name", equalTo(originalName));
    }

    @Test
    void testDeleteUserAccount_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testDeleteUserAccount_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().delete(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(403);
    }

    @Test
    void testDeleteUserAccount_authorized() {
        final String accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + accountId).then().statusCode(204);
    }

    @Test
    void testCreateMyAccountPayment_ignoresSpoofedUserId() {
        final String clientKey = uniqueClientKey();
        final String sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, "spoofed_user", "100.00", "Spoofed payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments").then().statusCode(201)
                .body("userPublicId", equalTo(ADMIN_USER_ID)).body("username", equalTo("admin"));
    }

    @Test
    void testUpdateMyAccountPayment_preservesExistingUserId() {
        final String clientKey = uniqueClientKey();
        final String sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, ADMIN_USER_ID, "100.00", "Original payment",
                        "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments").then().statusCode(201).extract()
                .path("publicId");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, "spoofed_user", "150.00", "Updated payment",
                        "2024-01-02"))
                .when().put(ENDPOINT_URL + "/my/" + sourceAccountId + "/payments/" + paymentId).then().statusCode(200)
                .body("userPublicId", equalTo(ADMIN_USER_ID)).body("username", equalTo("admin"))
                .body("comment", equalTo("Updated payment"));
    }

    @Test
    void testCreateMyAccountPayment_allowsPaymentsBetweenAnyAccessibleAccounts() {
        final String clientKey = uniqueClientKey();
        final String scopedAccountId = this.createAdminAccessibleAccount(clientKey);
        final String sourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        // Payment not involving the scoped account is now permitted as long as
        // the user has access to both source and target accounts.
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(targetAccountId, sourceAccountId, ADMIN_USER_ID, "100.00",
                        "Off-account payment", "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments").then().statusCode(201);
    }

    @Test
    void testUpdateMyAccountPayment_allowsMovingPaymentToOtherAccessibleAccounts() {
        final String clientKey = uniqueClientKey();
        final String scopedAccountId = this.createAdminAccessibleAccount(clientKey);
        final String originalTargetAccountId = this.createAdminAccessibleAccount(clientKey);
        final String replacementSourceAccountId = this.createAdminAccessibleAccount(clientKey);
        final String replacementTargetAccountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(originalTargetAccountId, scopedAccountId, ADMIN_USER_ID, "100.00",
                        "Original payment", "2024-01-01"))
                .when().post(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments").then().statusCode(201).extract()
                .path("publicId");

        // Updating to accounts not involving scopedAccountId is now permitted.
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createPaymentJson(replacementTargetAccountId, replacementSourceAccountId, ADMIN_USER_ID, "150.00",
                        "Moved payment", "2024-01-02"))
                .when().put(ENDPOINT_URL + "/my/" + scopedAccountId + "/payments/" + paymentId).then().statusCode(200);
    }

    @Test
    void testUpdateUserAccountMeta_setsIsNegativeFlag() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);

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
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/users");
    }

    @Test
    void testGetAssociatedUsers_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/users").then()
                .statusCode(403);
    }

    @Test
    void testGetAssociatedUsers_authorized() {
        final String accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/" + accountId + "/users");
    }

    @Test
    void testGetAccountPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/payments");
    }

    @Test
    void testGetAccountPayments_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/payments").then()
                .statusCode(403);
    }

    @Test
    void testGetAccountPayments_authorized() {
        final String accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/" + accountId + "/payments");
    }

    @Test
    void testGetAccountBalance_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/balance");
    }

    @Test
    void testGetAccountBalance_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7/balance").then()
                .statusCode(403);
    }

    @Test
    void testGetAccountBalance_authorized() {
        final String accountId = authenticatedJsonRequest().body(createDefaultAccountJson()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/" + accountId + "/balance");
    }

    @Test
    void testGetAccountsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/" + ADMIN_USER_ID);
    }

    @Test
    void testGetAccountsByUser_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/user/" + ADMIN_USER_ID).then().statusCode(403);
    }

    @Test
    void testGetAccountsByUser_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/" + ADMIN_USER_ID);
    }

    @Test
    void testGetMyAccountBalance_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
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
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments").then()
                .statusCode(200);
    }

    @Test
    void testGetMyAccountOutgoingPayments_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments/outgoing")
                .then().statusCode(200);
    }

    @Test
    void testGetMyAccountIncomingPayments_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
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
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/check").then()
                .statusCode(200).body("hasAccess", equalTo(true));
    }

    @Test
    void testUpdateUserAccount_authorized() {
        final String accountId = authenticatedRequest().contentType(ContentType.JSON).body(createDefaultAccountJson())
                .when().post(ENDPOINT_URL).then().statusCode(201).extract().path("publicId");
        authenticatedRequest().contentType(ContentType.JSON).body(createDefaultAccountJson()).when()
                .put(ENDPOINT_URL + "/" + accountId).then().statusCode(200);
    }

    @Test
    void testRemoveUserFromAccount_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        forwardedAuthenticatedRequest(clientKey).when()
                .delete(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(204);
    }

    private String createAdminAccessibleAccount(final String clientKey) {
        final String accountId =
                forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(createUniqueAccountJson())
                        .when().post(ENDPOINT_URL).then().statusCode(201).extract().path("publicId");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).when()
                .post(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(201);

        // Grant canManage permission so payment tests can proceed
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(createUserAccountMetaJson(false, true)).when()
                .patch(ENDPOINT_URL + "/" + accountId + "/users/" + ADMIN_USER_ID).then().statusCode(204);

        return accountId;
    }

    private static RequestSpecification forwardedAuthenticatedRequest(final String clientKey) {
        return authenticatedRequest().header(FORWARDED_FOR_HEADER, clientKey);
    }

    private static String uniqueClientKey() {
        return "account-resource-" + TestUtil.generateUniqueSuffix();
    }

    @Test
    void testCreateAndUpdateMyAccountPayment_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentJson =
                createPaymentJson(targetAccountId, accountId, ADMIN_USER_ID, "10.00", "My payment", "2024-01-01");

        final String paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(paymentJson).when().post(ENDPOINT_URL + "/my/" + accountId + "/payments").then().statusCode(201)
                .extract().path("publicId");

        if (paymentId != null) {
            final String updatedJson = createPaymentJson(targetAccountId, accountId, ADMIN_USER_ID, "20.00",
                    "Updated payment", "2024-01-01");
            forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(updatedJson).when()
                    .put(ENDPOINT_URL + "/my/" + accountId + "/payments/" + paymentId).then().statusCode(200);
        }
    }

    @Test
    void testCreateAndUpdateMyAccountPayment_forbidden() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentJson =
                createPaymentJson(targetAccountId, accountId, ADMIN_USER_ID, "10.00", "My payment", "2024-01-01");

        authenticatedGuestJsonRequest().body(paymentJson).when().post(ENDPOINT_URL + "/my/" + accountId + "/payments")
                .then().statusCode(403);

        authenticatedGuestJsonRequest().body(paymentJson).when()
                .put(ENDPOINT_URL + "/my/" + accountId + "/payments/01AN4Z07BY79KA1307SR9X4MV9").then().statusCode(403);
    }

    @Test
    void testCreateAndUpdateMyAccountPayment_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL + "/my/01AN4Z07BY79KA1307SR9X4MV7/payments", createEmptyJson());
        testUnauthorizedPut(ENDPOINT_URL + "/my/01AN4Z07BY79KA1307SR9X4MV7/payments/01AN4Z07BY79KA1307SR9X4MV8",
                createEmptyJson());
    }

    @Test
    void testCreateAndUpdateMyAccountPayment_validationErrors() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(createEmptyJson()).when()
                .post(ENDPOINT_URL + "/my/" + accountId + "/payments").then().statusCode(422);

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(createEmptyJson()).when()
                .put(ENDPOINT_URL + "/my/" + accountId + "/payments/01AN4Z07BY79KA1307SR9X4MV8").then().statusCode(422);
    }

    @Test
    void testUpdateMyAccountPayment_notFound() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentJson =
                createPaymentJson(targetAccountId, accountId, ADMIN_USER_ID, "10.00", "My payment", "2024-01-01");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(paymentJson).when()
                .put(ENDPOINT_URL + "/my/" + accountId + "/payments/999999").then().statusCode(404);
    }

    @Test
    void testDeleteMyAccountPayment_authorized() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);

        final String paymentJson =
                createPaymentJson(accountId, null, ADMIN_USER_ID, "10.00", "Delete test", "2024-01-01");

        final String paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(paymentJson).when().post(ENDPOINT_URL + "/my/" + accountId + "/payments").then().statusCode(201)
                .extract().path("publicId");

        if (paymentId != null) {
            forwardedAuthenticatedRequest(clientKey).when()
                    .delete(ENDPOINT_URL + "/my/" + accountId + "/payments/" + paymentId).then().statusCode(204);

            final var payments =
                    forwardedAuthenticatedRequest(clientKey).when().get(ENDPOINT_URL + "/my/" + accountId + "/payments")
                            .then().statusCode(200).extract().jsonPath().getList("publicId");
            assertFalse(payments.contains(paymentId));
        }
    }

    @Test
    void testDeleteMyAccountPayment_notFound() {
        final String clientKey = uniqueClientKey();
        final String accountId = this.createAdminAccessibleAccount(clientKey);

        forwardedAuthenticatedRequest(clientKey).when()
                .delete(ENDPOINT_URL + "/my/" + accountId + "/payments/non-existent").then().statusCode(404);
    }

    @Test
    void testMyAccountPayment_advancedAccessAndForbiddenChecks() {
        final String clientKey = uniqueClientKey();
        final String accountIdA = this.createAdminAccessibleAccount(clientKey);
        final String targetAccountIdA = this.createAdminAccessibleAccount(clientKey);
        final String accountIdB = this.createAdminAccessibleAccount(clientKey);

        final String paymentJson =
                createPaymentJson(targetAccountIdA, accountIdA, ADMIN_USER_ID, "10.00", "My payment", "2024-01-01");

        final String paymentId = forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON)
                .body(paymentJson).when().post(ENDPOINT_URL + "/my/" + accountIdA + "/payments").then().statusCode(201)
                .extract().path("publicId");

        // Try updating payment using an account it does not belong to
        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(paymentJson).when()
                .put(ENDPOINT_URL + "/my/" + accountIdB + "/payments/" + paymentId).then().statusCode(403);

        // Use a source/target account that admin does not have access to
        final String forbiddenPayload = createPaymentJson("01AN4Z07BY79KA1307SR9X4MV9", accountIdA, ADMIN_USER_ID,
                "10.00", "My payment", "2024-01-01");

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(forbiddenPayload).when()
                .put(ENDPOINT_URL + "/my/" + accountIdA + "/payments/" + paymentId).then().statusCode(403);

        forwardedAuthenticatedRequest(clientKey).contentType(ContentType.JSON).body(forbiddenPayload).when()
                .post(ENDPOINT_URL + "/my/" + accountIdA + "/payments").then().statusCode(403);
    }

    @Test
    void testAccountResource_Additional404AndAssociationChecks() {
        final String name = "Coverage Account " + TestUtil.generateUniqueSuffix();
        final String accountId = authenticatedJsonRequest().body(createAccountJson(name)).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");
        authenticatedRequest().when().get(ENDPOINT_URL + "/name/" + name).then().statusCode(200).body("name",
                equalTo(name));

        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999999");
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + accountId + "/users/999999").then().statusCode(404);
        authenticatedJsonRequest().body("{\"isNegative\":true}").when()
                .patch(ENDPOINT_URL + "/" + accountId + "/users/01AN4Z07BY79KA1307SR9X4MV7").then().statusCode(404);

        authenticatedRequest().when().get(ENDPOINT_URL + "/name/nonexistent-" + TestUtil.generateUniqueSuffix()).then()
                .statusCode(404);

        authenticatedRequest().when()
                .get(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7/account/" + accountId + "/check").then()
                .statusCode(200);
    }
}
