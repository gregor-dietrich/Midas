package de.vptr.midas.api.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.security.PasswordHashingService;
import de.vptr.midas.api.util.EntityValidator;
import de.vptr.midas.api.util.SearchQueryUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
        return this.toResponseDtos(users);
    }

    /** Returns the user with the given username. */
    public Optional<UserResponseDto> findByUsername(@Nullable final String username) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.username = ?1", username)
                .<UserEntity>firstResultOptional().map(this::toResponseDto);
    }

    /** Returns the user with the given id. */
    public Optional<UserResponseDto> findById(final Long id) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id = ?1", id)
                .<UserEntity>firstResultOptional().map(this::toResponseDto);
    }

    /** Returns the user with the given email. */
    public Optional<UserResponseDto> findByEmail(@Nullable final String email) {
        return UserEntity.find("SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.email = ?1", email)
                .<UserEntity>firstResultOptional().map(this::toResponseDto);
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
        return this.toResponseDto(user);
    }

    @SuppressFBWarnings(value = "UC_USELESS_OBJECT", justification = "Entity populated then persisted via Panache;"
            + " SpotBugs cannot see Panache's bytecode-generated persist()")
    private UserEntity buildUserEntity(final UserDto userDto, @Nullable final String normalizedEmail) {
        final UserEntity user = new UserEntity();
        user.username = userDto.username;
        user.email = normalizedEmail;
        user.banned = userDto.banned != null ? userDto.banned : false;
        user.activated = userDto.activated != null ? userDto.activated : false;
        user.activationKey = userDto.activationKey != null ? userDto.activationKey : UUID.randomUUID().toString();
        user.lastIp = userDto.lastIp;

        // Generate salt and hash password
        final var salt = this.passwordHashingService.generateSalt();
        try {
            final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
            user.salt = salt;
            user.password = hashedPassword;
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
        }

        // Set rank if provided, otherwise default to rank 2 (User)
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            user.rank = rank;
        } else {
            if (this.defaultRankId == null) {
                throw new ValidationException("Default rank ID is not configured");
            }
            final UserRankEntity defaultRank = UserRankEntity.findById(this.defaultRankId);
            if (defaultRank == null) {
                throw new ValidationException("Default rank with ID " + this.defaultRankId + " not found");
            }
            user.rank = defaultRank;
        }

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
        existingUser.banned = userDto.banned != null ? userDto.banned : false;
        existingUser.activated = userDto.activated != null ? userDto.activated : false;
        // activationKey and lastIp are never settable via API — keep existing values

        // Handle password update if provided
        if (userDto.password != null && !userDto.password.trim().isEmpty()) {
            final var salt = this.passwordHashingService.generateSalt();
            try {
                final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
                existingUser.salt = salt;
                existingUser.password = hashedPassword;
            } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        // Set rank if provided
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            existingUser.rank = rank;
        } else {
            if (this.defaultRankId == null) {
                throw new ValidationException("Default rank ID is not configured");
            }
            final UserRankEntity defaultRank = UserRankEntity.findById(this.defaultRankId);
            if (defaultRank == null) {
                throw new ValidationException("Default rank with ID " + this.defaultRankId + " not found");
            }
            existingUser.rank = defaultRank;
        }

        existingUser.persistAndFlush();
        return this.toResponseDto(existingUser);
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
        // activationKey and lastIp are never settable via API

        // Handle password update if provided
        if (userDto.password != null && !userDto.password.trim().isEmpty()) {
            final var salt = this.passwordHashingService.generateSalt();
            try {
                final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
                existingUser.salt = salt;
                existingUser.password = hashedPassword;
            } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        // Set rank if provided
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            existingUser.rank = rank;
        }

        existingUser.persistAndFlush();
        return this.toResponseDto(existingUser);
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
        return this.toResponseDtos(users);
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
        return this.toResponseDtos(users);
    }

    private UserResponseDto toResponseDto(final UserEntity user) {
        return this.toResponseDtos(List.of(user)).get(0);
    }

    private List<UserResponseDto> toResponseDtos(final List<UserEntity> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = users.stream().map(u -> u.id).toList();

        final List<Object[]> postResults = PostEntity.getEntityManager()
                .createQuery("SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :ids GROUP BY p.user.id",
                        Object[].class)
                .setParameter("ids", ids).getResultList();
        final Map<Long, Long> postCounts = new HashMap<>();
        for (final Object[] row : postResults) {
            postCounts.put((Long) row[0], (Long) row[1]);
        }

        final List<Object[]> commentResults = PostCommentEntity.getEntityManager().createQuery(
                "SELECT c.user.id, COUNT(c) FROM PostCommentEntity c WHERE c.user.id IN :ids GROUP BY c.user.id",
                Object[].class).setParameter("ids", ids).getResultList();
        final Map<Long, Long> commentCounts = new HashMap<>();
        for (final Object[] row : commentResults) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }

        return users.stream().map(
                u -> new UserResponseDto(u, postCounts.getOrDefault(u.id, 0L), commentCounts.getOrDefault(u.id, 0L)))
                .toList();
    }
}
