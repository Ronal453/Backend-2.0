package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.port.in.GestionarLotesTrabajadorUseCase;
import com.plantopolis.backend.domain.port.out.HistorialEstadoLoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrabajadorLoteService implements GestionarLotesTrabajadorUseCase {

    private final LoteRepositoryPort loteRepo;
    private final HistorialEstadoLoteRepositoryPort historialRepo;
    private final UsuarioRepositoryPort usuarioRepo;

    private static final String ESTADO_GERMINANDO = "GERMINANDO";
    private static final String ESTADO_CRECIENDO = "CRECIENDO";
    private static final String ESTADO_LISTO_VENTA = "LISTO_PARA_VENTA";
    private static final String ESTADO_DESCARTADO = "DESCARTADO";

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            ESTADO_GERMINANDO, ESTADO_CRECIENDO, ESTADO_LISTO_VENTA, ESTADO_DESCARTADO
    );

    @Override
    public Page<LoteProduccion> listarLotes(Long idZona, String estadoLote, Pageable pageable) {
        return loteRepo.buscarConFiltros(idZona, estadoLote, pageable);
    }

    @Override
    public LoteProduccion obtenerDetalleLote(Long idLote) {
        return loteRepo.buscarPorId(idLote)
                .orElseThrow(() -> new RuntimeException("Lote de producción no encontrado: " + idLote));
    }

    @Override
    @Transactional
    public LoteProduccion cambiarEstadoLote(Long idLote, String nuevoEstado, String observaciones, String emailUsuario) {
        var lote = obtenerDetalleLote(idLote);
        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + emailUsuario));

        String estadoActual = lote.getEstadoLote();
        String nuevo = nuevoEstado != null ? nuevoEstado.trim().toUpperCase() : "";

        if (!ESTADOS_VALIDOS.contains(nuevo)) {
            throw new RuntimeException("Estado de lote inválido: " + nuevoEstado);
        }

        if (estadoActual.equalsIgnoreCase(nuevo)) {
            return lote;
        }

        validarTransicionLote(estadoActual, nuevo);

        lote.setEstadoLote(nuevo);
        var loteActualizado = loteRepo.guardar(lote);

        var historial = HistorialEstadoLote.builder()
                .idLote(idLote)
                .idUsuario(usuario.getIdUsuario())
                .estadoAnterior(estadoActual)
                .estadoNuevo(nuevo)
                .observaciones(observaciones != null ? observaciones.trim() : null)
                .fechaCambio(LocalDateTime.now())
                .build();
        historialRepo.guardar(historial);

        return loteActualizado;
    }

    @Override
    public List<HistorialEstadoLote> obtenerHistorialLote(Long idLote) {
        return historialRepo.buscarPorIdLote(idLote);
    }

    private void validarTransicionLote(String origen, String destino) {
        if (ESTADO_DESCARTADO.equalsIgnoreCase(origen)) {
            throw new RuntimeException("Un lote descartado no puede cambiar a otro estado.");
        }

        if (ESTADO_DESCARTADO.equalsIgnoreCase(destino)) {
            return;
        }

        boolean valida = false;
        if (ESTADO_GERMINANDO.equalsIgnoreCase(origen) && ESTADO_CRECIENDO.equalsIgnoreCase(destino)) {
            valida = true;
        } else if (ESTADO_CRECIENDO.equalsIgnoreCase(origen) && ESTADO_LISTO_VENTA.equalsIgnoreCase(destino)) {
            valida = true;
        }

        if (!valida) {
            throw new RuntimeException(
                    "Transición de lote no permitida de " + origen + " a " + destino +
                    ". Flujo permitido: GERMINANDO -> CRECIENDO -> LISTO_PARA_VENTA (o DESCARTADO)."
            );
        }
    }
}
