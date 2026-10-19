package com.bsf.vcloud.dtos.video;

import com.bsf.vcloud.enums.VideoStatus;

import java.time.LocalDateTime;

public record VideoFilterRequest(
        String originalFilename,
        VideoStatus status,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {}
