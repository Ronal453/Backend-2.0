package com.plantopolis.backend.domain.port.in;

/**
 * Puerto de entrada primario para el caso de uso de recuperación y restablecimiento de contraseña (HU2).
 * Permite a cualquier usuario solicitar un enlace de recuperación seguro por correo
 * y posteriormente restablecer su contraseña mediante un token temporal válido.
 */
public interface RecuperarPasswordUseCase {

    /**
     * Procesa la solicitud inicial de olvido de contraseña. Si el correo existe y está activo,
     * genera un token seguro y envía el correo con las instrucciones de restablecimiento.
     *
     * @param email Correo electrónico proporcionado por el usuario
     */
    void solicitarRecuperacion(String email);

    /**
     * Valida el token temporal y actualiza la contraseña del usuario con un nuevo hash seguro.
     *
     * @param token Token JWT temporal de recuperación recibido por el usuario
     * @param nuevaPassword Nueva contraseña en texto plano para validar y cifrar
     */
    void restablecerPassword(String token, String nuevaPassword);
}
