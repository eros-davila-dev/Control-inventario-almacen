package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;

/** Proyeccion de saldo consolidado por producto y almacen (consulta agregada). */
public record StockAgregadoDto(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        Long almacenId,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal reservada,
        BigDecimal stockMinimo,
        BigDecimal puntoReorden,
        BigDecimal stockMaximo,
        BigDecimal costoPromedio,
        Boolean critico
) {
}
