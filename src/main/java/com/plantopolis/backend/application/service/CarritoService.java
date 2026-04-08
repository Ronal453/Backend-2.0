package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.domain.port.in.GestionarCarritoUseCase;
import com.plantopolis.backend.domain.port.out.CarritoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarritoService implements GestionarCarritoUseCase {

    private final CarritoRepositoryPort carritoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final UsuarioRepositoryPort usuarioRepository;

    // ── Ver carrito ──────────────────────────────────────────
    @Override
    public Carrito verCarrito(String email) {
        var usuario = obtenerUsuario(email);
        return carritoRepository.buscarCarritoActivo(usuario.getIdUsuario())
                .orElseGet(() -> carritoRepository.crearCarrito(
                        usuario.getIdUsuario()));
    }

    // ── Agregar item ─────────────────────────────────────────
    @Override
    @Transactional
    public Carrito agregarItem(String email, Long idProducto, Integer cantidad) {

        // 1. Validar que el producto existe y tiene stock
        var producto = productoRepository.buscarPorId(idProducto)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado: " + idProducto));

        if (!producto.getActivo()) {
            throw new RuntimeException("Producto no disponible");
        }
        if (producto.getStock() < cantidad) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + producto.getStock());
        }

        // 2. Obtener o crear carrito activo
        var usuario = obtenerUsuario(email);
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseGet(() -> carritoRepository
                        .crearCarrito(usuario.getIdUsuario()));

        // 3. Si el producto ya está en el carrito → sumar cantidad
        var itemExistente = carritoRepository
                .buscarItem(carrito.getIdCarrito(), idProducto);

        if (itemExistente.isPresent()) {
            var item = itemExistente.get();
            int nuevaCantidad = item.getCantidad() + cantidad;

            if (producto.getStock() < nuevaCantidad) {
                throw new RuntimeException(
                        "Stock insuficiente. Disponible: " + producto.getStock());
            }
            item.setCantidad(nuevaCantidad);
            carritoRepository.guardarItem(item);
        } else {
            // 4. Si no existe → crear nuevo item
            var nuevoItem = ItemCarrito.builder()
                    .idCarrito(carrito.getIdCarrito())
                    .idProducto(idProducto)
                    .cantidad(cantidad)
                    .build();
            carritoRepository.guardarItem(nuevoItem);
        }

        // 5. Devolver carrito actualizado
        return carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow();
    }

    // ── Actualizar cantidad ──────────────────────────────────
    @Override
    @Transactional
    public Carrito actualizarCantidad(String email, Long idItem, Integer cantidad) {

        if (cantidad <= 0) {
            return eliminarItem(email, idItem);
        }

        var item = carritoRepository.buscarPorId(idItem)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        // Validar stock
        var producto = productoRepository
                .buscarPorId(obtenerItemPorId(idItem).getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStock() < cantidad) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + producto.getStock());
        }

        var itemActualizado = ItemCarrito.builder()
                .idItem(idItem)
                .idCarrito(item.getIdCarrito())
                .idProducto(obtenerItemPorId(idItem).getIdProducto())
                .cantidad(cantidad)
                .build();

        carritoRepository.guardarItem(itemActualizado);
        return verCarrito(email);
    }

    // ── Eliminar item ────────────────────────────────────────
    @Override
    @Transactional
    public Carrito eliminarItem(String email, Long idItem) {
        carritoRepository.eliminarItem(idItem);
        return verCarrito(email);
    }

    // ── Vaciar carrito ───────────────────────────────────────
    @Override
    @Transactional
    public void vaciarCarrito(String email) {
        var usuario = obtenerUsuario(email);
        carritoRepository.buscarCarritoActivo(usuario.getIdUsuario())
                .ifPresent(c -> carritoRepository.vaciarCarrito(c.getIdCarrito()));
    }

    // ── Helpers ──────────────────────────────────────────────
    private com.plantopolis.backend.domain.model.Usuario obtenerUsuario(String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private ItemCarrito obtenerItemPorId(Long idItem) {
        return carritoRepository.buscarPorId(idItem)
                .map(c -> c.getItems().stream()
                        .filter(i -> i.getIdItem().equals(idItem))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Item no encontrado")))
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));
    }
}