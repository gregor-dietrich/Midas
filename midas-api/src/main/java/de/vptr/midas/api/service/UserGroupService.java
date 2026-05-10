package de.vptr.midas.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.vptr.midas.api.dto.UserGroupDto;
import de.vptr.midas.api.dto.UserGroupResponseDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserGroupEntity;
import de.vptr.midas.api.entity.UserGroupMetaEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for user group business logic. */
@ApplicationScoped
public class UserGroupService {

    /** Returns all groups with default pagination. */
    public List<UserGroupResponseDto> getAllGroups() {
        return getAllGroups(0, 20);
    }

    /** Returns all groups with pagination. */
    public List<UserGroupResponseDto> getAllGroups(final int page, final int size) {
        final List<UserGroupEntity> groups = UserGroupEntity.findAll().page(page, size).list();
        return this.toGroupResponseDtos(groups);
    }

    /** Returns the group with the given id. */
    public Optional<UserGroupResponseDto> findById(final Long id) {
        return UserGroupEntity.findByIdOptional(id).map(entity -> this.toGroupResponseDto((UserGroupEntity) entity));
    }

    /** Returns the group with the given name. */
    public Optional<UserGroupResponseDto> findByName(final String name) {
        return Optional.ofNullable(UserGroupEntity.findByName(name)).map(this::toGroupResponseDto);
    }

    /** Returns all users in the given group. */
    public List<UserResponseDto> getUsersInGroup(final Long groupId) {
        final UserGroupEntity group = UserGroupEntity.findById(groupId);
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }
        final List<UserEntity> users = UserEntity.find("""
                SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id IN \
                (SELECT m.user.id FROM UserGroupMetaEntity m WHERE m.group.id = ?1)""", groupId).list();
        return this.toUserResponseDtos(users);
    }

    /** Returns all groups that the given user belongs to. */
    public List<UserGroupResponseDto> getGroupsForUser(final Long userId) {
        final List<UserGroupEntity> groups = UserGroupEntity.find("""
                SELECT g FROM UserGroupEntity g WHERE g.id IN \
                (SELECT m.group.id FROM UserGroupMetaEntity m WHERE m.user.id = ?1)""", userId).list();
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

    /** Updates the group with the given id. */
    @Transactional
    public UserGroupResponseDto updateGroup(final Long id, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = UserGroupEntity.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingGroup.name = groupDto.name;
        existingGroup.persistAndFlush();

        return this.toGroupResponseDto(existingGroup);
    }

    /** Partially updates the group with the given id. */
    @Transactional
    public UserGroupResponseDto patchGroup(final Long id, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = UserGroupEntity.findById(id);
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

    /** Deletes the group with the given id. */
    @Transactional
    public boolean deleteGroup(final Long id) {
        return UserGroupEntity.deleteById(id);
    }

    /** Adds a user to a group. */
    @Transactional
    public UserGroupMetaEntity addUserToGroup(final Long userId, final Long groupId) {
        // Check if association already exists
        if (UserGroupMetaEntity.isUserInGroup(userId, groupId)) {
            throw new WebApplicationException("User is already in this group", Response.Status.CONFLICT);
        }

        final UserEntity user = UserEntity.findById(userId);
        final UserGroupEntity group = UserGroupEntity.findById(groupId);

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        final var meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;
        meta.persistAndFlush();
        return meta;
    }

    /** Removes a user from a group. */
    @Transactional
    public boolean removeUserFromGroup(final Long userId, final Long groupId) {
        return UserGroupMetaEntity.delete("user.id = ?1 AND group.id = ?2", userId, groupId) > 0;
    }

    /** Returns whether the given user is in the given group. */
    public boolean isUserInGroup(final Long userId, final Long groupId) {
        return UserGroupMetaEntity.isUserInGroup(userId, groupId);
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
        final Map<Long, Long> userCounts = new HashMap<>();
        for (final Object[] row : countResults) {
            userCounts.put((Long) row[0], (Long) row[1]);
        }

        return groups.stream().map(g -> new UserGroupResponseDto(g, userCounts.getOrDefault(g.id, 0L))).toList();
    }

    private List<UserResponseDto> toUserResponseDtos(final List<UserEntity> users) {
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
