package com.plantopolis.backend.domain.port.in;

public interface LoginGoogleUseCase {
    LoginGoogleResult loginConGoogle(String googleIdToken);
    LoginGoogleResult registrarConGoogle(String googleIdToken);

    record LoginGoogleResult(String token, String email, String rol) {}
}
