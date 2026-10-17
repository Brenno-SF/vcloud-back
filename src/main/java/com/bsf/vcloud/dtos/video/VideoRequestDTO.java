package com.bsf.vcloud.dtos.video;

import com.bsf.vcloud.dtos.thumb.ThumbDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VideoRequestDTO(
        @NotBlank  String originalFilename,
        @NotBlank String contentType,
        @NotNull Long sizeBytes,
        ThumbDTO thumb
) {}
