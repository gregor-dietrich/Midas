package de.vptr.midas.api.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.vptr.midas.api.dto.PasswordChangeDto;
import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.security.EncryptionService;
import de.vptr.midas.api.security.PasswordHashingService;
import de.vptr.midas.api.util.EntityValidator;
import de.vptr.midas.api.util.SearchQueryUtil;
import io.quarkus.cache.CacheInvalidateAll;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for user business logic. */
@ApplicationScoped
public class UserService {

    /** The Administrator rank seeded by V1. */
    private static final String ADMIN_RANK_PUBLIC_ID = "01AN4Z07BY79KA1307SR9X4MV3";

    @Inject
    UserResponseAssembler userResponseAssembler;

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    Validator validator;

    @Inject
    AdministratorGuard administratorGuard;

    @ConfigProperty(name = "midas.default-rank-public-id")
    @Nullable
    String defaultRankPublicId;

    /** Returns all users with default pagination. */
    public List<UserResponseDto> getAllUsers() {
        return this.getAllUsers(0, 20);
    }

    /** Returns all users with the given pagination. */
    public List<UserResponseDto> getAllUsers(final int page, final int size) {
        final List<UserEntity> users = UserEntity
                .find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank ORDER BY u.publicId").page(page, size).list();
        return this.userResponseAssembler.toResponseDtos(users);
    }

    /** Returns the user with the given username. */
    public Optional<UserResponseDto> findByUsername(@Nullable final String username) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.username = ?1", username)
                .<UserEntity>firstResultOptional().map(this.userResponseAssembler::toResponseDto);
    }

    /** Returns the user with the given publicId. */
    public Optional<UserResponseDto> findById(final String publicId) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.publicId = ?1", publicId)
                .<UserEntity>firstResultOptional().map(this.userResponseAssembler::toResponseDto);
    }

    /** Returns the user with the given email. */
    public Optional<UserResponseDto> findByEmail(@Nullable final String email) {
        final String blindIndex = this.encryptionService.generateBlindIndex(email);
        return UserEntity
                .find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.emailBlindIndex = ?1", blindIndex)
                .<UserEntity>firstResultOptional().map(this.userResponseAssembler::toResponseDto);
    }

    /**
     * Normalizes email field by converting empty/blank strings to null. This ensures that only null or valid email
     * addresses are stored in the database, preventing unique constraint violations from multiple empty strings.
     */
    @Nullable
    private String normalizeEmail(@Nullable final String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    /** Creates a new user. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserResponseDto createUser(final UserDto userDto) {
        // Check for duplicate username
        EntityValidator.checkUnique(this.findByUsername(userDto.username), "Username", userDto.username,
                "already taken");

        // Normalize email and check for duplicate email only if email is provided
        final String normalizedEmail = this.normalizeEmail(userDto.email);
        if (normalizedEmail != null) {
            EntityValidator.checkUnique(this.findByEmail(normalizedEmail), "Email", normalizedEmail, "already in use");
        }

        final UserEntity user = this.buildUserEntity(userDto, normalizedEmail);
        user.persistAndFlush();
        return this.userResponseAssembler.toResponseDto(user);
    }

    /** Tells whether any user account exists. */
    public boolean hasUsers() {
        return UserEntity.count() > 0;
    }

    /**
     * Creates the initial admin account, but only while no user exists. Once any account exists, it does nothing, so it
     * cannot be used to escalate privileges.
     *
     * @return true if the admin was created, false if users already exist
     * @throws ConstraintViolationException
     *             if the username or password violates the regular {@link UserDto} rules
     */
    @Transactional
    public boolean createInitialAdmin(final String username, final String password) {
        if (this.hasUsers()) {
            return false;
        }
        final var admin = new UserDto(username, password, null, ADMIN_RANK_PUBLIC_ID, false, true, null);
        final var violations = this.validator.validate(admin);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        this.createUser(admin);
        return true;
    }

    private UserEntity buildUserEntity(final UserDto userDto, @Nullable final String normalizedEmail) {
        final UserEntity user = new UserEntity();
        user.username = userDto.username;
        user.email = normalizedEmail;
        user.emailBlindIndex = this.encryptionService.generateBlindIndex(normalizedEmail);
        user.banned = Boolean.TRUE.equals(userDto.banned);
        user.activated = Boolean.TRUE.equals(userDto.activated);
        // Always server-generated; never client-settable (see UserDto).
        user.activationKey = UUID.randomUUID().toString();
        user.lastIp = userDto.lastIp;

        this.applyPassword(user, userDto.password);
        this.applyRank(user, userDto.rankPublicId, true);

        return user;
    }

    /** Updates the user with the given publicId. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserResponseDto updateUser(final String publicId, final UserDto userDto) {
        return this.administratorGuard.guard(() -> {
            final UserEntity existingUser = UserEntity.find("publicId", publicId).firstResult();
            if (existingUser == null) {
                throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
            }

            // Check for duplicate username (only if username is different from current)
            if (!Objects.equals(userDto.username, existingUser.username)) {
                EntityValidator.checkUnique(this.findByUsername(userDto.username), "Username", userDto.username);
            }

            // Normalize email and check for duplicate email (only if email is different
            // from current)
            final String normalizedEmail = this.normalizeEmail(userDto.email);
            if (!Objects.equals(normalizedEmail, existingUser.email) && normalizedEmail != null) {
                EntityValidator.checkUnique(this.findByEmail(normalizedEmail), "Email", normalizedEmail);
            }

            // Complete replacement (PUT semantics)
            existingUser.username = userDto.username;
            existingUser.email = normalizedEmail;
            existingUser.emailBlindIndex = this.encryptionService.generateBlindIndex(normalizedEmail);
            existingUser.banned = Boolean.TRUE.equals(userDto.banned);
            existingUser.activated = Boolean.TRUE.equals(userDto.activated);

            this.applyPassword(existingUser, userDto.password);
            this.applyRank(existingUser, userDto.rankPublicId, true);

            existingUser.persistAndFlush();
            return this.userResponseAssembler.toResponseDto(existingUser);
        });
    }

    /** Partially updates the user with the given publicId. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserResponseDto patchUser(final String publicId, final UserDto userDto) {
        // A patch omits fields, so the DTO as a whole is not validated; a new password still is.
        if (userDto.password != null) {
            final var violations = this.validator.validateProperty(userDto, "password");
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        return this.administratorGuard.guard(() -> {
            final UserEntity existingUser = UserEntity.find("publicId", publicId).firstResult();
            if (existingUser == null) {
                throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
            }

            // Check for duplicate username if username is being updated
            if (userDto.username != null && !userDto.username.trim().isEmpty()
                    && !userDto.username.equals(existingUser.username)) {
                EntityValidator.checkUnique(this.findByUsername(userDto.username), "Username", userDto.username);
            }

            // Check for duplicate email if email is being updated
            if (userDto.email != null) {
                final String normalizedEmail = this.normalizeEmail(userDto.email);
                if (!Objects.equals(normalizedEmail, existingUser.email) && normalizedEmail != null) {
                    EntityValidator.checkUnique(this.findByEmail(normalizedEmail), "Email", normalizedEmail);
                }
            }

            // Partial update (PATCH semantics) - only update provided fields
            if (userDto.username != null && !userDto.username.trim().isEmpty()) {
                existingUser.username = userDto.username;
            }
            if (userDto.email != null) {
                final String normalizedEmail = this.normalizeEmail(userDto.email);
                existingUser.email = normalizedEmail;
                existingUser.emailBlindIndex = this.encryptionService.generateBlindIndex(normalizedEmail);
            }
            if (userDto.banned != null) {
                existingUser.banned = userDto.banned;
            }
            if (userDto.activated != null) {
                existingUser.activated = userDto.activated;
            }

            this.applyPassword(existingUser, userDto.password);
            this.applyRank(existingUser, userDto.rankPublicId, false);

            existingUser.persistAndFlush();
            return this.userResponseAssembler.toResponseDto(existingUser);
        });
    }

    /** Changes the password for the given username. */
    @Transactional
    public void changePassword(final String username, final PasswordChangeDto passwordChangeDto) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }

        // Re-verification: Check if user is banned or inactive
        if (user.banned) {
            throw new WebApplicationException("User is banned", Response.Status.FORBIDDEN);
        }
        if (!user.activated) {
            throw new WebApplicationException("User is not activated", Response.Status.FORBIDDEN);
        }

        // Verify current password
        if (!this.passwordHashingService.verifyPassword(passwordChangeDto.currentPassword, user.password)) {
            // Not 401: the request itself is authenticated, and clients treat 401 as an expired session.
            throw new WebApplicationException("Invalid current password", 422);
        }

        // Apply new password
        this.applyPassword(user, passwordChangeDto.newPassword);
        user.persistAndFlush();
    }

    private void applyPassword(final UserEntity user, @Nullable final String password) {
        if (password != null && !password.trim().isEmpty()) {
            final var hashedPassword = this.passwordHashingService.hashPassword(password);
            user.password = hashedPassword;
        }
    }

    private void applyRank(final UserEntity user, @Nullable final String rankPublicId, final boolean useDefault) {
        if (rankPublicId != null) {
            final UserRankEntity rank = UserRankEntity.find("publicId", rankPublicId).firstResult();
            if (rank == null) {
                throw new ValidationException("Rank with Public ID " + rankPublicId + " not found");
            }
            user.rank = rank;
        } else if (useDefault) {
            if (this.defaultRankPublicId == null) {
                throw new ValidationException("Default rank Public ID is not configured");
            }
            final UserRankEntity defaultRank = UserRankEntity.find("publicId", this.defaultRankPublicId).firstResult();
            if (defaultRank == null) {
                throw new ValidationException("Default rank with Public ID " + this.defaultRankPublicId + " not found");
            }
            user.rank = defaultRank;
        }
    }

    /** Deletes the user with the given publicId. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public boolean deleteUser(final String publicId) {
        return this.administratorGuard.guard(() -> UserEntity.delete("publicId", publicId) > 0);
    }

    /** Returns all active non-banned users. */
    public List<UserResponseDto> findActiveUsers() {
        final List<UserEntity> users = UserEntity
                .find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.activated = true AND u.banned = false")
                .list();
        return this.userResponseAssembler.toResponseDtos(users);
    }

    /** Returns all users matching the given query. */
    public List<UserResponseDto> searchUsers(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllUsers();
        }
        final List<UserEntity> users = SearchQueryUtil.applyDefaultSearchLimit(UserEntity.find("""
                SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank \
                WHERE LOWER(u.username) LIKE ?1 ESCAPE '\\'""", searchTerm)).list();
        return this.userResponseAssembler.toResponseDtos(users);
    }
}
