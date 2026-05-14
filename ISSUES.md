# Issues

## 1. Security Remediation & Hardening

**Goal:** Address critical vulnerabilities and architectural weaknesses identified during the security audit.

### Sub-Task: Auth Hardening & DoS Prevention

- [x] **Fix User Lockout DoS:** Update `AuthFailureRateLimitFilter` to use a composite key (`username:ip`) for rate limiting. This prevents attackers from locking out legitimate users via username-only throttling.

### Sub-Task: Authorization & IDOR Prevention

- [x] **Eliminate Impersonation in Posts/Payments:**
  - Remove `userPublicId` from `PostDto` and `PaymentDto` request bodies.
  - Update `PostService` and `PaymentService` to resolve the owner entity via `SecurityContext.getUserPrincipal().getName()`.
- [x] **Granular Ownership Checks:** Ensure all `PUT`, `PATCH`, and `DELETE` operations on user-owned resources (Posts, Comments, Payments) verify that the authenticated user is the actual owner (or an admin).

### Sub-Task: Content Security (XSS Protection)

- [ ] **OWASP HTML Sanitization:** Integrate the OWASP Java HTML Sanitizer (already in `pom.xml`) into the service layer.
- [ ] **Sanitize User Content:** Apply strict HTML sanitization to `content` fields in `PostService` and `PageService`, and the `comment` field in `PostCommentService`.

### Sub-Task: Cryptographic Integrity

- [ ] **Key Separation:** Update `EncryptionService` to use a Key Derivation Function (KDF) to derive distinct sub-keys for AES-GCM encryption and HMAC-SHA256 blind indexing from the master key.

### Sub-Task: Session & GUI Hardening

- [ ] **Tighten CSP:** Remove `unsafe-inline` and `unsafe-eval` from the Content Security Policy in `midas-gui/src/main/resources/application.properties`.
- [ ] **Persist Session Key:** Externalize the session encryption key in `SessionAuthorizationStore` (e.g., via `EncryptionKeyManager` or config) to support horizontal scaling and prevent session loss on restart.

---

## Best Practice Mandates

1. **API-Side Auth:** Never trust the GUI for permissions. Always use `@RolesAllowed` on every API endpoint.
2. **Re-verification:** The API should re-fetch user status from DB on sensitive operations (e.g., `isBanned`, `rank`).
3. **No Plaintext:** Never log PII or passwords. Scrub DTOs before logging.
4. **Ownership Principle:** Always derive "owner" IDs from the authenticated session, never from request parameters.
5. **Quality Gates:** Every task is incomplete until `make format lint test` passes for both `midas-api` and `midas-gui`.
