package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String website   // campo honeypot: si llega con valor, es un bot
) {}
