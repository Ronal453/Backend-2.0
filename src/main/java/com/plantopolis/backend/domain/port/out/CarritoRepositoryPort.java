package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import java.util.Optional;

public interface CarritoRepositoryPort {

    // Buscar carrito activo del usuario
    Optional<Carrito> buscarCarritoActivo(Long idUsuario);

    // Crear nuevo carrito
    Carrito crearCarrito(Long idUsuario);

    // Buscar item dentro del carrito
    Optional<ItemCarrito> buscarItem(Long idCarrito, Long idProducto);

     //buscar un ítem directamente por su ID
    Optional<ItemCarrito> buscarItemPorId(Long idItem);

    // Agregar o actualizar item
    ItemCarrito guardarItem(ItemCarrito item);

    // Eliminar item
    void eliminarItem(Long idItem);

    // Vaciar carrito completo
    void vaciarCarrito(Long idCarrito);

    // Buscar carrito por id
    Optional<Carrito> buscarPorId(Long idCarrito);

    // Cambiar estado del carrito
    void cambiarEstado(Long idCarrito, Long idEstado);
}