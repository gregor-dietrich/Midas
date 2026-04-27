# GUI Test Utilities

This package contains utility classes for GUI service tests that help reduce code duplication and provide consistent testing patterns.

## Overview of Utilities

### TestUtil

Basic utility for generating unique test data and common assertions.

**Key Features:**

- `generateUniqueTestSuffix()` - Creates unique identifiers for test data
- `createUniqueTestUsername()`, `createUniqueTestEmail()`, `createUniqueTestName()` - Generate unique test strings with prefixes
- `assertServiceNotNull()` - Assert that a service is not null
- `TEST_AUTH_HEADER` - Standard authentication header constant

### MockDataUtil

Creates mock data objects with default values for testing.

**Key Features:**

- `TestConstants` - Common test constants (IDs, names, etc.)
- Factory methods for DTOs: `createUserDto()`, `createUserResponseDto()`, `createAccountDto()`, `createPostDto()`, `createPostResponseDto()`, `createPaymentResponseDto()`, `createPageDto()`, `createUserGroupDto()`, `createUserRankResponseDto()`, `createUserRankDto()`, `createPostCategoryDto()`, `createPostCommentDto()`, `createPostCommentResponseDto()`, `createPageResponseDto()`, `createPostCategoryResponseDto()`
- Support for both default and parameterized object creation

### TestDataBuilder

Creates unique test data objects using the TestUtil for uniqueness.

**Key Features:**

- `createUniqueUser()`, `createUniqueAccount()`, `createUniqueUserGroup()`, `createUniqueUserRank()`, `createUniquePost()`, `createUniquePostComment()`, `createUniquePayment()`, `createUniquePage()`, `createUniquePostCategory()` - Unique test objects
- `createUser()`, `createAccountResponse()` - Factory methods with specific parameters
- Uses TestUtil methods internally for consistent unique generation

### ServiceTestUtil

Common patterns for service testing including authentication, response handling, and error scenarios.

**Key Features:**

- `setupAuthHeader()` - Configure authentication mocks with default or custom header
- `testReturnEmptyListWhenNotAuthenticated()` - Test unauthenticated scenarios for list methods
- `testReturnEmptyWhenNotAuthenticated()` - Test unauthenticated scenarios for optional methods
- `testReturnEmptyOn404()` - Test not found scenarios
- `testServiceExceptionOnBadResponse()` - Test error handling for bad responses
- `testAuthenticationRequired()` - Test authentication requirement scenarios
- `testProcessingExceptionHandling()` - Test connection failure handling
- `test401ErrorHandling()` - Test 401 error handling with logout verification
- `testSuccessfulServiceCall()` - Test successful service calls with verification
- Factory methods for creating mock responses and exceptions

### ExceptionTestUtil

Utility for testing exception scenarios with predefined assertions.

**Key Features:**

- `assertThrowsAuthenticationRequired()` - Test authentication exceptions
- `assertThrowsBackendConnectionFailed()` - Test connection failure handling
- `assertHandles401WithLogout()` - Test 401 error handling with logout verification
- `assertThrowsNotAuthenticated()`, `assertThrowsUserNotAuthenticated()` - Various authentication exception assertions
- `assertThrowsAuthenticationException()`, `assertThrowsServiceException()` - Generic exception assertions with custom messages
- `assertThrowsSessionExpired()`, `assertThrowsUnexpectedError()`, `assertThrowsUnexpectedErrorOccurred()` - Specific error scenario assertions
- `assertThrowsBackendError()` - Test backend error with specific status
- Exception factory methods: `createProcessingException()`, `create401Exception()`, `createWebApplicationException()`

### ResponseTestUtil

Creates mock Response objects for testing HTTP interactions.

**Key Features:**

- `createOkResponse()`, `createCreatedResponse()`, `createNotFoundResponse()`, `createBadRequestResponse()`, `createUnauthorizedResponse()`, `createServerErrorResponse()`, `createInternalServerErrorResponse()`, `createNoContentResponse()` - Status-specific response creators
- `createMockResponse()` with custom status and optional entity
- Consistent Response object creation for tests

### BaseServiceTest (Abstract)

Base class that service tests can extend to inherit common mock setup and utility methods.

**Key Features:**

- Pre-configured `@Mock AuthService`
- Convenience methods that delegate to other utilities: `setupAuthHeader()`, `testAuthenticationRequired()`, `testProcessingExceptionHandling()`, `test401ErrorHandling()`, `testReturnEmptyWhenNotAuthenticated()`, `testReturnEmptyOn404()`, `testServiceExceptionOnBadResponse()`
- Reduces boilerplate in individual test classes

### VaadinTestUtil

Placeholder utility for Vaadin-specific testing patterns (currently empty).

### ComponentTestUtil

Placeholder utility for component testing patterns (currently empty).

## Usage Examples

### Before Refactoring

```java
@Test
void getAllUsers_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
    // When & Then
    assertThatThrownBy(() -> this.userService.getAllUsers(null))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Authentication required");
}

@Test
void getAllUsers_shouldThrowServiceException_whenProcessingExceptionOccurs() {
    // Given
    final String authHeader = "Basic dGVzdDp0ZXN0";
    when(this.userClient.getAllUsers(authHeader))
            .thenThrow(new ProcessingException("Connection failed"));

    // When & Then
    assertThatThrownBy(() -> this.userService.getAllUsers(authHeader))
            .isInstanceOf(ServiceException.class)
            .hasMessage("Backend connection failed");
}
```

### After Refactoring

```java
@Test
void getAllUsers_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
    // When & Then
    ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.getAllUsers(null));
}

@Test
void getAllUsers_shouldThrowServiceException_whenProcessingExceptionOccurs() {
    // Given
    final String authHeader = TestUtil.TEST_AUTH_HEADER;
    when(this.userClient.getAllUsers(authHeader))
            .thenThrow(ExceptionTestUtil.createProcessingException());

    // When & Then
    ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.userService.getAllUsers(authHeader));
}
```

### Creating Test Data

**Before:**

```java
final var user = new UserResponseDto();
user.id = 1L;
user.username = "testuser";
user.email = "test@example.com";
user.rankId = 1L;
user.rankName = "User";
user.activated = true;
user.banned = false;
user.created = LocalDateTime.now();
```

**After:**

```java
final var user = MockDataUtil.createUserResponseDto(1L, "testuser", "test@example.com");
// or for unique data:
final var uniqueUser = TestDataBuilder.createUniqueUser();
```

### Response Mocking

**Before:**

```java
final var mockResponse = Response.status(200).entity(expectedUser).build();
```

**After:**

```java
final var mockResponse = ResponseTestUtil.createOkResponse(expectedUser);
```

## Benefits

1. **Reduced Code Duplication**: Common patterns are extracted into reusable utilities
2. **Consistency**: All tests use the same patterns for similar scenarios
3. **Maintainability**: Changes to test patterns only need to be made in one place
4. **Readability**: Test intentions are clearer with descriptive utility method names
5. **Type Safety**: Utilities provide compile-time safety for common test scenarios

## Migration Guide

To migrate existing tests:

1. **Replace manual DTO creation** with `MockDataUtil` or `TestDataBuilder` methods
2. **Replace exception assertions** with `ExceptionTestUtil` methods
3. **Replace Response creation** with `ResponseTestUtil` methods
4. **Replace common test patterns** with `ServiceTestUtil` methods
5. **Consider extending `BaseServiceTest`** for new test classes
6. **Use `TestUtil.TEST_AUTH_HEADER`** instead of hardcoded auth headers

## Testing the Utilities

The utilities themselves should be tested to ensure they work correctly. The refactored service tests serve as integration tests that verify the utilities work in real scenarios.
