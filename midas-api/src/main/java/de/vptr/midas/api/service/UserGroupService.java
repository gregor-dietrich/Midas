package de.vptr.midas.api.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.vptr.midas.api.dto.UserGroupDto;
import de.vptr.midas.api.dto.UserGroupResponseDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserGroupEntity;
import de.vptr.midas.api.entity.UserGroupMetaEntity;
import de.vptr.midas.api.util.QueryAggregationUtil;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for user group business logic. */
@ApplicationScoped
public class UserGroupService {

    @Inject
    UserResponseAssembler userResponseAssembler;

    /** Returns all groups with default pagination. */
    public List<UserGroupResponseDto> getAllGroups() {
        return this.getAllGroups(0, 20);
    }

    /** Returns all groups with pagination. */
    public List<UserGroupResponseDto> getAllGroups(final int page, final int size) {
        final List<UserGroupEntity> groups = UserGroupEntity.findAll().page(page, size).list();
        return this.toGroupResponseDtos(groups);
    }

    /** Returns the group with the given publicId. */
    public Optional<UserGroupResponseDto> findById(final String publicId) {
        return UserGroupEntity.find("publicId", publicId).firstResultOptional()
                .map(entity -> this.toGroupResponseDto((UserGroupEntity) entity));
    }

    /** Returns the group with the given name. */
    public Optional<UserGroupResponseDto> findByName(final String name) {
        return Optional.ofNullable(UserGroupEntity.findByName(name)).map(this::toGroupResponseDto);
    }

    /** Returns all users in the given group publicId. */
    public List<UserResponseDto> getUsersInGroup(final String groupPublicId) {
        final UserGroupEntity group = UserGroupEntity.find("publicId", groupPublicId).firstResult();
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }
        final List<UserEntity> users = UserEntity.find("""
                SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id IN \
                (SELECT m.user.id FROM UserGroupMetaEntity m WHERE m.group.id = ?1)""", group.id).list();
        return this.userResponseAssembler.toResponseDtos(users);
    }

    /** Returns all groups that the given user belongs to. */
    public List<UserGroupResponseDto> getGroupsForUser(final String userPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        final List<UserGroupEntity> groups = UserGroupEntity.find("""
                SELECT g FROM UserGroupEntity g WHERE g.id IN \
                (SELECT m.group.id FROM UserGroupMetaEntity m WHERE m.user.id = ?1)""", user.id).list();
        return this.toGroupResponseDtos(groups);
    }

    /** Creates a new user group. */
    @Transactional
    public UserGroupResponseDto createGroup(final UserGroupDto groupDto) {
        final UserGroupEntity group = new UserGroupEntity();
        group.name = groupDto.name;
        group.persistAndFlush();

        final UserGroupEntity savedGroup = UserGroupEntity.findById(group.id);
        return new UserGroupResponseDto(savedGroup, 0L);
    }

    /** Updates the group with the given publicId. */
    @Transactional
    public UserGroupResponseDto updateGroup(final String publicId, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = UserGroupEntity.find("publicId", publicId).firstResult();
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingGroup.name = groupDto.name;
        existingGroup.persistAndFlush();

        return this.toGroupResponseDto(existingGroup);
    }

    /** Partially updates the group with the given publicId. */
    @Transactional
    public UserGroupResponseDto patchGroup(final String publicId, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = UserGroupEntity.find("publicId", publicId).firstResult();
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (groupDto.name != null && !groupDto.name.trim().isEmpty()) {
            existingGroup.name = groupDto.name;
        }

        existingGroup.persistAndFlush();
        return this.toGroupResponseDto(existingGroup);
    }

    /** Deletes the group with the given publicId. */
    @Transactional
    public boolean deleteGroup(final String publicId) {
        return UserGroupEntity.delete("publicId", publicId) > 0;
    }

    /** Adds a user to a group. */
    @Transactional
    public UserGroupMetaEntity addUserToGroup(final String userPublicId, final String groupPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final UserGroupEntity group = UserGroupEntity.find("publicId", groupPublicId).firstResult();

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Check if association already exists
        if (UserGroupMetaEntity.isUserInGroup(Objects.requireNonNull(user.id), Objects.requireNonNull(group.id))) {
            throw new WebApplicationException("User is already in this group", Response.Status.CONFLICT);
        }

        final var meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;
        meta.persistAndFlush();
        return meta;
    }

    /** Removes a user from a group. */
    @Transactional
    public boolean removeUserFromGroup(final String userPublicId, final String groupPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final UserGroupEntity group = UserGroupEntity.find("publicId", groupPublicId).firstResult();

        if (user == null || group == null) {
            return false;
        }

        return UserGroupMetaEntity.delete("user.id = ?1 AND group.id = ?2", Objects.requireNonNull(user.id),
                Objects.requireNonNull(group.id)) > 0;
    }

    /** Returns whether the given user is in the given group. */
    public boolean isUserInGroup(final String userPublicId, final String groupPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final UserGroupEntity group = UserGroupEntity.find("publicId", groupPublicId).firstResult();

        if (user == null || group == null) {
            return false;
        }

        return UserGroupMetaEntity.isUserInGroup(Objects.requireNonNull(user.id), Objects.requireNonNull(group.id));
    }

    /** Returns all groups matching the given query. */
    public List<UserGroupResponseDto> searchGroups(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllGroups();
        }
        final List<UserGroupEntity> groups = SearchQueryUtil
                .applyDefaultSearchLimit(UserGroupEntity.find("LOWER(name) LIKE ?1 ESCAPE '\\'", searchTerm)).list();
        return this.toGroupResponseDtos(groups);
    }

    private UserGroupResponseDto toGroupResponseDto(final UserGroupEntity group) {
        final long userCount = UserGroupMetaEntity.count("group.id", group.id);
        return new UserGroupResponseDto(group, userCount);
    }

    private List<UserGroupResponseDto> toGroupResponseDtos(final List<UserGroupEntity> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = groups.stream().map(g -> g.id).toList();

        final List<Object[]> countResults = UserGroupMetaEntity.getEntityManager().createQuery(
                "SELECT m.group.id, COUNT(m) FROM UserGroupMetaEntity m WHERE m.group.id IN :ids GROUP BY m.group.id",
                Object[].class).setParameter("ids", ids).getResultList();
        final Map<Long, Long> userCounts = QueryAggregationUtil.toLongCountMap(countResults);

        return groups.stream().map(g -> new UserGroupResponseDto(g, userCounts.getOrDefault(g.id, 0L))).toList();
    }
}
