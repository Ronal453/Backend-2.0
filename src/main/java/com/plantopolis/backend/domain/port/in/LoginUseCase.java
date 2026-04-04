package com.plantopolis.backend.domain.port.in;

public interface LoginUseCase {
    String login(String email, String password);
}
