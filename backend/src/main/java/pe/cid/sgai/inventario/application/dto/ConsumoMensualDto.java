package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;

public record ConsumoMensualDto(
        Integer anio, Integer mes, String area, String centroCosto,
        BigDecimal cantidad, BigDecimal valor
) {
}
