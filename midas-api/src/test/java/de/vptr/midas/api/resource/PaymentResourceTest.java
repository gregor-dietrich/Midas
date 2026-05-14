package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPaymentJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedPaymentJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.createPaymentJsonWithTestAccounts;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedSearchWithJson;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PaymentResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/payments";

    @Test
    void testGetAllUserPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserPayments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetPaymentById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPaymentById_authorizedWithExistingPayment() {
        final String id = authenticatedJsonRequest().body(createPaymentJsonWithTestAccounts()).when().post(ENDPOINT_URL)
                .then().statusCode(201).extract().path("publicId");

        authenticatedRequest().when().get(ENDPOINT_URL + "/" + id).then().statusCode(200);
    }

    @Test
    void testGetPaymentById_authorizedWithNonExistentPayment() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetPaymentsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPaymentsByUser_authorizedWithValidUser() {
        final String paymentJson = createPaymentJsonWithTestAccounts();
        final String userId = authenticatedJsonRequest().body(paymentJson).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("userPublicId");

        authenticatedRequest().when().get(ENDPOINT_URL + "/user/" + userId).then().statusCode(200);
    }

    @Test
    void testGetRecentPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/recent?limit=10");
    }

    @Test
    void testGetRecentPayments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/recent?limit=10");
    }

    @Test
    void testCreatePayment_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultPaymentJson());
    }

    @Test
    void testCreatePayment_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createPaymentJsonWithTestAccounts());
    }

    @Test
    void testCreatePayment_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createPaymentJsonWithTestAccounts());
    }

    @Test
    void testCreatePayment_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreatePayment_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreatePayment_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdatePayment_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createUpdatedPaymentJson());
    }

    @Test
    void testDeletePayment_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", "DELETE");
    }

    @Test
    void testGetPaymentsBySourceAccount_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/source/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPaymentsBySourceAccount_authorized() {
        final String paymentJson = createPaymentJsonWithTestAccounts();
        final String sourceId = authenticatedJsonRequest().body(paymentJson).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("sourceAccountPublicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/source/" + sourceId);
    }

    @Test
    void testGetPaymentsByTargetAccount_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/target/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPaymentsByTargetAccount_authorized() {
        final String paymentJson = createPaymentJsonWithTestAccounts();
        final String targetId = authenticatedJsonRequest().body(paymentJson).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("targetAccountPublicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/target/" + targetId);
    }

    @Test
    void testGetPaymentsByDateRange_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetPaymentsByDateRange_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetTotalAmountByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7/total");
    }

    @Test
    void testGetTotalAmountByUser_authorized() {
        final String paymentJson = createPaymentJsonWithTestAccounts();
        final String userId = authenticatedJsonRequest().body(paymentJson).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("userPublicId");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/" + userId + "/total");
    }

    @Test
    void testUpdatePayment_authorized() {
        final String id = createTestPayment();

        authenticatedJsonRequest().body(createPaymentJsonWithTestAccounts()).when().put(ENDPOINT_URL + "/" + id).then()
                .statusCode(200);
    }

    @Test
    void testPatchPayment_authorized() {
        final String id = createTestPayment();

        authenticatedJsonRequest().body(createPaymentJsonWithTestAccounts()).when().patch(ENDPOINT_URL + "/" + id)
                .then().statusCode(200);
    }

    @Test
    void testDeletePayment_authorized() {
        final String id = createTestPayment();

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    private String createTestPayment() {
        return authenticatedJsonRequest().body(createPaymentJsonWithTestAccounts()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");
    }

    @Test
    void testGetPaymentsByDateRange_missingParams() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/date-range").then().statusCode(400);
        authenticatedRequest().when().get(ENDPOINT_URL + "/date-range?startDate=2024-01-01").then().statusCode(400);
        authenticatedRequest().when().get(ENDPOINT_URL + "/date-range?endDate=2024-12-31").then().statusCode(400);
    }

    @Test
    void testSearchPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchPayments_authorized() {
        testAuthorizedSearchWithJson(ENDPOINT_URL + "/search");
    }

    @Test
    void testDeletePayment_nonexistent() {
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }
}
