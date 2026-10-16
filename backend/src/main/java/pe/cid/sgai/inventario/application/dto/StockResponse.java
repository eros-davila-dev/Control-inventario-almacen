package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.EstadoLote;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saldo detallado por producto - almacen - lote - ubicacion (HU-26). */
public record StockResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        Long almacenId,
        String almacenNombre,
        Long loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        EstadoLote estadoLote,
        Long diasParaVencer,
        Long ubicacionId,
        String ubicacionCodigo,
        BigDecimal cantidad,
        BigDecimal reservada,
        BigDecimal disponible,
        BigDecimal costoPromedio,
        BigDecimal valorizado,
        boolean despachable
) {
}
