# GUI Test Utilities

## Service tests

GUI services are tested in two layers:

- **`*HttpContractTest`** (`@QuarkusTest`, extending `AbstractHttpContractTest`) run a service against
  `StubBackend`, an in-process HTTP stub of the Midas API. They pin what the service does with a given status and
  payload, and what it sends (path, query, body, credentials). Prefer them for anything observable over HTTP.
- **Mock-based `*ServiceTest`** mock the generated REST clients with typed returns and cover what the stub cannot
  reach cheaply: connection failures (`ServiceConnectionFailureTest`), session caching, CDI events, edge cases of
  in-memory logic. Express backend errors as a thrown `WebApplicationException` (`new WebApplicationException(500)`)
  or `ProcessingException`.

## Utilities

- `AbstractHttpContractTest` / `StubBackend`: base and stub for the contract tests; every request must go to
  `/api/v1/` and carry the Authorization header. `AuthServiceHttpContractTest` runs the real `AuthService` instead of
  the base's mock, for the login check and the header a generated client sends from a worker thread.
- `AbstractSessionServiceTestBase`: mocks `VaadinSession.getCurrent()` for services that cache in the session.
- `MockDataUtil`: DTOs with default values for dialog and view tests, and for mock-based service tests
  (`UserRankServiceTest`, `PostCommentServiceTest`).
- `UiTestUtil`: helpers for Vaadin component tests.
