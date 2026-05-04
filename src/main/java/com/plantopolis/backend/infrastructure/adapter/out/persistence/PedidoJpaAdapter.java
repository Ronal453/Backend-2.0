package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adaptador de persistencia para el dominio Pedido.
 *
 * PROBLEMA RESUELTO:
 *   En PedidoService, el checkout ocurre dentro de una sola @Transactional.
 *   Hibernate mantiene un caché de primer nivel (sesión) donde guarda las
 *   entidades ya cargadas. Cuando guardamos Pago con pagoJpaRepo.save(),
 *   Hibernate lo sabe pero la relación PedidoEntity.pago puede quedar null
 *   en el caché porque el Pedido se guardó ANTES que el Pago.
 *
 *   Al llamar findByIdWithRelations(), Hibernate puede devolver el Pedido
 *   desde el caché (sin el Pago cargado) en vez de ir a BD.
 *
 * SOLUCIÓN:
 *   entityManager.clear() antes de la query → limpia el caché de primer nivel
 *   → Hibernate hace una query SQL real → carga el Pago con JOIN FETCH
 *   → metodo y estadoPago siempre disponibles.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/adapter/out/persistence/PedidoJpaAdapter.java
 */
@Component
@RequiredArgsConstructor
public class PedidoJpaAdapter implements PedidoRepositoryPort {

    // Repositorio JPA con queries JOIN FETCH
    private final PedidoJpaRepository pedidoRepo;

    // Repositorio de Pago para cargar el pago si la relación falla
    private final PagoJpaRepository pagoRepo;

    // Mapper entity → dominio
    private final PedidoMapper mapper;

    // EntityManager para limpiar el caché de primer nivel de Hibernate
    // Necesario para forzar una lectura fresca desde BD
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Busca un pedido por ID cargando TODAS sus relaciones desde BD.
     *
     * entityManager.clear() limpia el caché de Hibernate antes de la query.
     * Esto garantiza que el JOIN FETCH cargue el Pago real desde BD,
     * incluyendo su MetodoPago y EstadoPago, que pueden estar null
     * si Hibernate devuelve la entidad cacheada de antes del save del Pago.
     *
     * @param idPedido ID del pedido a buscar
     * @return Optional con el pedido completo del dominio
     */
    @Override
    public Optional<Pedido> buscarPorId(Long idPedido) {
        // Limpiar el caché de primer nivel de Hibernate (sesión actual)
        // Esto fuerza que la siguiente query vaya directamente a BD
        // y no devuelva la versión cacheada (que puede tener pago=null)
        entityManager.clear();

        // Ahora la query con JOIN FETCH carga todo desde BD fresco
        return pedidoRepo.findByIdWithRelations(idPedido)
                .map(mapper::toDomain);
    }

    /**
     * Busca todos los pedidos de un usuario con sus relaciones cargadas.
     * Ordenados del más reciente al más antiguo.
     *
     * @param idUsuario ID del usuario dueño de los pedidos
     * @return lista de pedidos completos del dominio
     */
    @Override
    public List<Pedido> buscarPorUsuario(Long idUsuario) {
        // Usar query con JOIN FETCH para historial completo
        return pedidoRepo.findByIdUsuarioWithRelations(idUsuario)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * Genera un número de pedido único con formato PL-YYYYMMDD-XXXX.
     * Reintenta si hay colisión (hasta 10 intentos).
     *
     * @return número de pedido único garantizado
     */
    @Override
    public String generarNumeroPedido() {
        // Fecha en formato YYYYMMDD para el número de pedido
        String fecha = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String numero;
        int intentos = 0;

        do {
            if (intentos++ > 10) {
                throw new RuntimeException(
                        "No se pudo generar número de pedido único");
            }
            // Sufijo aleatorio de 4 dígitos
            String rand = String.valueOf(
                    ThreadLocalRandom.current().nextInt(1000, 9999));
            numero = "PL-" + fecha + "-" + rand;

        } while (pedidoRepo.existsByNumeroPedido(numero));

        return numero;
    }
}