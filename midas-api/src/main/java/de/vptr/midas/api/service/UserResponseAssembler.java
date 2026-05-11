package de.vptr.midas.api.service;

import java.util.List;
import java.util.Map;

import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.QueryAggregationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.enterprise.context.ApplicationScoped;

/** Builds user response DTOs with batched post/comment count lookups. */
@ApplicationScoped
public class UserResponseAssembler {

    /** Converts one user entity into a response DTO. */
    public UserResponseDto toResponseDto(final UserEntity user) {
        return this.toResponseDtos(List.of(user)).getFirst();
    }

    /** Converts user entities into response DTOs with shared count queries. */
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "ids is used multiple times in setParameter")
    public List<UserResponseDto> toResponseDtos(final List<UserEntity> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = users.stream().map(u -> u.id).toList();

        final Map<Long,
                Long> postCounts = QueryAggregationUtil.toLongCountMap(PostEntity.getEntityManager().createQuery(
                        "SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :ids GROUP BY p.user.id",
                        Object[].class).setParameter("ids", ids).getResultList());

        final Map<Long, Long> commentCounts = QueryAggregationUtil.toLongCountMap(PostCommentEntity.getEntityManager()
                .createQuery(
                        "SELECT c.user.id, COUNT(c) FROM PostCommentEntity c WHERE c.user.id IN :ids GROUP BY c.user.id",
                        Object[].class)
                .setParameter("ids", ids).getResultList());

        return users.stream().map(
                u -> new UserResponseDto(u, postCounts.getOrDefault(u.id, 0L), commentCounts.getOrDefault(u.id, 0L)))
                .toList();
    }
}
