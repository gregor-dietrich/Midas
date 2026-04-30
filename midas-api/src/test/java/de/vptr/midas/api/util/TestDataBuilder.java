package de.vptr.midas.api.util;

/**
 * Builder utility for creating test data JSON strings
 */
public class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Creates a basic user JSON for testing
     */
    public static String createUserJson(final String username, final String email, final String password) {
        return String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
    }

    /**
     * Creates a unique user JSON for testing (avoids conflicts)
     */
    public static String createUniqueUserJson() {
        final var suffix = TestUtil.generateUniqueSuffix();
        return createUserJson(
                "testuser_" + suffix,
                "test_" + suffix + "@example.com",
                "password123");
    }

    /**
     * Creates a basic post JSON for testing
     */
    public static String createPostJson(final String title, final String content, final boolean published,
            final Long userId) {
        return String.format("""
                {
                    "title": "%s",
                    "content": "%s",
                    "published": %s,
                    "userId": %d
                }
                """, title, content, published, userId);
    }

    /**
     * Creates a complete post JSON with all fields for testing
     */
    public static String createPostJson(final String title, final String content, final boolean published,
            final boolean commentable, final Long userId, final Long categoryId) {
        return String.format("""
                {
                    "title": "%s",
                    "content": "%s",
                    "published": %b,
                    "commentable": %b,
                    "userId": %d,
                    "categoryId": %d
                }
                """, title, content, published, commentable, userId, categoryId);
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
        return String.format("""
                {
                    "name": "%s"
                }
                """, name);
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
        return String.format("""
                {
                    "description": "%s",
                    "amount": %.2f,
                    "currency": "%s"
                }
                """, description, amount, currency);
    }

    /**
     * Creates a post category JSON for testing
     */
    public static String createPostCategoryJson(final String name, final String description) {
        return String.format("""
                {
                    "name": "%s",
                    "description": "%s"
                }
                """, name, description);
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
        return String.format("""
                {
                    "name": "%s"
                }
                """, name);
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
            final boolean pageDelete,
            final boolean postAdd, final boolean postEdit, final boolean postDelete,
            final boolean postCategoryAdd, final boolean postCategoryEdit, final boolean postCategoryDelete,
            final boolean postCommentAdd, final boolean postCommentEdit, final boolean postCommentDelete,
            final boolean userAdd, final boolean userEdit, final boolean userDelete,
            final boolean userGroupAdd, final boolean userGroupEdit, final boolean userGroupDelete,
            final boolean userRankAdd, final boolean userRankEdit, final boolean userRankDelete,
            final boolean userAccountAdd, final boolean userAccountEdit, final boolean userAccountDelete) {
        return String.format("""
                {
                    "name": "%s",
                    "pageAdd": %b,
                    "pageEdit": %b,
                    "pageDelete": %b,
                    "postAdd": %b,
                    "postEdit": %b,
                    "postDelete": %b,
                    "postCategoryAdd": %b,
                    "postCategoryEdit": %b,
                    "postCategoryDelete": %b,
                    "postCommentAdd": %b,
                    "postCommentEdit": %b,
                    "postCommentDelete": %b,
                    "userAdd": %b,
                    "userEdit": %b,
                    "userDelete": %b,
                    "userGroupAdd": %b,
                    "userGroupEdit": %b,
                    "userGroupDelete": %b,
                    "userRankAdd": %b,
                    "userRankEdit": %b,
                    "userRankDelete": %b,
                    "userAccountAdd": %b,
                    "userAccountEdit": %b,
                    "userAccountDelete": %b
                }
                """, name, pageAdd, pageEdit, pageDelete, postAdd, postEdit, postDelete,
                postCategoryAdd, postCategoryEdit, postCategoryDelete,
                postCommentAdd, postCommentEdit, postCommentDelete,
                userAdd, userEdit, userDelete,
                userGroupAdd, userGroupEdit, userGroupDelete,
                userRankAdd, userRankEdit, userRankDelete,
                userAccountAdd, userAccountEdit, userAccountDelete);
    }

    // PostComment builders
    public static String createPostCommentJson(final String content, final Long postId) {
        return String.format("""
                {
                    "content": "%s",
                    "postId": %d
                }
                """, content, postId);
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
    public static String createPageJson(final String title, final String content, final boolean published) {
        return String.format("""
                {
                    "title": "%s",
                    "content": "%s",
                    "published": %b
                }
                """, title, content, published);
    }

    public static String createDefaultPageJson() {
        return createPageJson("Test Page", "Test content", false);
    }

    public static String createUpdatedPageJson() {
        return createPageJson("Updated Page", "Updated content", true);
    }

    // Payment builders
    public static String createPaymentJson(final Long targetAccountId, final Long sourceAccountId, final Long userId,
            final String amount, final String comment, final String date) {
        return String.format("""
                {
                    "targetAccountId": %d,
                    "sourceAccountId": %d,
                    "userId": %d,
                    "amount": %s,
                    "comment": "%s",
                    "date": "%s"
                }
                """, targetAccountId, sourceAccountId, userId, amount, comment, date);
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
        return createUserRankJson("Test Rank", true, true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, true, true);
    }

    /**
     * Creates an updated user rank JSON for testing updates
     */
    public static String createUpdatedUserRankJson() {
        return createUserRankJson("Updated Test Rank", false, false, false, false, false, false,
                false, false, false, false, false, false,
                false, false, false, false, false, false,
                false, false, false, false, false, false);
    }

    /**
     * Creates a user-account meta JSON for testing the PATCH meta endpoint.
     *
     * @param isNegative the isNegative flag value
     * @return JSON string
     */
    public static String createUserAccountMetaJson(final boolean isNegative) {
        return String.format("""
                {
                    "isNegative": %b
                }
                """, isNegative);
    }

    /**
     * Creates an updated user update JSON (without password)
     */
    public static String createUserUpdateJson(final String username, final String email) {
        return String.format("""
                {
                    "username": "%s",
                    "email": "%s"
                }
                """, username, email);
    }

    /**
     * Creates a post patch JSON for testing partial updates
     */
    public static String createPostPatchJson(final String title) {
        return String.format("""
                {
                    "title": "%s"
                }
                """, title);
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
