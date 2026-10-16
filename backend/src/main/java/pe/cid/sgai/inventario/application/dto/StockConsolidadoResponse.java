package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;

/** Saldo consolidado con semaforo de reposicion y cobertura en dias (HU-39, HU-41). */
public record StockConsolidadoResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        Long almacenId,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal reservada,
        BigDecimal disponible,
        BigDecimal stockMinimo,
        BigDecimal puntoReorden,
        BigDecimal stockMaximo,
        BigDecimal costoPromedio,
        BigDecimal valorizado,
        BigDecimal consumoPromedioDiario,
        BigDecimal coberturaDias,
        boolean critico,
        String estadoReposicion
) {
}
