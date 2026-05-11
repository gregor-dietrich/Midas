package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPaymentJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedPaymentJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.createPaymentJsonWithTestAccounts;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

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
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetPaymentById_authorizedWithExistingPayment() {
        final Integer id =
                authenticatedRequest().contentType(ContentType.JSON).body(createPaymentJsonWithTestAccounts()).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        authenticatedRequest().when().get(ENDPOINT_URL + "/" + id).then().statusCode(200).contentType(ContentType.JSON);
    }

    @Test
    void testGetPaymentById_authorizedWithNonExistentPayment() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetPaymentsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/1");
    }

    @Test
    void testGetPaymentsByUser_authorizedWithValidUser() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/user/1").then().statusCode(200)
                .contentType(ContentType.JSON);
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
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUpdatedPaymentJson());
    }

    @Test
    void testDeletePayment_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1", "DELETE");
    }

    @Test
    void testGetPaymentsBySourceAccount_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/source/1");
    }

    @Test
    void testGetPaymentsBySourceAccount_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/source/1");
    }

    @Test
    void testGetPaymentsByTargetAccount_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/target/1");
    }

    @Test
    void testGetPaymentsByTargetAccount_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/target/1");
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
        testUnauthorizedAccess(ENDPOINT_URL + "/user/1/total");
    }

    @Test
    void testGetTotalAmountByUser_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/1/total");
    }

    @Test
    void testUpdatePayment_authorized() {
        final Integer id =
                authenticatedRequest().contentType(ContentType.JSON).body(createPaymentJsonWithTestAccounts()).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        authenticatedRequest().contentType(ContentType.JSON).body(createUpdatedPaymentJson()).when()
                .put(ENDPOINT_URL + "/" + id).then().statusCode(200);
    }

    @Test
    void testPatchPayment_authorized() {
        final Integer id =
                authenticatedRequest().contentType(ContentType.JSON).body(createPaymentJsonWithTestAccounts()).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        authenticatedRequest().contentType(ContentType.JSON).body(createDefaultPaymentJson()).when()
                .patch(ENDPOINT_URL + "/" + id).then().statusCode(200);
    }

    @Test
    void testDeletePayment_authorized() {
        final Integer id =
                authenticatedRequest().contentType(ContentType.JSON).body(createPaymentJsonWithTestAccounts()).when()
                        .post(ENDPOINT_URL).then().statusCode(201).extract().path("id");

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    @Test
    void testSearchPayments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchPayments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/search?query=test");
    }
}
