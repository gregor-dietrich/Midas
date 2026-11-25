package de.vptr.midas.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserRankResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;

/** Service for user rank business logic. */
@ApplicationScoped
public class UserRankService {

    /** Refusal message for deleting the rank every new account gets. */
    public static final String DEFAULT_RANK_MESSAGE = "The default rank cannot be deleted";

    @Inject
    AdministratorGuard administratorGuard;

    @ConfigProperty(name = "midas.default-rank-public-id")
    @Nullable
    String defaultRankPublicId;

    /** Returns all ranks. */
    @CacheResult(cacheName = "user-ranks")
    public List<UserRankResponseDto> getAllRanks() {
        final List<UserRankEntity> ranks = UserRankEntity.listAll();
        return this.toRankResponseDtos(ranks);
    }

    /** Returns the rank with the given publicId. */
    public Optional<UserRankResponseDto> findById(final String publicId) {
        return UserRankEntity.find("publicId", publicId).firstResultOptional()
                .map(entity -> this.toRankResponseDto((UserRankEntity) entity));
    }

    /** Returns the rank with the given name. */
    public Optional<UserRankResponseDto> findByName(final String name) {
        return UserRankEntity.find("name", name).firstResultOptional()
                .map(entity -> this.toRankResponseDto((UserRankEntity) entity));
    }

    /** Returns all ranks matching the given query. */
    public List<UserRankResponseDto> searchRanks(@Nullable final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllRanks();
        }
        final List<UserRankEntity> ranks = SearchQueryUtil
                .applyDefaultSearchLimit(UserRankEntity.find("LOWER(name) LIKE ?1 ESCAPE '\\'", searchTerm)).list();
        return this.toRankResponseDtos(ranks);
    }

    /** Creates a new user rank without a caller, so without a {@link PrivilegeCeiling}. */
    public UserRankResponseDto createRank(final UserRankDto rankDto) {
        return this.createRank(rankDto, null);
    }

    /** Creates a new user rank within the caller's {@link PrivilegeCeiling}. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserRankResponseDto createRank(final UserRankDto rankDto, @Nullable final String currentUsername) {
        UserRankEntity rank = new UserRankEntity();
        rank.name = rankDto.name;
        this.mapPermissions(rank, rankDto);
        PrivilegeCeiling.of(currentUsername).requireCanGrant(rank);

        rank.persistAndFlush();
        rank = UserRankEntity.findById(rank.id);
        return new UserRankResponseDto(rank, 0L);
    }

    /** Updates the rank with the given publicId without a caller, so without a {@link PrivilegeCeiling}. */
    public UserRankResponseDto updateRank(final String publicId, final UserRankDto rankDto) {
        return this.updateRank(publicId, rankDto, null);
    }

    /**
     * Updates the rank with the given publicId within the caller's {@link PrivilegeCeiling}; permissions left out
     * become false.
     */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserRankResponseDto updateRank(final String publicId, final UserRankDto rankDto,
            @Nullable final String currentUsername) {
        return this.administratorGuard.guard(() -> {
            final PrivilegeCeiling ceiling = PrivilegeCeiling.of(currentUsername);
            final UserRankEntity existingRank = findForChange(publicId, ceiling);

            // Complete replacement (PUT semantics)
            existingRank.name = rankDto.name;
            this.mapPermissions(existingRank, rankDto);
            ceiling.requireCanGrant(existingRank);

            existingRank.persist();
            return this.toRankResponseDto(existingRank);
        });
    }

    /** Partially updates the rank with the given publicId without a caller, so without a {@link PrivilegeCeiling}. */
    public UserRankResponseDto patchRank(final String publicId, final UserRankDto rankDto) {
        return this.patchRank(publicId, rankDto, null);
    }

    /** Partially updates the rank with the given publicId within the caller's {@link PrivilegeCeiling}. */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public UserRankResponseDto patchRank(final String publicId, final UserRankDto rankDto,
            @Nullable final String currentUsername) {
        return this.administratorGuard.guard(() -> {
            final PrivilegeCeiling ceiling = PrivilegeCeiling.of(currentUsername);
            final UserRankEntity existingRank = findForChange(publicId, ceiling);

            // Partial update (PATCH semantics) - only update provided fields
            if (rankDto.name != null) {
                existingRank.name = rankDto.name;
            }
            this.patchPermissions(existingRank, rankDto);
            ceiling.requireCanGrant(existingRank);

            existingRank.persist();
            return this.toRankResponseDto(existingRank);
        });
    }

    /** Loads the rank a change applies to, refusing a missing one and one above the caller's ceiling. */
    private static UserRankEntity findForChange(final String publicId, final PrivilegeCeiling ceiling) {
        final UserRankEntity rank = UserRankEntity.find("publicId", publicId).firstResult();
        if (rank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }
        ceiling.requireCanManageRank(rank);
        return rank;
    }

    /** Full replacement — null defaults to false. Used by create/update. */
    private void mapPermissions(final UserRankEntity rank, final UserRankDto dto) {
        rank.adminView = booleanOrFalse(dto.adminView);
        rank.pageAdd = booleanOrFalse(dto.pageAdd);
        rank.pageDelete = booleanOrFalse(dto.pageDelete);
        rank.pageEdit = booleanOrFalse(dto.pageEdit);
        rank.postAdd = booleanOrFalse(dto.postAdd);
        rank.postDelete = booleanOrFalse(dto.postDelete);
        rank.postEdit = booleanOrFalse(dto.postEdit);
        rank.postCategoryAdd = booleanOrFalse(dto.postCategoryAdd);
        rank.postCategoryDelete = booleanOrFalse(dto.postCategoryDelete);
        rank.postCategoryEdit = booleanOrFalse(dto.postCategoryEdit);
        rank.postCommentAdd = booleanOrFalse(dto.postCommentAdd);
        rank.postCommentDelete = booleanOrFalse(dto.postCommentDelete);
        rank.postCommentEdit = booleanOrFalse(dto.postCommentEdit);
        rank.userAdd = booleanOrFalse(dto.userAdd);
        rank.userDelete = booleanOrFalse(dto.userDelete);
        rank.userEdit = booleanOrFalse(dto.userEdit);
        rank.userGroupAdd = booleanOrFalse(dto.userGroupAdd);
        rank.userGroupDelete = booleanOrFalse(dto.userGroupDelete);
        rank.userGroupEdit = booleanOrFalse(dto.userGroupEdit);
        rank.userAccountAdd = booleanOrFalse(dto.userAccountAdd);
        rank.userAccountDelete = booleanOrFalse(dto.userAccountDelete);
        rank.userAccountEdit = booleanOrFalse(dto.userAccountEdit);
        rank.userRankAdd = booleanOrFalse(dto.userRankAdd);
        rank.userRankDelete = booleanOrFalse(dto.userRankDelete);
        rank.userRankEdit = booleanOrFalse(dto.userRankEdit);
    }

    private static Boolean booleanOrFalse(@Nullable final Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }

    /** Partial update — only overwrites non-null fields. Used by patch. */
    private void patchPermissions(final UserRankEntity rank, final UserRankDto dto) {
        applyPermissionsFromDto(rank, dto);
    }

    private static Boolean keepExistingIfNull(@Nullable final Boolean value, final Boolean current) {
        return value != null ? value : current;
    }

    /** Applies non-null permission fields from the DTO to the entity. */
    private void applyPermissionsFromDto(final UserRankEntity rank, final UserRankDto dto) {
        rank.adminView = keepExistingIfNull(dto.adminView, rank.adminView);
        rank.pageAdd = keepExistingIfNull(dto.pageAdd, rank.pageAdd);
        rank.pageDelete = keepExistingIfNull(dto.pageDelete, rank.pageDelete);
        rank.pageEdit = keepExistingIfNull(dto.pageEdit, rank.pageEdit);
        rank.postAdd = keepExistingIfNull(dto.postAdd, rank.postAdd);
        rank.postDelete = keepExistingIfNull(dto.postDelete, rank.postDelete);
        rank.postEdit = keepExistingIfNull(dto.postEdit, rank.postEdit);
        rank.postCategoryAdd = keepExistingIfNull(dto.postCategoryAdd, rank.postCategoryAdd);
        rank.postCategoryDelete = keepExistingIfNull(dto.postCategoryDelete, rank.postCategoryDelete);
        rank.postCategoryEdit = keepExistingIfNull(dto.postCategoryEdit, rank.postCategoryEdit);
        rank.postCommentAdd = keepExistingIfNull(dto.postCommentAdd, rank.postCommentAdd);
        rank.postCommentDelete = keepExistingIfNull(dto.postCommentDelete, rank.postCommentDelete);
        rank.postCommentEdit = keepExistingIfNull(dto.postCommentEdit, rank.postCommentEdit);
        rank.userAdd = keepExistingIfNull(dto.userAdd, rank.userAdd);
        rank.userDelete = keepExistingIfNull(dto.userDelete, rank.userDelete);
        rank.userEdit = keepExistingIfNull(dto.userEdit, rank.userEdit);
        rank.userGroupAdd = keepExistingIfNull(dto.userGroupAdd, rank.userGroupAdd);
        rank.userGroupDelete = keepExistingIfNull(dto.userGroupDelete, rank.userGroupDelete);
        rank.userGroupEdit = keepExistingIfNull(dto.userGroupEdit, rank.userGroupEdit);
        rank.userAccountAdd = keepExistingIfNull(dto.userAccountAdd, rank.userAccountAdd);
        rank.userAccountDelete = keepExistingIfNull(dto.userAccountDelete, rank.userAccountDelete);
        rank.userAccountEdit = keepExistingIfNull(dto.userAccountEdit, rank.userAccountEdit);
        rank.userRankAdd = keepExistingIfNull(dto.userRankAdd, rank.userRankAdd);
        rank.userRankDelete = keepExistingIfNull(dto.userRankDelete, rank.userRankDelete);
        rank.userRankEdit = keepExistingIfNull(dto.userRankEdit, rank.userRankEdit);
    }

    /** Deletes the rank with the given publicId without a caller, so without a {@link PrivilegeCeiling}. */
    public boolean deleteRank(final String publicId) {
        return this.deleteRank(publicId, null);
    }

    /**
     * Deletes the rank with the given publicId within the caller's {@link PrivilegeCeiling}. The configured default
     * rank is never deleted, by anyone.
     */
    @Transactional
    @CacheInvalidateAll(cacheName = "user-ranks")
    public boolean deleteRank(final String publicId, @Nullable final String currentUsername) {
        final UserRankEntity rank = UserRankEntity.find("publicId", publicId).firstResult();
        if (rank == null) {
            return false;
        }
        if (publicId.equals(this.defaultRankPublicId)) {
            throw new ValidationException(DEFAULT_RANK_MESSAGE);
        }
        PrivilegeCeiling.of(currentUsername).requireCanManageRank(rank);
        return UserRankEntity.delete("publicId", publicId) > 0;
    }

    private UserRankResponseDto toRankResponseDto(final UserRankEntity rank) {
        final long usersCount = UserEntity.count("rank.id", rank.id);
        return new UserRankResponseDto(rank, usersCount);
    }

    private List<UserRankResponseDto> toRankResponseDtos(final List<UserRankEntity> ranks) {
        if (ranks.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = ranks.stream().map(r -> r.id).toList();

        final List<Object[]> countResults = UserEntity.getEntityManager()
                .createQuery("SELECT u.rank.id, COUNT(u) FROM UserEntity u WHERE u.rank.id IN :ids GROUP BY u.rank.id",
                        Object[].class)
                .setParameter("ids", ids).getResultList();
        final Map<Long, Long> userCounts = new HashMap<>();
        for (final Object[] row : countResults) {
            userCounts.put((Long) row[0], (Long) row[1]);
        }

        return ranks.stream().map(r -> new UserRankResponseDto(r, userCounts.getOrDefault(r.id, 0L))).toList();
    }
}
