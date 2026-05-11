# Issues

## (WIP) User Accounts (API & GUI)

- profile info, account settings, privacy settings
- it should allow the current user to edit their own profile information
  - it should not allow user rank assignment
  - it should include password change functionality
  - add avatar/profile picture management

## ULIDs as Primary/Foreign Keys (API)

**Goal:** remove sequential IDs from external surfaces; use ULIDs to reduce enumeration risk.

**Plan:**

1. Inventory every entity/resource exposing numeric IDs in API routes, DTOs, and GUI URLs.
2. Add immutable `publicId` ULID column per entity (unique + indexed). Keep current numeric PK/FK internal during migration.
3. Create DB migration to add columns and indexes, backfill ULIDs for existing rows, then enforce `NOT NULL`.
4. Update API contracts to accept/return ULIDs only (path params, request/response DTOs, service lookups by `publicId`).
5. Update GUI routing and REST clients to use ULIDs end-to-end.
6. Add ULID validation + tests for CRUD and authorization flows using ULID identifiers.
7. Roll out in two steps: compatibility window (dual-read), then remove numeric-ID access from controllers/DTOs.

**Done when:**

- No external endpoint exposes numeric IDs.
- All existing records have unique non-null ULIDs.
- Clients and docs fully switched to ULIDs.
- SpotBugs, CheckStyle and all Maven Tests passing for api and gui.

## Encrypt-at-Rest (API)

**Goal:** encrypt PII at rest with file-backed key management.

**Plan:**

1. Classify sensitive fields across entities (starting with `UserEntity.email`, `UserEntity.lastIp`) and record lookup requirements (display-only vs searchable).
   - Note: lastIp should be removed permanently anyway.
2. Implement field encryption using AES-256-GCM with random IV per value and versioned ciphertext envelope.
3. Add blind-index/hash companion columns for fields requiring equality search.
4. Introduce key management via `MIDAS_ENCRYPTION_KEY_FILE`:
   - load key from file on startup
   - generate and persist key if missing
   - enforce restrictive file permissions
5. Add shared crypto service + entity converters/mappers so encryption/decryption is centralized and plaintext is never logged.
6. Add migrations to create encrypted/blind-index columns, backfill legacy plaintext, and remove plaintext columns after cutover.
7. Update `application.properties` with sensible local default key path and `docker-compose.yml` with env var + persistent volume mount.
8. Add unit/integration tests for crypto behavior, startup key generation, CRUD, and searchable encrypted fields.

**Done when:**

- Target PII fields are stored encrypted at rest.
- App starts with existing key or generates one when absent.
- Compose/dev setup persists key material via mounted volume.
- SpotBugs, CheckStyle and all Maven Tests passing for api and gui.

---

## Test Coverage Improvement Plan

**Goal:** Raise overall test coverage to 80%+ lines / 62%+ branches across all API packages, with GUI services (excl. views/components) reaching same target.

**Current State (from .coverage.md):**

- API: 74.4% lines / 57.1% branches
- GUI: 80.8% lines / 55.0% branches
- Total: 77.7% lines / 56.2% branches

**Target:** 80% lines / 62% branches per package on average. View and component classes excluded from GUI requirements.

### Phase 1: Quick Wins (0% coverage stubs, <30 minutes)

These are unused or trivial DTOs with no test footprint. Easy to knock out:

1. **PasswordChangeDto** (6 lines, 0% lines coverage)
   - Exists but unused (see X-CONS-02)
   - Add basic DTO serialization tests (getter/setter round-trip) once the password change endpoint is implemented (tied to X-CONS-02 timeline)

2. **AccountAccessResponseDto** (1 line, 0% lines)
   - Add minimal DTO test (serialization round-trip)

3. **AccountAssociationResponseDto** (1 line, 0% lines)
   - Add minimal DTO test

**Effort:** ~1-2 hours total. Unblocks removal of 0% coverage anomalies.

### Phase 2: Highest Impact Resources (API)

These resource classes are <40% coverage and handle critical CRUD paths. Prioritize in this order:

1. **AccountResource** (44.8% lines, 10.3% branches) — 62 lines uncovered
   - Missing: GET /accounts/{id}, PATCH operations, error cases (auth failures, not-found, validation)
   - Add tests for GET list/paginated, GET by ID, create, update, delete, authorization checks
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~6-8 hours
   - **✅ COMPLETED:** 95.4% lines / 62.5% branches

2. **UserGroupResource** (27.6% lines, 0% branches) — 21 lines uncovered
   - Missing: most CRUD paths
   - Add comprehensive resource tests: GET, POST, PUT, DELETE with various auth scenarios
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~4-5 hours
   - **✅ COMPLETED:** 89.7% lines / 50.0% branches

3. **PaymentResource** (36.7% lines, 0% branches) — 19 lines uncovered
   - Missing: create, list with pagination, error cases
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~4-5 hours
   - **✅ COMPLETED:** 93.3% lines / 50.0% branches

4. **PageResource** (38.9% lines, 0% branches) — 11 lines uncovered
   - Missing: CRUD ops, search paths
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~3-4 hours
   - **✅ COMPLETED:** 88.9% lines / 50.0% branches

5. **PostCommentResource** (51.5% lines, 0% branches) — 16 lines uncovered
   - Missing: authorization, error cases
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~3-4 hours
   - **✅ COMPLETED:** 90.9% lines / 50.0% branches

6. **PostResource** (51.6% lines, 0% branches) — 15 lines uncovered
   - Missing: search, filtering, authorization edge cases
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~3-4 hours

**Subtotal:** ~23-30 hours. Raises API resource coverage from 44.8% → ~78-80%, and branches from 10.3% → ~65%+.

### Phase 3: Service Layer (API)

Focus on services with <70% lines coverage:

1. **AccountAccessService** (50.8% lines, 44.4% branches) — 58 lines uncovered
   - Missing: permission checks, edge cases
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~5-6 hours

2. **UserGroupService** (56.2% lines, 27.3% branches) — 28 lines uncovered
   - Missing: validation, error paths
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~4-5 hours

3. **PostCommentService** (67.5% lines, 41.7% branches) — 26 lines uncovered
   - Missing: edge cases, authorization checks
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~3-4 hours

**Subtotal:** ~12-15 hours. Raises API service coverage to ~78-80%+, branches to ~65%+.

### Phase 4: Entity Coverage (API)

Entity classes are currently 58.1% lines, 12.5% branches. These are Active Record pattern classes and often require integration tests to exercise properly:

**Affected entities (worst first):**

- UserGroupEntity (7.7%), UserGroupMetaEntity (14.3%), PostCommentEntity (16.7%), PageEntity (20.0%), PostCategoryEntity (25.0%), AccountEntity (28.6%), UserEntity (33.3%)

**Strategy:** Rather than unit-testing entities in isolation, leverage existing repository/service integration tests and add missing scenarios:

- Constructor variants, lifecycle hooks, lazy-load relationships
- Business logic methods (if any)
- Validation constraints

**Target:** 80% lines, 65%+ branches via integration test coverage.
**Effort:** ~6-8 hours (spread across service tests).

### Phase 5: GUI Service Layer (non-view)

**Current state:** GUI services are 84% lines / 68% branches — already strong. Remaining gaps:

1. **BackendAvailabilityService** (36.4% lines, 50% branches) — 14 lines uncovered
   - Missing: timeout/failure scenarios
   - **Target:** 80% lines, 62%+ branches
   - **Effort:** ~2 hours

2. **UserRankService** (78.2% lines, 60% branches) — 19 lines uncovered
   - Missing: edge cases
   - **Effort:** ~2-3 hours

**Subtotal:** ~4-5 hours.

### Recommended Execution Order

1. **Week 1 (Phase 1 + Phase 2.1-2.3)**
   - Quick wins: 1-2 hours
   - AccountResource, UserGroupResource, PaymentResource: ~14-18 hours
   - Total: ~15-20 hours

2. **Week 2 (Phase 2.4-2.6 + Phase 3)**
   - PageResource, PostCommentResource, PostResource: ~10-12 hours
   - AccountAccessService, UserGroupService, PostCommentService: ~12-15 hours
   - Total: ~22-27 hours

3. **Week 3 (Phase 4 + Phase 5)**
   - Entity coverage (via integration tests): ~6-8 hours
   - GUI services: ~4-5 hours
   - Total: ~10-13 hours

**Grand Total:** ~47-60 hours to reach 80% lines / 65% branches across API + GUI (services only).

### Key Testing Patterns to Leverage

- **Resource tests:** Use `@QuarkusTest` + `@TestHTTPEndpoint`, mock services where needed
- **Service tests:** Unit test with mocked repositories; integration test with `@QuarkusTest` for complex logic
- **Entity tests:** Cover via integration tests (CRUD, relationships, lifecycle)
- **Branch coverage:** Pay special attention to error paths, authorization checks, edge cases (null, empty, boundaries)

### Success Criteria

- [ ] All API packages ≥80% lines, ≥62% branches
- [ ] All API services ≥80% lines, ≥62% branches
- [ ] All API resources ≥80% lines, ≥62% branches
- [ ] GUI services (excl. views/components) ≥80% lines, ≥62% branches
- [ ] No 0% coverage anomalies remain
- [ ] `make coverage` passes and generates report

---

## Code Review Findings — Security, Bugs, Performance, Architecture

## Cross-Module Consistency & Architecture

### X-CONS-02: PasswordChangeDto exists but no endpoint/feature

- **Issue:** `PasswordChangeDto` exists in API but no GUI client or endpoint uses it.
- **Fix:** Implement the full password-change flow: API endpoint (`POST /api/v1/users/me/password`), GUI REST client method, GUI service method, GUI view/dialog for password change.
- **Review instruction:** Ensure the new endpoint follows existing patterns: `@Authenticated`, `@RolesAllowed`, `@Valid` DTO, service-layer validation, response via `ResponseUtil`.

### New Feature: Password Change (API + GUI)

**Goal:** Allow authenticated users to change their own password.

**API Plan:**

1. Add `POST /api/v1/users/me/password` to `UserResource` (or new `MeResource`).
2. Consume `@Valid PasswordChangeDto`.
3. Verify `currentPassword` matches stored hash via `PasswordHashingService`.
4. Validate `newPassword` meets complexity rules (reuse existing validation if any, or add `@Size(min = 8)`).
5. Hash new password with new random salt via `PasswordHashingService`.
6. Update `UserEntity.password` and `UserEntity.salt`.
7. Return `204 No Content` on success, `401` if current password wrong, `422` if validation fails.
8. Add tests: `UserResourceTest` and `UserServiceTest`.

**GUI Plan:**

1. Add `changePassword` method to `UserClient` REST client.
2. Add `changePassword` method to `UserService` with standard exception handling.
3. Add "Change Password" button/dialog to `UserDetailView` or `AdminUserDialog`.
4. On success, show notification and optionally force re-login.
5. Add tests for the service method and dialog.

**Review instruction:** Ensure password change does not store plaintext, uses the same PBKDF2-SHA256 params as registration, and invalidates existing sessions if the backend supports it.

---

## API-SMELL-01: AccountResource is a God class

- **File:** `midas-api/src/main/java/.../resource/AccountResource.java` (455 lines)
- **Issue:** Mixes account CRUD, payment creation, balance queries, user-account associations, and metadata updates.
- **Fix:** Extract payment-related endpoints to `PaymentResource` (or dedicated sub-resource). Extract user-account association endpoints to `AccountMembershipResource`.
- **Review instruction:** Audit all resource classes >300 lines for SRP violations. Split any class handling more than one distinct domain concept.

---

## Architecture: GUI Admin CRUD Abstraction Refactor

**Note:** GUI-SMELL-01 (admin views) and GUI-SMELL-02 (admin dialogs) moved here from Phase 3 — they are architectural refactor work, not immediate bug fixes.

**Goal:** Eliminate duplicated CRUD boilerplate across ~17 admin view/dialog files.

### GUI-SMELL-01: Admin views duplicated CRUD boilerplate

- **Files:** `midas-gui/src/main/java/.../view/AdminPostView.java`, `AdminUserView.java`, etc. (~9 files)
- **Issue:** Each admin view repeats grid, data provider, dialog, action column, search layout, notification.
- **Fix:** Introduce `AbstractAdminCrudView<T, D extends FormDialog<T>>` with template methods.

### GUI-SMELL-02: Admin dialogs duplicated form logic

- **Files:** `midas-gui/src/main/java/.../component/dialog/AdminPostDialog.java`, etc. (~8 files)
- **Issue:** Dialogs repeat `Binder<T>` init, field creation, `BeanValidationBinder` config, save/cancel listeners.
- **Fix:** Create `AbstractAdminFormDialog<T>` with protected `configureBinder()` hook and generic `save()` / `cancel()` handlers.

**Plan:**

1. Create `AbstractAdminCrudView<T extends Serializable, D extends FormDialog<T>>` extending `AbstractAsyncDataView`.
   - Template methods: `createDialog()`, `getGridColumns()`, `getClientMethod()`, `getDeleteConfirmMessage()`.
   - Shared logic: grid setup, data provider wiring, action column renderer, search layout, success notification.
2. Create `AbstractAdminFormDialog<T>` extending `FormDialog<T>`.
   - Protected hooks: `configureBinder()`, `createFormLayout()`.
   - Generic `save()` / `cancel()` handlers using a `Consumer<T>` callback.
3. Migrate one view/dialog pair as a pilot (e.g., `AdminPostView` + `AdminPostDialog`).
4. Migrate remaining 8 views and 7 dialogs incrementally.
5. Ensure all existing tests continue to pass; add tests for the abstract base classes.

**Review instruction:** Ensure the abstraction does not force unnatural complexity on simple CRUD views. Keep overrides lightweight. Verify SpotBugs/Checkstyle pass after the refactor.

**Done when:**

- All admin views extend `AbstractAdminCrudView`.
- All admin dialogs extend `AbstractAdminFormDialog`.
- No duplicated grid/dialog setup code remains.
- All existing tests pass.
- SpotBugs, CheckStyle and all Maven Tests passing for gui.

---

## Review Instructions Summary

For every issue category above, perform the following codebase-wide reviews before fixing:

1. **Unbounded Maps / Caches:** Search all `ConcurrentHashMap`, `HashMap`, `LinkedHashMap` used as caches in both modules. Ensure every cache has eviction (TTL or max-size). Apply Caffeine/Guava consistently.
2. **Exception Logging:** Search all `LOG.*f(format, ..., throwable)` calls. Verify the throwable is passed as the first argument (JBoss Logging signature) or use a non-varargs overload.
3. **Anonymous Access:** Search all `@PermitAll` in `midas-api`. Ensure only genuinely public endpoints (health checks) use it. Remove from all business resources.
4. **Authentication Guards:** Verify all `@Route` views in `midas-gui` use `MainLayout` or `AdminMainLayout` (both already implement `BeforeEnterObserver` with auth checks). Ensure no view bypasses these layouts or has its own inconsistent auth logic.
5. **Blocking Async:** Search all `CompletableFuture.supplyAsync`, `CompletableFuture.runAsync` in `midas-gui`. Ensure blocking I/O never uses the common pool.
6. **Backend Error Leakage:** Search all `Notification.show`, `NotificationUtil`, and user-facing error displays in `midas-gui`. Ensure raw backend messages are never shown.
7. **Database-Specific Column Definitions:** Search all `@Column(columnDefinition = ...)` in `midas-api`. Remove or generalize MySQL-specific types.
8. **Missing @Test:** Search all test methods named `test*` in both modules for missing `@Test` or `@ParameterizedTest`.
9. **Unbounded Lists:** Search all service/repository methods returning `List<T>` in `midas-api`. Add pagination or max limits.
10. **Duplicated Guard Clauses:** Verify all `midas-gui` services use `authService.requireAuthorizationHeader()` (which already centralizes the null-check and throws `AuthenticationException`). Ensure no legacy `getAuthorizationHeader() == null` patterns remain.
11. **PII in Logs:** Search all `toString()` overrides and log statements in both modules for sensitive data (passwords, emails, financial data, IP addresses). Redact or remove.
12. **God Classes:** Identify all classes >300 lines. Evaluate for SRP violations and split where appropriate.
13. **Hardcoded Constants:** Search all numeric literals representing timeouts, retries, limits, page sizes, buffer sizes. Extract to named constants or config.
14. **DTO Field Mismatches:** Compare every API request/response DTO with its GUI counterpart and with test JSON builders. Fix all drift.
15. **Patch vs PUT Semantics:** Audit all `patch*` methods to ensure they reject invalid input rather than silently skipping.
