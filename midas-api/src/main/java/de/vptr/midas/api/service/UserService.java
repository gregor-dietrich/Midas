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
import de.vptr.midas.api.security.PasswordHashingService;
import de.vptr.midas.api.util.EntityValidator;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for user business logic. */
@ApplicationScoped
public class UserService {

    @Inject
    UserResponseAssembler userResponseAssembler;

    @Inject
    PasswordHashingService passwordHashingService;

    @ConfigProperty(name = "midas.default-rank-id", defaultValue = "2")
    @Nullable
    Long defaultRankId;

    /** Returns all users with default pagination. */
    public List<UserResponseDto> getAllUsers() {
        return this.getAllUsers(0, 20);
    }

    /** Returns all users with the given pagination. */
    public List<UserResponseDto> getAllUsers(final int page, final int size) {
        final List<UserEntity> users = UserEntity
                .find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank ORDER BY u.id").page(page, size).list();
        return this.userResponseAssembler.toResponseDtos(users);
    }

    /** Returns the user with the given username. */
    public Optional<UserResponseDto> findByUsername(@Nullable final String username) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.username = ?1", username)
                .<UserEntity>firstResultOptional().map(this.userResponseAssembler::toResponseDto);
    }

    /** Returns the user with the given id. */
    public Optional<UserResponseDto> findById(final Long id) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id = ?1", id)
                .<UserEntity>firstResultOptional().map(this.userResponseAssembler::toResponseDto);
    }

    /** Returns the user with the given email. */
    public Optional<UserResponseDto> findByEmail(@Nullable final String email) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.email = ?1", email)
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

    private UserEntity buildUserEntity(final UserDto userDto, @Nullable final String normalizedEmail) {
        final UserEntity user = new UserEntity();
        user.username = userDto.username;
        user.email = normalizedEmail;
        user.banned = Boolean.TRUE.equals(userDto.banned);
        user.activated = Boolean.TRUE.equals(userDto.activated);
        user.activationKey = userDto.activationKey != null ? userDto.activationKey : UUID.randomUUID().toString();
        user.lastIp = userDto.lastIp;

        this.applyPassword(user, userDto.password);
        this.applyRank(user, userDto.rankId, true);

        return user;
    }

    /** Updates the user with the given id. */
    @Transactional
    public UserResponseDto updateUser(final Long id, final UserDto userDto) {
        final UserEntity existingUser = UserEntity.findById(id);
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
        existingUser.banned = Boolean.TRUE.equals(userDto.banned);
        existingUser.activated = Boolean.TRUE.equals(userDto.activated);

        this.applyPassword(existingUser, userDto.password);
        this.applyRank(existingUser, userDto.rankId, true);

        existingUser.persistAndFlush();
        return this.userResponseAssembler.toResponseDto(existingUser);
    }

    /** Partially updates the user with the given id. */
    @Transactional
    public UserResponseDto patchUser(final Long id, final UserDto userDto) {
        final UserEntity existingUser = UserEntity.findById(id);
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
            existingUser.email = this.normalizeEmail(userDto.email);
        }
        if (userDto.banned != null) {
            existingUser.banned = userDto.banned;
        }
        if (userDto.activated != null) {
            existingUser.activated = userDto.activated;
        }

        this.applyPassword(existingUser, userDto.password);
        this.applyRank(existingUser, userDto.rankId, false);

        existingUser.persistAndFlush();
        return this.userResponseAssembler.toResponseDto(existingUser);
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
            throw new WebApplicationException("Invalid current password", Response.Status.UNAUTHORIZED);
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

    private void applyRank(final UserEntity user, @Nullable final Long rankId, final boolean useDefault) {
        if (rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + rankId + " not found");
            }
            user.rank = rank;
        } else if (useDefault) {
            if (this.defaultRankId == null) {
                throw new ValidationException("Default rank ID is not configured");
            }
            final UserRankEntity defaultRank = UserRankEntity.findById(this.defaultRankId);
            if (defaultRank == null) {
                throw new ValidationException("Default rank with ID " + this.defaultRankId + " not found");
            }
            user.rank = defaultRank;
        }
    }

    /** Deletes the user with the given id. */
    @Transactional
    public boolean deleteUser(final Long id) {
        return UserEntity.deleteById(id);
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
                WHERE LOWER(u.username) LIKE ?1 ESCAPE '\\' OR LOWER(u.email) LIKE ?1 ESCAPE '\\'""", searchTerm))
                .list();
        return this.userResponseAssembler.toResponseDtos(users);
    }
}
