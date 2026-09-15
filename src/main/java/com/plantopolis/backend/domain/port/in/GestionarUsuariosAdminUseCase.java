package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionarUsuariosAdminUseCase {

    Page<Usuario> listarUsuarios(String nombre, Long idRol, Boolean activo, Pageable pageable);

    Usuario activar(Long idUsuario);

    Usuario desactivar(Long idUsuario);

    String resetearPassword(Long idUsuario);

    /**
     * Crea una nueva cuenta con rol TRABAJADOR. Solo puede ser invocado
     * por un usuario con rol ADMINISTRADOR (se valida en el controller
     * vía @PreAuthorize).
     *
     * @param nombreCompleto  nombre completo del trabajador
     * @param correo          correo único, será su usuario de login
     * @param passwordInicial contraseña inicial en texto plano (se hashea antes de guardar)
     * @throws RuntimeException si el correo ya está registrado
     */
    Usuario crearTrabajador(String nombreCompleto, String correo, String passwordInicial);
}