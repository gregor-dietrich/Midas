package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createAccountJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueUserJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CoverageExpansionTest {

    @Test
    void testAccountResource_AdditionalCoverage() {
        final String endpoint = "/api/v1/accounts";

        // 404 cases
        testAuthorizedGetWithNonExistentResource(endpoint + "/999999");
        authenticatedRequest().when().delete(endpoint + "/999999").then().statusCode(404);
        authenticatedRequest().when().delete(endpoint + "/1/users/999999").then().statusCode(404);

        // getAccountByName positive case
        final String name = "Coverage Account " + TestUtil.generateUniqueSuffix();
        authenticatedJsonRequest().body(createAccountJson(name)).when().post(endpoint).then().statusCode(201);
        authenticatedRequest().when().get(endpoint + "/name/" + name).then().statusCode(200).body("name",
                equalTo(name));

        // getAccountByName 404
        authenticatedRequest().when().get(endpoint + "/name/nonexistent-" + TestUtil.generateUniqueSuffix()).then()
                .statusCode(404);

        // getOutgoing/IncomingPayments (admin)
        authenticatedRequest().when().get(endpoint + "/1/payments/outgoing").then().statusCode(200);
        authenticatedRequest().when().get(endpoint + "/1/payments/incoming").then().statusCode(200);

        // checkUserAccountAssociation
        authenticatedRequest().when().get(endpoint + "/user/1/account/1/check").then().statusCode(200);

        // getMyAccountsWithBalances
        authenticatedRequest().when().get(endpoint + "/my/with-balances").then().statusCode(200);
    }

    @Test
    void testUserResource_AdditionalCoverage() {
        final String endpoint = "/api/v1/users";

        // 404 cases
        authenticatedRequest().when().get(endpoint + "/999999").then().statusCode(404);
        authenticatedRequest().when().get(endpoint + "/username/nonexistent-" + TestUtil.generateUniqueSuffix()).then()
                .statusCode(404);
        authenticatedRequest().when()
                .get(endpoint + "/email?email=nonexistent-" + TestUtil.generateUniqueSuffix() + "@example.com").then()
                .statusCode(404);
        authenticatedRequest().when().delete(endpoint + "/999999").then().statusCode(404);
        authenticatedRequest().contentType("application/json").body(createUniqueUserJson()).when()
                .put(endpoint + "/999999").then().statusCode(404);
        authenticatedRequest().contentType("application/json").body("{\"username\":\"new\"}").when()
                .patch(endpoint + "/999999").then().statusCode(404);

        // createUser with rankId
        authenticatedJsonRequest().body(createUniqueUserJson()).when().post(endpoint).then().statusCode(201);
    }

    @Test
    void testOtherResources_404Coverage() {
        authenticatedRequest().when().get("/api/v1/categories/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/categories/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/comments/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/comments/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/pages/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/pages/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/posts/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/posts/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/user-groups/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/user-groups/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/user-groups/1/users/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/user-ranks/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/user-ranks/999999").then().statusCode(404);

        authenticatedRequest().when().get("/api/v1/payments/999999").then().statusCode(404);
        authenticatedRequest().when().delete("/api/v1/payments/999999").then().statusCode(404);
    }
}
