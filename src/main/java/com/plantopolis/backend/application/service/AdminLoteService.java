package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.domain.port.in.GestionarLotesAdminUseCase;
import com.plantopolis.backend.domain.port.out.HistorialEstadoLoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminLoteService implements GestionarLotesAdminUseCase {

    private final LoteRepositoryPort loteRepository;
    private final ZonaRepositoryPort zonaRepository;
    private final ProductoRepositoryPort productoRepository;
    private final HistorialEstadoLoteRepositoryPort historialEstadoLoteRepository;

    private final UsuarioRepositoryPort usuarioRepository;

    private static final String ESTADO_GERMINANDO = "GERMINANDO";
    private static final String ESTADO_LISTO_VENTA = "LISTO_PARA_VENTA";
    private static final String ESTADO_DESCARTADO = "DESCARTADO";

    @Override
    @Transactional
    public LoteProduccion registrarLote(LoteProduccion lote, String emailAdmin) {
        // Buscar el usuario administrador
        var usuario = usuarioRepository.buscarPorEmail(emailAdmin)
                .orElseThrow(() -> new IllegalArgumentException("Usuario administrador no encontrado: " + emailAdmin));

        // Validación de zona y capacidad
        Zona zona = zonaRepository.buscarPorId(lote.getIdZona())
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        long lotesEnZona = loteRepository.contarLotesActivosPorZona(zona.getIdZona());
        if (zona.getCapacidadMaxima() != null && lotesEnZona >= zona.getCapacidadMaxima()) {
            throw new IllegalStateException("La zona no tiene capacidad disponible para más lotes");
        }

        String estadoInicial = (lote.getEstadoLote() != null && !lote.getEstadoLote().trim().isEmpty()) 
                                ? lote.getEstadoLote().trim().toUpperCase() 
                                : ESTADO_GERMINANDO;

        if (!java.util.Set.of(ESTADO_GERMINANDO, "CRECIENDO", ESTADO_LISTO_VENTA, ESTADO_DESCARTADO).contains(estadoInicial)) {
            throw new IllegalArgumentException("Estado de lote inválido: " + estadoInicial);
        }

        // Valores iniciales
        lote.setEstadoLote(estadoInicial);
        lote.setCantidadActual(lote.getCantidadInicial());
        lote.setEsVinculado(false);
        lote.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        
        // Asignar fecha de siembra si no viene en el payload
        if (lote.getFechaSiembra() == null) {
            lote.setFechaSiembra(lote.getFechaCreacion().toLocalDate());
        }

        LoteProduccion loteGuardado = loteRepository.guardar(lote);

        // Registrar historial inicial
        HistorialEstadoLote historial = HistorialEstadoLote.builder()
                .idLote(loteGuardado.getIdLote())
                .idUsuario(usuario.getIdUsuario())
                .estadoAnterior("NINGUNO")
                .estadoNuevo(estadoInicial)
                .observaciones("Registro inicial del lote")
                .fechaCambio(loteGuardado.getFechaCreacion())
                .build();
        historialEstadoLoteRepository.guardar(historial);

        return loteGuardado;
    }

    @Override
    @Transactional
    public LoteProduccion vincularLoteConProducto(Long idLote, Long idProducto) {
        LoteProduccion lote = loteRepository.buscarPorId(idLote)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        // HU16b: Bloquear vinculación de lote descartado
        if (ESTADO_DESCARTADO.equalsIgnoreCase(lote.getEstadoLote())) {
            throw new IllegalStateException("No se puede vincular un lote en estado DESCARTADO");
        }

        // HU16: Solo lotes en estado LISTO_PARA_VENTA pueden vincularse
        if (!ESTADO_LISTO_VENTA.equalsIgnoreCase(lote.getEstadoLote())) {
            throw new IllegalStateException("El lote debe estar en estado LISTO_PARA_VENTA para vincularse");
        }

        if (Boolean.TRUE.equals(lote.getEsVinculado())) {
            throw new IllegalStateException("El lote ya fue vinculado previamente");
        }

        Producto producto = productoRepository.buscarPorId(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        // Sumar la cantidad del lote al stock del producto
        producto.setStock(producto.getStock() + lote.getCantidadActual());
        productoRepository.guardar(producto);

        // Marcar lote como vinculado
        lote.setEsVinculado(true);
        lote.setIdProducto(producto.getIdProducto());
        lote.setNombreProducto(producto.getNombreProducto());
        lote.setFechaVinculacion(LocalDateTime.now(ZoneId.of("America/Bogota")));

        return loteRepository.guardar(lote);
    }
}
