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

}
