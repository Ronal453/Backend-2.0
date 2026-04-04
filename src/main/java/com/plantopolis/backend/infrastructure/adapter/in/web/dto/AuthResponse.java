package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

public record AuthResponse(
    String token,
    String email,
    String rol,
    String mensaje
) {}
