package de.vptr.midas.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserRankResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for user rank business logic. */
@ApplicationScoped
public class UserRankService {

    /** Returns all ranks. */
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

    /** Creates a new user rank. */
    @Transactional
    public UserRankResponseDto createRank(final UserRankDto rankDto) {
        UserRankEntity rank = new UserRankEntity();
        rank.name = rankDto.name;
        this.mapPermissions(rank, rankDto);

        rank.persistAndFlush();
        rank = UserRankEntity.findById(rank.id);
        return new UserRankResponseDto(rank, 0L);
    }

    /** Processes the request and returns the result. */
    @Transactional
    public UserRankResponseDto updateRank(final String publicId, final UserRankDto rankDto) {
        final UserRankEntity existingRank = UserRankEntity.find("publicId", publicId).firstResult();
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingRank.name = rankDto.name;
        this.mapPermissions(existingRank, rankDto);

        existingRank.persist();
        return this.toRankResponseDto(existingRank);
    }

    /** Processes the request and returns the result. */
    @Transactional
    public UserRankResponseDto patchRank(final String publicId, final UserRankDto rankDto) {
        final UserRankEntity existingRank = UserRankEntity.find("publicId", publicId).firstResult();
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (rankDto.name != null) {
            existingRank.name = rankDto.name;
        }
        this.patchPermissions(existingRank, rankDto);

        existingRank.persist();
        return this.toRankResponseDto(existingRank);
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

    /** Applies non-null permission fields from the DTO to the entity. */
    @SuppressWarnings("PMD.NPathComplexity")
    private void applyPermissionsFromDto(final UserRankEntity rank, final UserRankDto dto) {
        if (dto.adminView != null) {
            rank.adminView = dto.adminView;
        }
        if (dto.pageAdd != null) {
            rank.pageAdd = dto.pageAdd;
        }
        if (dto.pageDelete != null) {
            rank.pageDelete = dto.pageDelete;
        }
        if (dto.pageEdit != null) {
            rank.pageEdit = dto.pageEdit;
        }
        if (dto.postAdd != null) {
            rank.postAdd = dto.postAdd;
        }
        if (dto.postDelete != null) {
            rank.postDelete = dto.postDelete;
        }
        if (dto.postEdit != null) {
            rank.postEdit = dto.postEdit;
        }
        if (dto.postCategoryAdd != null) {
            rank.postCategoryAdd = dto.postCategoryAdd;
        }
        if (dto.postCategoryDelete != null) {
            rank.postCategoryDelete = dto.postCategoryDelete;
        }
        if (dto.postCategoryEdit != null) {
            rank.postCategoryEdit = dto.postCategoryEdit;
        }
        if (dto.postCommentAdd != null) {
            rank.postCommentAdd = dto.postCommentAdd;
        }
        if (dto.postCommentDelete != null) {
            rank.postCommentDelete = dto.postCommentDelete;
        }
        if (dto.postCommentEdit != null) {
            rank.postCommentEdit = dto.postCommentEdit;
        }
        if (dto.userAdd != null) {
            rank.userAdd = dto.userAdd;
        }
        if (dto.userDelete != null) {
            rank.userDelete = dto.userDelete;
        }
        if (dto.userEdit != null) {
            rank.userEdit = dto.userEdit;
        }
        if (dto.userGroupAdd != null) {
            rank.userGroupAdd = dto.userGroupAdd;
        }
        if (dto.userGroupDelete != null) {
            rank.userGroupDelete = dto.userGroupDelete;
        }
        if (dto.userGroupEdit != null) {
            rank.userGroupEdit = dto.userGroupEdit;
        }
        if (dto.userAccountAdd != null) {
            rank.userAccountAdd = dto.userAccountAdd;
        }
        if (dto.userAccountDelete != null) {
            rank.userAccountDelete = dto.userAccountDelete;
        }
        if (dto.userAccountEdit != null) {
            rank.userAccountEdit = dto.userAccountEdit;
        }
        if (dto.userRankAdd != null) {
            rank.userRankAdd = dto.userRankAdd;
        }
        if (dto.userRankDelete != null) {
            rank.userRankDelete = dto.userRankDelete;
        }
        if (dto.userRankEdit != null) {
            rank.userRankEdit = dto.userRankEdit;
        }
    }

    /** Deletes the rank with the given publicId. */
    @Transactional
    public boolean deleteRank(final String publicId) {
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
