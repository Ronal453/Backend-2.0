package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.CausaMerma;
import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.Merma;
import com.plantopolis.backend.domain.port.in.GestionarMermasTrabajadorUseCase;
import com.plantopolis.backend.domain.port.out.CausaMermaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialEstadoLoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.MermaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrabajadorMermaService implements GestionarMermasTrabajadorUseCase {

    private final MermaRepositoryPort mermaRepo;
    private final CausaMermaRepositoryPort causaRepo;
    private final LoteRepositoryPort loteRepo;
    private final HistorialEstadoLoteRepositoryPort historialLoteRepo;
    private final UsuarioRepositoryPort usuarioRepo;

    @Override
    public List<CausaMerma> listarCausasMerma() {
        return causaRepo.listarActivas();
    }

    @Override
    @Transactional
    public Merma registrarMerma(Long idLote, Long idCausa, Integer cantidadPerdida,
                                LocalDate fechaMerma, String observaciones, String emailUsuario) {

        if (cantidadPerdida == null || cantidadPerdida <= 0) {
            throw new RuntimeException("La cantidad perdida debe ser mayor a cero.");
        }

        var lote = loteRepo.buscarPorId(idLote)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + idLote));

        var causa = causaRepo.buscarPorId(idCausa)
                .orElseThrow(() -> new RuntimeException("Causa de merma no encontrada: " + idCausa));

        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + emailUsuario));

        if (cantidadPerdida > lote.getCantidadActual()) {
            throw new RuntimeException(
                    "La cantidad de merma (" + cantidadPerdida +
                    ") supera la cantidad actual disponible en el lote (" + lote.getCantidadActual() + ")."
            );
        }

        int nuevaCantidadActual = lote.getCantidadActual() - cantidadPerdida;
        lote.setCantidadActual(nuevaCantidadActual);

        if (nuevaCantidadActual == 0 && !"DESCARTADO".equalsIgnoreCase(lote.getEstadoLote())) {
            String estadoAnterior = lote.getEstadoLote();
            lote.setEstadoLote("DESCARTADO");

            var historial = HistorialEstadoLote.builder()
                    .idLote(idLote)
                    .idUsuario(usuario.getIdUsuario())
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo("DESCARTADO")
                    .observaciones("Descartado automáticamente: la merma redujo la cantidad a 0.")
                    .fechaCambio(LocalDateTime.now())
                    .build();
            historialLoteRepo.guardar(historial);
        }

        loteRepo.guardar(lote);

        var merma = Merma.builder()
                .idLote(idLote)
                .idCausa(causa.getIdCausa())
                .idUsuario(usuario.getIdUsuario())
                .cantidadPerdida(cantidadPerdida)
                .fechaMerma(fechaMerma != null ? fechaMerma : LocalDate.now())
                .observaciones(observaciones != null ? observaciones.trim() : null)
                .fechaRegistro(LocalDateTime.now())
                .build();

        return mermaRepo.guardar(merma);
    }

    @Override
    public List<Merma> listarMermasRecientes(int limite) {
        return mermaRepo.listarRecientes(limite > 0 ? limite : 20);
    }
}
