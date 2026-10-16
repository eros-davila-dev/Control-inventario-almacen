package pe.cid.sgai.despacho.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DespachoResponse(
        Long id,
        String numero,
        Long requisicionId,
        String requisicionNumero,
        Long almacenId,
        String almacenNombre,
        String area,
        String centroCosto,
        LocalDate fecha,
        Long movimientoId,
        String entregadoA,
        String despachadoPor,
        String observacion,
        BigDecimal valorTotal,
        List<LineaDespachoResponse> lineas
) {

    public record LineaDespachoResponse(
            Long id,
            Long requisicionLineaId,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String unidadMedida,
            Long loteId,
            String codigoLote,
            LocalDate fechaVencimiento,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            boolean fefoRespetado,
            String motivoNoFefo
    ) {
    }
}
