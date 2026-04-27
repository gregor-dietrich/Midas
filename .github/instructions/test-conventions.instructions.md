---
description: "Use when writing, modifying, or reviewing tests. Covers test structure, naming, mocking strategy, and test utilities for the Midas GUI project."
applyTo: "src/test/**"
---

# Test Conventions — Midas GUI

## Structure

- Test files mirror source packages: `service/PostServiceTest.java`, `view/MainLayoutTest.java`, `component/dialog/AdminUserDialogTest.java`
- JUnit 5 + Mockito + AssertJ — no Quarkus test container needed (services are unit-tested in isolation)
- Three main test categories:
  - **Service tests** (`service/`): Unit tests with Mockito mocks for REST clients and auth
  - **View tests** (`view/`): Unit tests with reflection setup for Vaadin views
  - **Component tests** (`component/`): Unit tests for dialogs, layouts, and button components

## Naming

- Test class: `<ClassName>Test` (e.g., `PostServiceTest`, `MainLayoutTest`)
- Test method: `methodName_shouldExpectedResult_whenCondition` (e.g., `getAllPosts_shouldReturnPosts_whenAuthHeaderProvided`)
- Use descriptive names that read as a specification

## Service Tests

- Annotate with `@ExtendWith(MockitoExtension.class)`
- Can extend `BaseServiceTest` for common mock setup (provides `@Mock AuthService authService`)
- Mock dependencies:
  - `@Mock` + `@RestClient` + `@Inject` for REST client interfaces
  - `@Mock` for AuthService
  - `@InjectMocks` for the service under test
- Given / When / Then structure with AssertJ fluent assertions

### Common Patterns

```java
@Test
void getAllPosts_shouldReturnPosts_whenAuthHeaderProvided() {
    // Given
    final var authHeader = TestUtil.TEST_AUTH_HEADER;
    final var expectedPosts = Arrays.asList(MockDataUtil.createPostResponseDto());
    when(this.postClient.getAllPosts(authHeader)).thenReturn(expectedPosts);

    // When
    final var result = this.postService.getAllPosts(authHeader);

    // Then
    assertThat(result).isEqualTo(expectedPosts);
    verify(this.postClient).getAllPosts(authHeader);
}
```

### Exception Testing

Use `ExceptionTestUtil` helpers:

- `assertThrowsAuthenticationRequired(runnable)` — verifies `AuthenticationException` when auth header is null
- `assertThrowsBackendConnectionFailed(runnable)` — verifies `ServiceException` on `ProcessingException`

Test all error paths:

1. Auth header null → `AuthenticationException`
2. `ProcessingException` (connection failure) → `ServiceException`
3. `WebApplicationException` with 401 → `authService.logout()` + `AuthenticationException`
4. `WebApplicationException` with other status → `ServiceException` with error message from `ErrorMessageUtil`

## View Tests

- Use reflection to set `initialized = true` to skip Vaadin component initialization (which requires `VaadinService`)
- `MockedStatic` is thread-local — cannot mock statics in async `CompletableFuture` threads
- Mock services and verify view behavior independently of the Vaadin runtime
- Use `ComponentTestUtil` for Vaadin component test helpers

## Test Utilities

Located in `util/`:

| Utility             | Purpose                                                                    |
| ------------------- | -------------------------------------------------------------------------- |
| `BaseServiceTest`   | Abstract base with `@Mock AuthService` and common setup methods            |
| `MockDataUtil`      | Factory methods for creating mock DTOs (`createPostResponseDto()`, etc.)   |
| `TestDataBuilder`   | Builds unique test data with generated suffixes                            |
| `ResponseTestUtil`  | Creates mock HTTP responses for `WebApplicationException` testing          |
| `ServiceTestUtil`   | Auth header setup helpers (`setupAuthHeader()`)                            |
| `ExceptionTestUtil` | Exception assertion helpers (`assertThrowsAuthenticationRequired()`, etc.) |
| `TestUtil`          | General constants (`TEST_AUTH_HEADER`) and helpers                         |
| `ComponentTestUtil` | Vaadin component test helpers (placeholder — currently empty) |

## Assertions

- AssertJ fluent assertions (`assertThat(result).isEqualTo(expected)`)
- One logical concept per test — multiple asserts are fine if they verify one thing
- `assertThatThrownBy(() -> ...)` for exception assertions

## Mocking Strategy

- **REST clients**: mock with Mockito `@Mock`. Stub with `when(...).thenReturn(...)` or `when(...).thenThrow(...)`
- **AuthService**: mock to control authentication state. `when(authService.getAuthorizationHeader()).thenReturn(header)`
- **Vaadin static methods**: use `MockedStatic` sparingly — prefer testing logic in isolation
- **No real HTTP calls**: all REST client calls are mocked. No Testcontainers needed.

## Running Tests

```shell
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=PostServiceTest

# Single test method
./mvnw test -Dtest=PostServiceTest#getAllPosts_shouldReturnPosts_whenAuthenticated
```
