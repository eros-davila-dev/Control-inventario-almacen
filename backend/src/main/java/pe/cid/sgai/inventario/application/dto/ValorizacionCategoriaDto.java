package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;

public record ValorizacionCategoriaDto(
        Long categoriaId, String categoriaNombre, Long productos,
        BigDecimal cantidad, BigDecimal valor
) {
}
