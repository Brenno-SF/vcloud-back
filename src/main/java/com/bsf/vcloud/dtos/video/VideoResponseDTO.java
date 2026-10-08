package com.bsf.vcloud.dtos.video;

import com.bsf.vcloud.entity.Video;
import com.bsf.vcloud.enums.VideoStatus;

import java.time.LocalDateTime;

public record VideoResponseDTO(
         String originalFilename,
         String contentType,
         Long sizeBytes,
         VideoStatus status,
         LocalDateTime createdAt,
         LocalDateTime updatedAt
) {
    public VideoResponseDTO(Video video) {
        this(
                video.getOriginalFilename(),
                video.getContentType(),
                video.getSizeBytes(),
                video.getStatus(),
                video.getCreatedAt(),
                video.getUpdatedAt()
        );
    }
}
