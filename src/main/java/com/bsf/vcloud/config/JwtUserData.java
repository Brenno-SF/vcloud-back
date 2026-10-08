package com.bsf.vcloud.config;

import lombok.Builder;

import java.util.UUID;

@Builder
public record JwtUserData(UUID id, String name, String email) {
}
