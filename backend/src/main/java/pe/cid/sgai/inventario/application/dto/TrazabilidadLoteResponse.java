package pe.cid.sgai.inventario.application.dto;

import java.util.List;

/** Reconstruccion de la historia de un lote (HU-17, HU-45). */
public record TrazabilidadLoteResponse(
        LoteResponse lote,
        List<StockResponse> ubicacionesActuales,
        List<MovimientoLoteItem> movimientos
) {

    public record MovimientoLoteItem(
            Long movimientoId,
            String numero,
            String tipo,
            String tipoDescripcion,
            java.time.LocalDate fecha,
            String almacen,
            String areaDestino,
            String centroCosto,
            java.math.BigDecimal cantidad,
            String documento,
            String usuario,
            String estado
    ) {
    }
}
