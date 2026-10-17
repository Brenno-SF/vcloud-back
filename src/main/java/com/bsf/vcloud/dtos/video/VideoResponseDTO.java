package com.bsf.vcloud.dtos.video;

import com.bsf.vcloud.enums.VideoStatus;

import java.io.Serializable;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public record VideoResponseDTO(
         UUID id,
         String originalFilename,
         String contentType,
         Long sizeBytes,
         VideoStatus status,
         LocalDateTime createdAt,
         LocalDateTime updatedAt,
         URL thumbnailUrl
) implements Serializable {}
