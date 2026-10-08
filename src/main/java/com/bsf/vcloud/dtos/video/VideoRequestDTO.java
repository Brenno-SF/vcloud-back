package com.bsf.vcloud.dtos.video;

import com.bsf.vcloud.entity.Video;
import jakarta.validation.constraints.NotBlank;

public record VideoRequestDTO(
        @NotBlank  String originalFilename,
        @NotBlank String contentType,
        @NotBlank Long sizeBytes
) {}
