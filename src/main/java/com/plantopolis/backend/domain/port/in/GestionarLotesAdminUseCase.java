package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.LoteProduccion;

public interface GestionarLotesAdminUseCase {

    /**
     * HU13: Registrar lote de producción.
     * @param lote datos del nuevo lote
     * @param emailAdmin email del administrador que registra
     * @return LoteProduccion registrado
     */
    LoteProduccion registrarLote(LoteProduccion lote, String emailAdmin);

    /**
     * HU16: Vincular lote con producto del catálogo.
     * @param idLote identificador del lote a vincular
     * @param idProducto identificador del producto al que se suma stock
     * @param emailAdmin email del administrador que realiza la vinculación
     * @return LoteProduccion vinculado
     */
    LoteProduccion vincularLoteConProducto(Long idLote, Long idProducto, String emailAdmin);

    /**
     * HU35: Asociar (o desasociar) un lote existente con su proveedor de origen.
     * @param idLote identificador del lote
     * @param idProveedor proveedor a asignar; {@code null} elimina la asociación (campo opcional)
     * @return LoteProduccion actualizado
     */
    LoteProduccion asignarProveedorALote(Long idLote, Long idProveedor);

}
