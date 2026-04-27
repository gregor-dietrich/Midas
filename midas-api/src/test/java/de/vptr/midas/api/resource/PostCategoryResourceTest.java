package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPostCategoryJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedPostCategoryJson;
import static de.vptr.midas.api.util.TestUtil.createTestCategory;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithExistingResourceAndJson;
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

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PostCategoryResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/categories";

    @Test
    void testGetAllPostCategories_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPostCategories_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetRootCategories_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/root");
    }

    @Test
    void testGetRootCategories_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/root");
    }

    @Test
    void testGetCategoryById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetCategoryById_authorizedWithExistingCategory() {
        Long id = createTestCategory();
        testAuthorizedGetWithExistingResourceAndJson(ENDPOINT_URL + "/" + id);
    }

    @Test
    void testGetCategoryById_authorizedWithNonExistentCategory() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetCategoriesByParent_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/parent/1");
    }

    @Test
    void testGetCategoriesByParent_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/parent/1");
    }

    @Test
    void testCreateCategory_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultPostCategoryJson());
    }

    @Test
    void testCreateCategory_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createDefaultPostCategoryJson());
    }

    @Test
    void testCreateCategory_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createDefaultPostCategoryJson());
    }

    @Test
    void testCreateCategory_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateCategory_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateCategory_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateCategory_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUpdatedPostCategoryJson());
    }

    @Test
    void testDeleteCategory_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/1");
    }
}
