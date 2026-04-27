package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.service.UserService;

/**
 * Utility class for service testing patterns
 */
public class ServiceTestUtil {

    private ServiceTestUtil() {
        // Utility class
    }

    /**
     * Tests that a service instance is not null
     */
    public static void assertServiceNotNull(final Object service) {
        assertNotNull(service);
    }

    /**
     * Generates a unique suffix for test data to avoid conflicts
     */
    public static String generateUniqueTestSuffix() {
        return TestUtil.generateUniqueSuffix();
    }

    /**
     * Creates a unique test username
     */
    public static String createUniqueTestUsername(final String prefix) {
        return prefix + "_" + generateUniqueTestSuffix();
    }

    /**
     * Creates a unique test email
     */
    public static String createUniqueTestEmail(final String prefix) {
        return prefix + "_" + generateUniqueTestSuffix() + "@example.com";
    }

    /**
     * Creates a unique test name (for entities that have a name field)
     */
    public static String createUniqueTestName(final String prefix) {
        return prefix + " " + generateUniqueTestSuffix();
    }

    /**
     * Sets up a test user and returns the entity
     */
    public static UserEntity setupTestUser(final UserService userService) {
        final var testUserDto = ServiceTestDataBuilder.createUniqueUserDto();
        final var createdUser = userService.createUser(testUserDto);
        return UserEntity.findById(createdUser.id);
    }

    /**
     * Sets up a test category and returns the entity
     */
    public static PostCategoryEntity setupTestCategory() {
        PostCategoryEntity testCategory = PostCategoryEntity.find("name", "Test Category").firstResult();
        if (testCategory == null) {
            testCategory = ServiceTestDataBuilder.createUniquePostCategoryEntity();
            testCategory.persist();
        }
        return testCategory;
    }

    /**
     * Cleans up test data for a specific entity type
     */
    public static void cleanupTestData(final Class<?> entityClass) {
        try {
            final var deleteAllMethod = entityClass.getMethod("deleteAll");
            deleteAllMethod.invoke(null);
        } catch (final Exception e) {
            // If deleteAll is not available, ignore
        }
    }
}
