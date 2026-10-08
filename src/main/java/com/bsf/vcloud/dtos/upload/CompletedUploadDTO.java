package com.bsf.vcloud.dtos.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompletedUploadDTO(
        @NotBlank String uploadId,
        @NotBlank String key,
        @NotNull List<PartDTO> parts
) {
}
