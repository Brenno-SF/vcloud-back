package com.bsf.vcloud.dtos.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        String email,
        String password,
        LocalDateTime createdAt
) {
}
