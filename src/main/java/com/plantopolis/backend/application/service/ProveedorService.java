package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Proveedor;
import com.plantopolis.backend.domain.model.ReporteProveedor;
import com.plantopolis.backend.domain.port.in.GestionarProveedoresAdminUseCase;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación del módulo de proveedores (HU34 / HU35 — RF33 / RF34).
 * <p>
 * Reglas de negocio aplicadas:
 * <ul>
 *   <li>El nombre del proveedor es único (sin distinguir mayúsculas/minúsculas).</li>
 *   <li>El tipo de insumo debe pertenecer a {@link Proveedor#TIPOS_INSUMO_VALIDOS}.</li>
 *   <li>Desactivar un proveedor NO borra ni altera los lotes que ya lo referencian (trazabilidad histórica).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProveedorService implements GestionarProveedoresAdminUseCase {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final String ESTADO_DESCARTADO = "DESCARTADO";
    private static final String ESTADO_EN_TIENDA = "EN_TIENDA";

    private final ProveedorRepositoryPort proveedorRepository;
    private final LoteRepositoryPort loteRepository;

    /** {@inheritDoc} */
    @Override
    public List<Proveedor> listarProveedores(String termino, String tipoInsumo, Boolean activo) {
        // Normalizar filtros vacíos a null para que la consulta JPQL los ignore
        String terminoNormalizado = (termino != null && !termino.isBlank()) ? termino.trim() : null;
        String tipoNormalizado = (tipoInsumo != null && !tipoInsumo.isBlank()) ? tipoInsumo.trim().toUpperCase() : null;
        return proveedorRepository.buscarConFiltros(terminoNormalizado, tipoNormalizado, activo);
    }

    /** {@inheritDoc} */
    @Override
    public List<Proveedor> listarProveedoresActivos() {
        return proveedorRepository.listarActivos();
    }

    /** {@inheritDoc} */
    @Override
    public Proveedor obtenerProveedor(Long idProveedor) {
        return proveedorRepository.buscarPorId(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + idProveedor));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Proveedor crearProveedor(Proveedor proveedor) {
        normalizarYValidar(proveedor);

        if (proveedorRepository.existeProveedorPorNombre(proveedor.getNombre())) {
            throw new IllegalArgumentException("Ya existe un proveedor con el nombre: " + proveedor.getNombre());
        }

        // Todo proveedor nuevo nace activo y con fecha de auditoría en hora de Bogotá
        proveedor.setIdProveedor(null);
        proveedor.setActivo(true);
        proveedor.setFechaCreacion(LocalDateTime.now(ZONA_BOGOTA));
        return proveedorRepository.guardar(proveedor);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Proveedor actualizarProveedor(Long idProveedor, Proveedor proveedor) {
        Proveedor existente = obtenerProveedor(idProveedor);
        normalizarYValidar(proveedor);

        if (proveedorRepository.existeProveedorPorNombreYDistintoId(proveedor.getNombre(), idProveedor)) {
            throw new IllegalArgumentException("Ya existe otro proveedor con el nombre: " + proveedor.getNombre());
        }

        // Solo se copian campos editables; estado y fecha de creación se preservan
        existente.setNombre(proveedor.getNombre());
        existente.setContacto(proveedor.getContacto());
        existente.setTelefono(proveedor.getTelefono());
        existente.setCorreo(proveedor.getCorreo());
        existente.setTipoInsumo(proveedor.getTipoInsumo());
        return proveedorRepository.guardar(existente);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Proveedor activarProveedor(Long idProveedor) {
        Proveedor existente = obtenerProveedor(idProveedor);
        existente.setActivo(true);
        return proveedorRepository.guardar(existente);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Proveedor desactivarProveedor(Long idProveedor) {
        Proveedor existente = obtenerProveedor(idProveedor);
        // No se bloquea por lotes asociados: la desactivación es lógica y la trazabilidad se conserva
        existente.setActivo(false);
        return proveedorRepository.guardar(existente);
    }

    /** {@inheritDoc} */
    @Override
    public List<LoteProduccion> listarLotesPorProveedor(Long idProveedor) {
        // Validar existencia para devolver 400 claro en vez de lista vacía ambigua
        obtenerProveedor(idProveedor);
        return loteRepository.buscarLotesPorProveedor(idProveedor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Se cargan los lotes con proveedor una sola vez y se agrupan en memoria para evitar
     * N consultas (una por proveedor). Los proveedores sin lotes también aparecen con ceros.
     * El resultado se ordena por número de lotes descendente y luego por nombre.
     * </p>
     */
    @Override
    public List<ReporteProveedor> generarReporteLotesPorProveedor() {
        Map<Long, List<LoteProduccion>> lotesPorProveedor = loteRepository.buscarLotesConProveedor().stream()
                .filter(l -> l.getIdProveedor() != null)
                .collect(Collectors.groupingBy(LoteProduccion::getIdProveedor));

        return proveedorRepository.listarTodos().stream()
                .map(p -> construirReporte(p, lotesPorProveedor.getOrDefault(p.getIdProveedor(), List.of())))
                .sorted(Comparator.comparingLong(ReporteProveedor::getTotalLotes).reversed()
                        .thenComparing(ReporteProveedor::getNombreProveedor, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Calcula los indicadores de calidad de un proveedor a partir de sus lotes.
     *
     * @param proveedor proveedor evaluado
     * @param lotes     lotes asociados (puede ser vacía)
     * @return fila del reporte
     */
    private ReporteProveedor construirReporte(Proveedor proveedor, List<LoteProduccion> lotes) {
        long descartados = lotes.stream().filter(l -> ESTADO_DESCARTADO.equalsIgnoreCase(l.getEstadoLote())).count();
        long enTienda = lotes.stream().filter(l -> ESTADO_EN_TIENDA.equalsIgnoreCase(l.getEstadoLote())).count();
        long iniciales = lotes.stream().mapToLong(l -> valorSeguro(l.getCantidadInicial())).sum();
        long actuales = lotes.stream().mapToLong(l -> valorSeguro(l.getCantidadActual())).sum();

        // La pérdida se mide contra lo sembrado; si no hay plantas se reporta 0 para evitar división por cero
        double perdida = iniciales > 0 ? ((double) (iniciales - actuales) / iniciales) * 100 : 0.0;

        return ReporteProveedor.builder()
                .idProveedor(proveedor.getIdProveedor())
                .nombreProveedor(proveedor.getNombre())
                .tipoInsumo(proveedor.getTipoInsumo())
                .activo(proveedor.getActivo())
                .totalLotes(lotes.size())
                .lotesEnCultivo(lotes.size() - descartados - enTienda)
                .lotesEnTienda(enTienda)
                .lotesDescartados(descartados)
                .plantasIniciales(iniciales)
                .plantasActuales(actuales)
                .porcentajePerdida(Math.round(perdida * 100.0) / 100.0)
                .build();
    }

    /** Convierte un Integer posiblemente nulo en long seguro para sumatorias. */
    private long valorSeguro(Integer valor) {
        return valor != null ? valor : 0L;
    }

    /**
     * Limpia espacios, convierte cadenas vacías opcionales en null y valida campos obligatorios.
     *
     * @param proveedor datos recibidos (se modifican in-place)
     * @throws IllegalArgumentException si falta el nombre o el tipo de insumo es inválido
     */
    private void normalizarYValidar(Proveedor proveedor) {
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setContacto(textoOpcional(proveedor.getContacto()));
        proveedor.setTelefono(textoOpcional(proveedor.getTelefono()));
        String correo = textoOpcional(proveedor.getCorreo());
        proveedor.setCorreo(correo != null ? correo.toLowerCase() : null);

        String tipo = proveedor.getTipoInsumo() != null ? proveedor.getTipoInsumo().trim().toUpperCase() : "";
        if (!Proveedor.TIPOS_INSUMO_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de insumo inválido: '" + proveedor.getTipoInsumo()
                    + "'. Valores permitidos: " + Proveedor.TIPOS_INSUMO_VALIDOS);
        }
        proveedor.setTipoInsumo(tipo);
    }

    /** @return el texto recortado, o null si viene vacío. */
    private String textoOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
