package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.domain.port.in.GestionarCarritoUseCase;
import com.plantopolis.backend.domain.port.out.CarritoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarritoService implements GestionarCarritoUseCase {

    private final CarritoRepositoryPort  carritoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final UsuarioRepositoryPort  usuarioRepository;

    // ── Ver carrito ──────────────────────────────────────────────────────────
    // Si el usuario no tiene carrito activo, lo crea vacío automáticamente
    @Override
    public Carrito verCarrito(String email) {
        var usuario = obtenerUsuario(email);
        return carritoRepository.buscarCarritoActivo(usuario.getIdUsuario())
                .orElseGet(() -> carritoRepository.crearCarrito(
                        usuario.getIdUsuario()));
    }

    // ── Agregar ítem ─────────────────────────────────────────────────────────
    // Valida que el producto exista, esté activo y tenga stock suficiente.
    // Si el producto ya está en el carrito, suma la cantidad en vez de duplicar.
    @Override
    @Transactional
    public Carrito agregarItem(String email, Long idProducto, Integer cantidad) {

        // 1. Validar producto y stock antes de cualquier escritura
        var producto = productoRepository.buscarPorId(idProducto)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado: " + idProducto));

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new RuntimeException("Producto no disponible");
        }
        if (producto.getStock() < cantidad) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + producto.getStock());
        }

        // 2. Obtener o crear el carrito activo del usuario
        var usuario = obtenerUsuario(email);
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseGet(() -> carritoRepository.crearCarrito(
                        usuario.getIdUsuario()));

        // 3. Si el producto ya existe en el carrito, sumar la cantidad
        var itemExistente = carritoRepository
                .buscarItem(carrito.getIdCarrito(), idProducto);

        if (itemExistente.isPresent()) {
            var item = itemExistente.get();
            int nuevaCantidad = item.getCantidad() + cantidad;

            // Revalidar stock con la cantidad acumulada
            if (producto.getStock() < nuevaCantidad) {
                throw new RuntimeException(
                        "Stock insuficiente. Disponible: " + producto.getStock());
            }
            item.setCantidad(nuevaCantidad);
            carritoRepository.guardarItem(item);
        } else {
            // 4. Si no existe, crear un ítem nuevo
            var nuevoItem = ItemCarrito.builder()
                    .idCarrito(carrito.getIdCarrito())
                    .idProducto(idProducto)
                    .cantidad(cantidad)
                    .build();
            carritoRepository.guardarItem(nuevoItem);
        }

        // 5. Devolver el carrito actualizado con datos frescos de BD
        return carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow();
    }

    // ── Actualizar cantidad ───────────────────────────────────────────────────
    // Si cantidad = 0, elimina el ítem. Valida stock antes de actualizar.
    //
    // SIN @Transactional aquí intencionalmente: así guardarItem() y el
    // verCarrito() posterior corren en transacciones separadas, garantizando
    // que verCarrito() abra una sesión de Hibernate completamente nueva y
    // lea datos frescos desde BD (sin caché de primer nivel contaminada).
    @Override
    public Carrito actualizarCantidad(String email, Long idItem, Integer cantidad) {

        // Si piden cantidad 0 o negativa, se interpreta como eliminar el ítem
        if (cantidad <= 0) {
            return eliminarItem(email, idItem);
        }

        // Buscar el ítem directamente por su ID (no por ID de carrito)
        var itemExistente = carritoRepository.buscarItemPorId(idItem)
                .orElseThrow(() -> new RuntimeException(
                        "Item no encontrado: " + idItem));

        // ── Validar ownership: el ítem debe pertenecer al carrito del usuario ──
        var usuario = obtenerUsuario(email);
        var carritoActivo = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("No tienes un carrito activo"));

        if (!itemExistente.getIdCarrito().equals(carritoActivo.getIdCarrito())) {
            throw new RuntimeException("No tienes permisos para modificar este ítem");
        }

        // Validar stock disponible para la nueva cantidad
        var producto = productoRepository
                .buscarPorId(itemExistente.getIdProducto())
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado"));

        if (producto.getStock() < cantidad) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + producto.getStock());
        }

        // Guardar el ítem con la cantidad actualizada
        // (el adapter hace refresh para limpiar caché de Hibernate)
        var itemActualizado = ItemCarrito.builder()
                .idItem(idItem)
                .idCarrito(itemExistente.getIdCarrito())
                .idProducto(itemExistente.getIdProducto())
                .cantidad(cantidad)
                .build();

        carritoRepository.guardarItem(itemActualizado);

        // verCarrito abre su propia transacción nueva → datos frescos garantizados
        return verCarrito(email);
    }

    // ── Eliminar ítem ────────────────────────────────────────────────────────
    // Elimina el ítem y devuelve el carrito actualizado
    @Override
    @Transactional
    public Carrito eliminarItem(String email, Long idItem) {
        carritoRepository.eliminarItem(idItem);
        return verCarrito(email);
    }

    // ── Vaciar carrito ───────────────────────────────────────────────────────
    // Elimina todos los ítems del carrito activo del usuario
    @Override
    @Transactional
    public void vaciarCarrito(String email) {
        var usuario = obtenerUsuario(email);
        carritoRepository.buscarCarritoActivo(usuario.getIdUsuario())
                .ifPresent(c -> carritoRepository.vaciarCarrito(
                        c.getIdCarrito()));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private com.plantopolis.backend.domain.model.Usuario obtenerUsuario(String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));
    }
}