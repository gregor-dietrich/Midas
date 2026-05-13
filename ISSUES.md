# Issues

## 1. Profile Management & Security Hardening

**Goal:** Implement comprehensive user profile management and upgrade core security primitives (Hashing, Throttling).

- [x] **Sub-Task: Upgrade to Bcrypt & Remove Salt Column**

- **API Change:** Replace PBKDF2 with Bcrypt in `PasswordHashingService`.

```java
// midas-api/.../security/PasswordHashingService.java
@ApplicationScoped
public class PasswordHashingService {
    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }
    public boolean verifyPassword(String password, String hash) {
        return BcryptUtil.matches(password, hash);
    }
}
```

- **Entity Change:** Remove `String salt` from `UserEntity`.
- **Database:** Update `init.sql` to remove `salt` column and update default hashes (e.g., admin:admin -> `$2a$10$8K1p/v87C.YmG.D.y.X.9.M.V.W.X.Y.Z`).

- [x] **Sub-Task: Password Change Endpoint**

- **API:** Implement `POST /api/v1/users/me/password`.
- **Validation:** Add `@Size(min = 8)` and complexity checks to `PasswordChangeDto`.
- **GUI:** Add "Change Password" dialog to `UserDetailView`.

### Sub-Task: Login Throttling (Exponential Backoff)

- **API:** Implement `LoginAttemptService` to track failures by IP/Username.
- **Logic:** `long delay = 1L << (attempts - 5);` (start slowing down after 5 failures).
- **Integration:** Call `loginAttemptService.recordFailure(username, ip)` in `AuthResource`.

---

## 2. ULIDs as Public Keys

**Goal:** Replace sequential IDs in all external APIs and URLs with ULIDs.

### Sub-Task: Infrastructure Implementation

- **API:** Add `UlidUtil` and `BaseEntity`.

```java
// midas-api/.../entity/BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    public String publicId;

    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null) this.publicId = UlidCreator.getUlid().toString();
    }
}
```

### Sub-Task: Migration & DTO Update

- **DB:** Migration to add `public_id`, backfill existing rows, add `UNIQUE INDEX`.
- **DTOs:** All Response DTOs must use `publicId` (String) instead of `id` (Long).
- **Resources:** Update `@Path("/{id}")` to `@Path("/{publicId}")` and use `find("publicId", publicId).firstResult()`.

---

## 3. Encrypt-at-Rest

**Goal:** Protect PII (Email) at rest using AES-256-GCM.

### Sub-Task: Crypto Infrastructure

- **API:** Implement `EncryptionKeyManager` (file-backed) and `EncryptionService`.
- **Envelope Format:** `1|base64(iv)|base64(ciphertext+tag)`.
- **Converter:**

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attr) {
        return CDI.current().select(EncryptionService.class).get().encrypt(attr);
    }
    // ... decrypt on convertToEntityAttribute
}
```

### Sub-Task: User Entity Email Encryption

- **Entity:** Annotate `UserEntity.email` with `@Convert(converter = EncryptedStringConverter.class)`.
- **Blind Index:** Add `email_blind_index` for searchable lookups.
- **Repository:** Update `UserRepository.findByEmail(email)` to use `generateBlindIndex(email)`.

---

## 4. Resilience & Timeouts in Clients

**Goal:** Centralize fault tolerance in the REST client layer.

- [x] **Sub-Task: Client Refactoring**

- **GUI:** Move `@Retry` and `@Timeout` from `Service` classes to `Client` interfaces.

```java
@RegisterRestClient(configKey = "user-api")
public interface UserClient {
    @GET
    @Path("/{publicId}")
    @Retry(maxRetries = 3)
    @Timeout(2000)
    UserResponseDto getUser(@PathParam("publicId") String publicId);
}
```

- **Config:** Define timeouts in `application.properties`:
  `quarkus.rest-client."user-api".connect-timeout=2000`

---

## 5. UI/UX Enhancements & Real-time Updates

**Goal:** Modernize the GUI with glassmorphism and real-time push.

- [x] **Sub-Task: GlassAccentBar Component**

- **GUI:** Create `GlassAccentBar.java` (Div with gradient top border).
- **Style:** Use absolute positioning and `var(--lumo-primary-color)`.

### Sub-Task: Real-time via Vaadin Push

- **GUI:** Add `@Push` to `AppConfig`.
- **Flow:** API fires CDI Event -> GUI `Broadcaster` -> `UI.access()` to update Grid/Comments.

---

## 6. Admin Improvements & Bug Fixes

**Goal:** Fix layout and filtering bugs in Admin views.

- [x] **Sub-Task: Date Range Filtering Fix**

- **API:** Ensure `Resource` methods handle invalid date strings by returning `400 Bad Request` or empty result, never the full dataset.
- **GUI:** Update `AdminCommentView` to validate date pickers before calling the service.

- [x] **Sub-Task: Sidebar Responsiveness**

- **GUI:** Fix CSS for `AdminLayout` to ensure the sidebar occupies 100% height and content scrolls independently.

---

## Best Practice Mandates

1. **API-Side Auth:** Never trust the GUI for permissions. Always use `@RolesAllowed` on every API endpoint.
2. **Re-verification:** The API should re-fetch user status from DB on sensitive operations (e.g., `isBanned`, `rank`).
3. **No Plaintext:** Never log PII or passwords. Scrub DTOs before logging.
4. **Quality Gates:** Every task is incomplete until `make lint test` passes for both `midas-api` and `midas-gui`.
