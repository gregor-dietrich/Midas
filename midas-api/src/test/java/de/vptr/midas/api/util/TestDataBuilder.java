package de.vptr.midas.api.util;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builder utility for creating test data JSON strings
 */
public class TestDataBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Creates a basic user JSON for testing
     */
    public static String createUserJson(final String username, final String email, final String password) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("username", username);
            data.put("email", email);
            data.put("password", password);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user test data", e);
        }
    }

    /**
     * Creates a unique user JSON for testing (avoids conflicts)
     */
    public static String createUniqueUserJson() {
        final var suffix = TestUtil.generateUniqueSuffix();
        return createUserJson("testuser_" + suffix, "test_" + suffix + "@example.com", "password123");
    }

    /**
     * Creates a basic post JSON for testing
     */
    public static String createPostJson(final String title, final String content, final boolean published,
            final Long userId) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("title", title);
            data.put("content", content);
            data.put("published", published);
            data.put("userId", userId);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize post test data", e);
        }
    }

    /**
     * Creates a complete post JSON with all fields for testing
     */
    public static String createPostJson(final String title, final String content, final boolean published,
            final boolean commentable, final Long userId, final Long categoryId) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("title", title);
            data.put("content", content);
            data.put("published", published);
            data.put("commentable", commentable);
            data.put("userId", userId);
            data.put("categoryId", categoryId);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize post test data", e);
        }
    }

    /**
     * Creates a default test post JSON
     */
    public static String createDefaultPostJson() {
        return createPostJson("Test Post", "Test content", false, 1L);
    }

    /**
     * Creates an updated post JSON for testing updates
     */
    public static String createUpdatedPostJson() {
        return createPostJson("Updated Post", "Updated content", true, 1L);
    }

    /**
     * Creates a basic account JSON for testing
     */
    public static String createAccountJson(final String name) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("name", name);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize account test data", e);
        }
    }

    /**
     * Creates a default test account JSON
     */
    public static String createDefaultAccountJson() {
        return createAccountJson("Test Account");
    }

    /**
     * Creates a unique account JSON for testing
     */
    public static String createUniqueAccountJson() {
        return createAccountJson("Test Account " + TestUtil.generateUniqueSuffix());
    }

    /**
     * Creates a payment JSON for testing
     */
    public static String createPaymentJson(final String description, final double amount, final String currency) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("description", description);
            data.put("amount", amount);
            data.put("currency", currency);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment test data", e);
        }
    }

    /**
     * Creates a post category JSON for testing
     */
    public static String createPostCategoryJson(final String name, final String description) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("name", name);
            data.put("description", description);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize post category test data", e);
        }
    }

    /**
     * Creates a default test post category JSON
     */
    public static String createDefaultPostCategoryJson() {
        return createPostCategoryJson("Test Category", "Test category description");
    }

    /**
     * Creates an updated post category JSON for testing updates
     */
    public static String createUpdatedPostCategoryJson() {
        return createPostCategoryJson("Updated Category", "Updated description");
    }

    /**
     * Creates a user group JSON for testing
     */
    public static String createUserGroupJson(final String name) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("name", name);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user group test data", e);
        }
    }

    /**
     * Creates a default test user group JSON
     */
    public static String createDefaultUserGroupJson() {
        return createUserGroupJson("Test Group");
    }

    /**
     * Creates an updated user group JSON for testing updates
     */
    public static String createUpdatedUserGroupJson() {
        return createUserGroupJson("Updated Group");
    }

    /**
     * Creates an invalid JSON for testing
     */
    public static String createEmptyJson() {
        return "{}";
    }

    /**
     * Creates an invalid JSON for testing
     */
    public static String createInvalidJson() {
        return """
                {
                    "id": "1",
                    "_non_existing_attribute_28520528u=280*20": ""
                }
                """;
    }

    /**
     * Creates a malformed JSON for testing
     */
    public static String createMalformedJson() {
        return """
                {
                    name: "Test
                    "description": "Test",
                }
                """;
    }

    /**
     * Creates a user rank JSON for testing
     */
    // UserRank builders
    public static String createUserRankJson(final String name, final boolean pageAdd, final boolean pageEdit,
            final boolean pageDelete, final boolean postAdd, final boolean postEdit, final boolean postDelete,
            final boolean postCategoryAdd, final boolean postCategoryEdit, final boolean postCategoryDelete,
            final boolean postCommentAdd, final boolean postCommentEdit, final boolean postCommentDelete,
            final boolean userAdd, final boolean userEdit, final boolean userDelete, final boolean userGroupAdd,
            final boolean userGroupEdit, final boolean userGroupDelete, final boolean userRankAdd,
            final boolean userRankEdit, final boolean userRankDelete, final boolean userAccountAdd,
            final boolean userAccountEdit, final boolean userAccountDelete) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("name", name);
            data.put("pageAdd", pageAdd);
            data.put("pageEdit", pageEdit);
            data.put("pageDelete", pageDelete);
            data.put("postAdd", postAdd);
            data.put("postEdit", postEdit);
            data.put("postDelete", postDelete);
            data.put("postCategoryAdd", postCategoryAdd);
            data.put("postCategoryEdit", postCategoryEdit);
            data.put("postCategoryDelete", postCategoryDelete);
            data.put("postCommentAdd", postCommentAdd);
            data.put("postCommentEdit", postCommentEdit);
            data.put("postCommentDelete", postCommentDelete);
            data.put("userAdd", userAdd);
            data.put("userEdit", userEdit);
            data.put("userDelete", userDelete);
            data.put("userGroupAdd", userGroupAdd);
            data.put("userGroupEdit", userGroupEdit);
            data.put("userGroupDelete", userGroupDelete);
            data.put("userRankAdd", userRankAdd);
            data.put("userRankEdit", userRankEdit);
            data.put("userRankDelete", userRankDelete);
            data.put("userAccountAdd", userAccountAdd);
            data.put("userAccountEdit", userAccountEdit);
            data.put("userAccountDelete", userAccountDelete);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user rank test data", e);
        }
    }

    // PostComment builders
    public static String createPostCommentJson(final String content, final Long postId) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("content", content);
            data.put("postId", postId);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize post comment test data", e);
        }
    }

    public static String createDefaultPostCommentJson(final Long postId) {
        return createPostCommentJson("Test comment", postId);
    }

    public static String createUpdatedPostCommentJson() {
        return """
                {
                    "content": "Updated comment content"
                }
                """;
    }

    public static String createPatchedPostCommentJson() {
        return """
                {
                    "content": "Patched comment content"
                }
                """;
    }

    // Page builders
    public static String createPageJson(final String title, final String content) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("title", title);
            data.put("content", content);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize page test data", e);
        }
    }

    public static String createDefaultPageJson() {
        return createPageJson("Test Page", "Test content");
    }

    public static String createUpdatedPageJson() {
        return createPageJson("Updated Page", "Updated content");
    }

    // Payment builders
    public static String createPaymentJson(final Long targetAccountId, final Long sourceAccountId, final Long userId,
            final String amount, final String comment, final String date) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("targetAccountId", targetAccountId);
            data.put("sourceAccountId", sourceAccountId);
            data.put("userId", userId);
            data.put("amount", amount);
            data.put("comment", comment);
            data.put("date", date);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment test data", e);
        }
    }

    public static String createDefaultPaymentJson() {
        return createPaymentJson(1L, 2L, 1L, "100.00", "Test payment", "2024-01-01");
    }

    public static String createUpdatedPaymentJson() {
        return createPaymentJson(1L, 2L, 1L, "150.00", "Updated payment", "2024-01-01");
    }

    /**
     * Creates a default test user rank JSON
     */
    public static String createDefaultUserRankJson() {
        return createUserRankJson("Test Rank", true, true, true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true, true, true);
    }

    /**
     * Creates an updated user rank JSON for testing updates
     */
    public static String createUpdatedUserRankJson() {
        return createUserRankJson("Updated Test Rank", false, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, false, false, false, false,
                false);
    }

    /**
     * Creates a user-account meta JSON for testing the PATCH meta endpoint.
     *
     * @param isNegative
     *            the isNegative flag value
     * @return JSON string
     */
    public static String createUserAccountMetaJson(final boolean isNegative) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("isNegative", isNegative);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user account meta test data", e);
        }
    }

    /**
     * Creates an updated user update JSON (without password)
     */
    public static String createUserUpdateJson(final String username, final String email) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("username", username);
            data.put("email", email);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user update test data", e);
        }
    }

    /**
     * Creates a post patch JSON for testing partial updates
     */
    public static String createPostPatchJson(final String title) {
        try {
            final var data = new LinkedHashMap<String, Object>();
            data.put("title", title);
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize post patch test data", e);
        }
    }

    /**
     * Creates a default post patch JSON
     */
    public static String createDefaultPostPatchJson() {
        return createPostPatchJson("Patched Post");
    }

    /**
     * Creates a default user update JSON
     */
    public static String createDefaultUserUpdateJson() {
        return createUserUpdateJson("updateduser", "updated@example.com");
    }
}
