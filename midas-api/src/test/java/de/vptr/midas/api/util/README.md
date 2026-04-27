# API Test Utilities

This package contains utility classes for API service and resource tests that help reduce code duplication and provide consistent testing patterns for REST API endpoints.

## Overview of Utilities

### TestUtil

Core utility for REST-assured testing with authentication, endpoint testing patterns, and unique data generation.

**Key Features:**

- **Authentication**: `authenticatedRequest()`, `authenticatedJsonRequest()` - Pre-configured requests with admin credentials
- **Direct Access**: `given()` - Direct access to REST-assured for custom scenarios  
- **Authorization Testing**: `testUnauthorizedAccess()`, `testUnauthorizedPost()`, `testUnauthorizedPut()`, `testUnauthorizedDelete()`, `testUnauthorizedPatch()` - Test unauthorized access scenarios
- **Authorized Testing**: `testAuthorizedGetWithJson()`, `testAuthorizedGetWithOptionalResource()`, `testAuthorizedPostWithRoleCheck()`, `testAuthorizedPutWithJson()`, `testAuthorizedPatchWithJson()`, `testAuthorizedPostWithCreation()` - Test authorized scenarios with various expectations
- **Resource Management**: `createTestCategory()`, `createTestPost()`, `createTestComment()` - Create test resources and return their IDs
- **Unique Data Generation**: `generateUniqueSuffix()`, `generateUniqueEmail()`, `generateUniqueUsername()` - Generate unique test data
- **Constants**: `ADMIN_USERNAME`, `ADMIN_PASSWORD` - Default admin credentials for testing

### TestDataBuilder

Factory utility for creating JSON strings for REST API test requests.

**Key Features:**

- **User JSON**: `createUserJson()`, `createUniqueUserJson()`, `createUserUpdateJson()`, `createDefaultUserUpdateJson()` - User data for API requests
- **Post JSON**: `createPostJson()`, `createDefaultPostJson()`, `createUpdatedPostJson()`, `createPostPatchJson()`, `createDefaultPostPatchJson()` - Post data with full and partial updates
- **Account JSON**: `createAccountJson()`, `createDefaultAccountJson()`, `createUniqueAccountJson()` - Account data
- **Category JSON**: `createPostCategoryJson()`, `createDefaultPostCategoryJson()`, `createUpdatedPostCategoryJson()` - Post category data
- **Comment JSON**: `createPostCommentJson()`, `createDefaultPostCommentJson()`, `createUpdatedPostCommentJson()`, `createPatchedPostCommentJson()` - Post comment data
- **Group JSON**: `createUserGroupJson()`, `createDefaultUserGroupJson()`, `createUpdatedUserGroupJson()` - User group data
- **Rank JSON**: `createUserRankJson()`, `createDefaultUserRankJson()`, `createUpdatedUserRankJson()` - User rank data with permission flags
- **Page JSON**: `createPageJson()`, `createDefaultPageJson()`, `createUpdatedPageJson()` - Page data
- **Payment JSON**: `createPaymentJson()`, `createDefaultPaymentJson()`, `createUpdatedPaymentJson()` - Payment data

### ServiceTestDataBuilder

Factory utility for creating DTOs and entities for service layer testing.

**Key Features:**

- **Unique DTOs**: `createUniqueUserDto()`, `createUniquePostDto()`, `createUniqueUserRankDto()`, `createUniqueUserGroupDto()`, `createUniquePageDto()` - Create unique DTOs with generated suffixes
- **Unique Entities**: `createUniqueAccountEntity()`, `createUniquePostCategoryEntity()`, `createUniquePageEntity()`, `createUniquePostCategoryDto()` - Create unique entities for persistence testing
- **Update DTOs**: `createUserUpdateDto()`, `createUserGroupUpdateDto()`, `createPaymentUpdateDto()`, `createPostUpdateDto()`, `createUserRankUpdateDto()` - DTOs for update operations
- **Complete Objects**: `createPaymentDto()`, `createPostCommentEntity()`, `createUserRankDto()`, `createPostDto()`, `createPageDto()` - Fully populated objects with all fields
- **Custom Parameterization**: Most methods accept parameters for customized object creation

### ServiceTestUtil

Utility for common service testing patterns and setup operations.

**Key Features:**

- **Service Validation**: `assertServiceNotNull()` - Assert service instances are properly injected
- **Unique Data Generation**: `generateUniqueTestSuffix()`, `createUniqueTestUsername()`, `createUniqueTestEmail()`, `createUniqueTestName()` - Generate unique test data with prefixes
- **Test Setup**: `setupTestUser()`, `setupTestCategory()` - Set up test entities in the database
- **Cleanup**: `cleanupTestData()` - Clean up test data for specific entity types
- **Delegation**: Delegates to `TestUtil` for suffix generation

### MockUtil

Simple utility providing common test constants and mock values.

**Key Features:**

- **TestIds**: Static constants for common test IDs (`USER_ID`, `ACCOUNT_ID`, `POST_ID`, etc.)
- **Mock Factories**: `createMockId()`, `createMockUsername()`, `createMockEmail()` - Simple mock value factories
- **Default Credentials**: `USERNAME`, `PASSWORD`, `TEST_EMAIL` - Default test credentials

## Usage Examples

### REST API Testing

**Before:**

```java
@Test
void testGetAllUsers() {
    given()
        .auth().basic("admin", "admin")
        .when()
        .get("/api/v1/users")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
}

@Test
void testUnauthorizedAccess() {
    given()
        .when()
        .get("/api/v1/users")
        .then()
        .statusCode(401);
}
```

**After:**

```java
@Test
void testGetAllUsers() {
    TestUtil.testAuthorizedGetWithJson("/api/v1/users");
}

@Test
void testUnauthorizedAccess() {
    TestUtil.testUnauthorizedAccess("/api/v1/users");
}
```

### Creating Test Data

**Before:**

```java
@Test
void testCreateUser() {
    String userJson = String.format("""
        {
            "username": "testuser_%d",
            "email": "test_%d@example.com", 
            "password": "password123"
        }
        """, System.currentTimeMillis(), System.currentTimeMillis());
        
    given()
        .auth().basic("admin", "admin")
        .contentType(ContentType.JSON)
        .body(userJson)
        .when()
        .post("/api/v1/users")
        .then()
        .statusCode(201);
}
```

**After:**

```java
@Test  
void testCreateUser() {
    String userJson = TestDataBuilder.createUniqueUserJson();
    TestUtil.testAuthorizedPostWithCreation("/api/v1/users", userJson);
}
```

### Service Layer Testing

**Before:**

```java
@Test
void testUserService() {
    UserDto userDto = new UserDto();
    userDto.username = "testuser_" + System.currentTimeMillis();
    userDto.email = "test_" + System.currentTimeMillis() + "@example.com";
    userDto.password = "password123";
    
    UserDto result = userService.createUser(userDto);
    assertNotNull(result);
}
```

**After:**

```java
@Test
void testUserService() {
    UserDto userDto = ServiceTestDataBuilder.createUniqueUserDto();
    UserDto result = userService.createUser(userDto);
    
    ServiceTestUtil.assertServiceNotNull(result);
}
```

### Resource Setup and Testing

**Before:**

```java
@Test
void testCreateComment() {
    // Create category first
    String categoryJson = """{"name": "Test Category", "description": "Test"}""";
    Integer categoryId = given()
        .auth().basic("admin", "admin")
        .contentType(ContentType.JSON)
        .body(categoryJson)
        .post("/api/v1/categories")
        .then()
        .statusCode(201)
        .extract().path("id");
    
    // Create post next  
    String postJson = String.format("""
        {
            "title": "Test Post",
            "content": "Test content", 
            "published": true,
            "commentable": true,
            "userId": 1,
            "categoryId": %d
        }""", categoryId);
        
    Integer postId = given()
        .auth().basic("admin", "admin")
        .contentType(ContentType.JSON)
        .body(postJson)
        .post("/api/v1/posts")
        .then()
        .statusCode(201)
        .extract().path("id");
    
    // Finally test comment creation
    String commentJson = String.format("""
        {
            "content": "Test comment",
            "postId": %d
        }""", postId);
        
    TestUtil.testAuthorizedPostWithCreation("/api/v1/comments", commentJson);
}
```

**After:**

```java
@Test
void testCreateComment() {
    Long postId = TestUtil.createTestPost();
    String commentJson = TestDataBuilder.createDefaultPostCommentJson(postId);
    TestUtil.testAuthorizedPostWithCreation("/api/v1/comments", commentJson);
}
```

## Benefits

1. **Reduced Boilerplate**: Eliminates repetitive REST-assured setup code
2. **Consistent Testing**: Standardized patterns for common API testing scenarios  
3. **Authentication Management**: Centralized handling of test authentication
4. **Unique Data Generation**: Prevents test conflicts with unique identifiers
5. **Resource Management**: Simplified creation and cleanup of test resources
6. **Maintainability**: Changes to test patterns only need to be made in one place
7. **Type Safety**: Compile-time safety for common test scenarios
8. **Readability**: Test intentions are clearer with descriptive utility method names

## Migration Guide

To migrate existing API tests:

1. **Replace REST-assured setup** with `TestUtil.authenticatedRequest()` or `TestUtil.authenticatedJsonRequest()`
2. **Replace manual JSON creation** with `TestDataBuilder` methods  
3. **Replace common authorization testing** with `TestUtil.testUnauthorizedAccess()` and authorized variants
4. **Replace resource creation code** with `TestUtil.createTestCategory()`, `createTestPost()`, etc.
5. **Use unique data generators** instead of manual timestamp-based uniqueness
6. **Replace service DTOs** with `ServiceTestDataBuilder` methods
7. **Use `MockUtil.TestIds`** for consistent test constants

## Framework Integration

These utilities are designed to work with:

- **REST-assured** for API endpoint testing
- **Quarkus Test** for service layer testing  
- **JUnit 5** for test structure and assertions
- **Panache/Hibernate** entities for database operations

## Testing the Utilities

The utilities themselves should be tested to ensure they work correctly. The refactored API tests serve as integration tests that verify the utilities work in real scenarios.
