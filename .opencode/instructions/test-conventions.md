# Test Conventions — Midas

Covers both API and GUI test patterns, structure, naming, mocking, and utilities.

---

## midas-api (Backend)

### Framework

- `@QuarkusTest` on all test classes
- REST Assured via `TestUtil` helpers for resource tests
- Docker required (Testcontainers starts PostgreSQL automatically)
- Service tests: `@Inject` the service; `@Transactional` on DB-mutating tests
- `ServiceTestDataBuilder` for unique test data

### Naming

- **Test class**: `<ResourceName>Test` or `<ServiceName>Test` (e.g., `PostResourceTest`, `PostServiceTest`)
- **Test method**: `testMethodName` or `testMethodName_context` (e.g., `testCreatePost`, `testGetPost_unauthorized`)

### Resource Tests

- Use `TestUtil` helpers: `testUnauthorizedAccess()`, `testAuthorizedPostWithCreation()`, etc.
- Admin/guest Basic Auth fixtures available
- Test HTTP status codes, response bodies, and error payloads

### Service Tests

```java
@QuarkusTest
@TestTransaction
class PostServiceTest {
    @Inject
    PostService postService;

    @Test
    void testCreatePost() {
        // Arrange: use ServiceTestDataBuilder for unique data
        // Act
        // Assert
    }
}
```

### Test Utilities

| Utility | Purpose |
|---|---|
| `TestUtil` | REST Assured helpers, `generateUniqueSuffix()`, auth fixtures |
| `TestDataBuilder` | Unique test data construction |
| `ServiceTestDataBuilder` | Service-layer test data |
| `ServiceTestUtil` | Service-level auth setup |

### Running Tests

```shell
cd midas-api
./mvnw test                              # all tests (needs Docker)
./mvnw test -Dtest=PostResourceTest      # single class
./mvnw test -Dtest=PostResourceTest#testCreatePost  # single method
```

---

## midas-gui (Frontend)

### Framework

- JUnit 5 + Mockito + AssertJ
- No Quarkus test container needed (services unit-tested in isolation)
- Three test categories: service, view, component

### Naming

- **Test class**: `<ClassName>Test` (e.g., `PostServiceTest`, `MainLayoutTest`)
- **Test method**: `methodName_shouldExpectedResult_whenCondition` (e.g., `getAllPosts_shouldReturnPosts_whenAuthHeaderProvided`)

### Service Tests

- Annotate with `@ExtendWith(MockitoExtension.class)`
- Can extend `BaseServiceTest` for common mock setup (provides `@Mock AuthService`)
- Mock dependencies:
  - `@Mock` + `@RestClient` + `@Inject` for REST client interfaces
  - `@Mock` for AuthService
  - `@InjectMocks` for the service under test
- Given / When / Then structure with AssertJ fluent assertions

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

Test all four error paths for every service method:

1. Auth header null → `AuthenticationException`
2. `ProcessingException` (connection failure) → `ServiceException`
3. `WebApplicationException` with 401 → `authService.logout()` + `AuthenticationException`
4. `WebApplicationException` with other status → `ServiceException` with message from `ErrorMessageUtil`

Use `ExceptionTestUtil` helpers:
- `assertThrowsAuthenticationRequired(runnable)`
- `assertThrowsBackendConnectionFailed(runnable)`

### View Tests

- Use reflection to set `initialized = true` to skip Vaadin component initialization (requires `VaadinService`)
- `MockedStatic` is thread-local — cannot mock statics in async `CompletableFuture` threads
- Mock services and verify view behavior independently of the Vaadin runtime
- Use `ComponentTestUtil` for Vaadin component test helpers

### Test Utilities

Located in `util/`:

| Utility | Purpose |
|---|---|
| `BaseServiceTest` | Abstract base with `@Mock AuthService` and common setup |
| `MockDataUtil` | Factory methods for mock DTOs |
| `TestDataBuilder` | Builds unique test data with generated suffixes |
| `ResponseTestUtil` | Creates mock HTTP responses for `WebApplicationException` |
| `ServiceTestUtil` | Auth header setup helpers |
| `ExceptionTestUtil` | Exception assertion helpers |
| `TestUtil` | General constants (`TEST_AUTH_HEADER`) and helpers |
| `ComponentTestUtil` | Vaadin component test helpers (placeholder) |

### Mocking Strategy

- **REST clients**: mock with Mockito `@Mock`. Stub with `when(...).thenReturn(...)` or `when(...).thenThrow(...)`
- **AuthService**: mock to control authentication state
- **Vaadin static methods**: use `MockedStatic` sparingly — prefer testing logic in isolation
- **No real HTTP calls**: all REST client calls are mocked. No Testcontainers needed.

### Running Tests

```shell
cd midas-gui
mvn test                                    # all tests
mvn test -Dtest=PostServiceTest             # single class
mvn test -Dtest=PostServiceTest#getAllPosts_shouldReturnPosts_whenAuthenticated  # single method
```
