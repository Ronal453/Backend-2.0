package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionarUsuariosAdminUseCase {

    /**
     * Lista usuarios con filtros opcionales.
     *
     * @param nombre filtro parcial por nombre o correo (puede ser null)
     * @param idRol  filtro por rol (1=ADMINISTRADOR, 2=TRABAJADOR, 3=CLIENTE; puede ser null)
     * @param activo filtro por estado de la cuenta (puede ser null = todos)
     */
    Page<Usuario> listarUsuarios(String nombre, Long idRol, Boolean activo, Pageable pageable);

    /** Reactiva el acceso de un usuario previamente desactivado. */
    Usuario activar(Long idUsuario);

    /** Revoca el acceso de un usuario. No borra su historial (pedidos, tareas, etc). */
    Usuario desactivar(Long idUsuario);

    /**
     * Genera una contraseña temporal aleatoria, la hashea y la persiste
     * reemplazando la anterior.
     *
     * @return la contraseña en texto plano, para que el admin la comunique
     *         una única vez al usuario de forma segura (no se vuelve a mostrar).
     */
    String resetearPassword(Long idUsuario);
}