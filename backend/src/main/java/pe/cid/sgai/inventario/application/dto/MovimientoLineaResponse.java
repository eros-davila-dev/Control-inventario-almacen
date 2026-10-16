package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoLineaResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        Long loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        Long ubicacionId,
        String ubicacionCodigo,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        boolean fefoRespetado,
        String motivoNoFefo
) {
}
