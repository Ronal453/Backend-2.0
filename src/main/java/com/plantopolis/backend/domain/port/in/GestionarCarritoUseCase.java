package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Carrito;

public interface GestionarCarritoUseCase {

    // Ver carrito del usuario autenticado
    Carrito verCarrito(String email);

    // Agregar producto al carrito
    Carrito agregarItem(String email, Long idProducto, Integer cantidad);

    // Actualizar cantidad de un item
    Carrito actualizarCantidad(String email, Long idItem, Integer cantidad);

    // Eliminar item del carrito
    Carrito eliminarItem(String email, Long idItem);

    // Vaciar todo el carrito
    void vaciarCarrito(String email);
}