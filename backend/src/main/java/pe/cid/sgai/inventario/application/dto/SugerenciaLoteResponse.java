package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lote propuesto por la politica FEFO para atender una cantidad (HU-21). */
public record SugerenciaLoteResponse(
        Long loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        Long diasParaVencer,
        Long ubicacionId,
        String ubicacionCodigo,
        BigDecimal disponible,
        BigDecimal cantidadSugerida,
        BigDecimal costoUnitario
) {
}
