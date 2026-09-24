package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank String idToken
) {}
