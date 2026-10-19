package com.bsf.vcloud.specification;

import com.bsf.vcloud.dtos.video.VideoFilterRequest;
import com.bsf.vcloud.entity.Video;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class VideoSpecification{
    public static Specification<Video> withFilter(UUID userId, VideoFilterRequest filter){
        return Specification
                .where(byUser(userId))
                .and(nameContains(filter))
                .and(statusEquals(filter))
                .and(createdBetween(filter));
    }

    private static Specification<Video> byUser(UUID userId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Video> createdBetween(VideoFilterRequest filter) {

        return (root, query, criteriaBuilder) -> {
            if (filter.createdAfter() == null && filter.createdBefore() == null) {
                return null;
            }
            if (filter.createdAfter() != null && filter.createdBefore() != null) {
                return criteriaBuilder.between(root.get("createdAt"), filter.createdAfter(), filter.createdBefore());
            }
            if (filter.createdAfter() != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.createdAfter());
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.createdBefore());
        };
    }

    private static Specification<Video> nameContains(VideoFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {

            if (filter.originalFilename() == null || filter.originalFilename().isBlank()) {
                return null;
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("originalFilename")), "%" + filter.originalFilename().toLowerCase() + "%");
        };
    }

    private static Specification<Video> statusEquals(VideoFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {

            if (filter.status() == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), filter.status());
        };
    }
}
