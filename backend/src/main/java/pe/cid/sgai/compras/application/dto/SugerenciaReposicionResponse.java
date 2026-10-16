package pe.cid.sgai.compras.application.dto;

import java.math.BigDecimal;

/**
 * Propuesta de reposicion calculada por el sistema (HU-27):
 * cantidad = max(stockMaximo - disponible, consumoPromedioDiario x diasCobertura).
 */
public record SugerenciaReposicionResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        boolean critico,
        BigDecimal disponible,
        BigDecimal stockMinimo,
        BigDecimal puntoReorden,
        BigDecimal stockMaximo,
        BigDecimal consumoPromedioDiario,
        BigDecimal coberturaDias,
        BigDecimal cantidadSugerida,
        String estadoReposicion
) {
}
