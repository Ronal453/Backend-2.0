package com.plantopolis.backend.infrastructure.adapter.in.web;

public record RegistroRequest(
    String nombre,
    String email,
    String password
) {}