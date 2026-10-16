package pe.cid.sgai.inventario.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RotacionProductoDto(
        Long productoId, String codigo, String nombre,
        BigDecimal cantidadConsumida, BigDecimal valorConsumido, LocalDate ultimaSalida
) {
}
