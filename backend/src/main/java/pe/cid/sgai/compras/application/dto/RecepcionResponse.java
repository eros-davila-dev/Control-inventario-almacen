package pe.cid.sgai.compras.application.dto;

import pe.cid.sgai.compras.domain.ResultadoCalidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecepcionResponse(
        Long id,
        String numero,
        Long ordenId,
        String ordenNumero,
        Long almacenId,
        String almacenNombre,
        LocalDate fecha,
        String guiaRemision,
        String factura,
        String transportista,
        BigDecimal temperaturaLlegada,
        boolean empaqueIntegro,
        ResultadoCalidad resultadoCalidad,
        String resultadoDescripcion,
        String observacion,
        Long movimientoId,
        String recibidoPor,
        List<LineaRecepcionResponse> lineas
) {

    public record LineaRecepcionResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String codigoLote,
            LocalDate fechaVencimiento,
            Long loteId,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            boolean conforme,
            String observacion
    ) {
    }
}
