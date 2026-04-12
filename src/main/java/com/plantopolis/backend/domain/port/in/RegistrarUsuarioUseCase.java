package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Usuario;

public interface RegistrarUsuarioUseCase {
    Usuario registrar(String nombre, String email, String password,
                      String telefono, String direccion);
}