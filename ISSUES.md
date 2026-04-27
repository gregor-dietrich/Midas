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

## Vaadin 25 theme migration fixes (GUI)

- Replace deprecated `lumoImports` usage in `midas-gui/src/main/frontend/themes/starter-theme/theme.json`.
- If utility classes required, add `@StyleSheet(Lumo.UTILITY_STYLESHEET)` in `midas-gui/src/main/java/de/vptr/midas/gui/AppConfig.java`.
- Fix component-style loading warning (`vaadin-text-field.css`) by enabling `themeComponentStyles` or moving styles to supported setup.
