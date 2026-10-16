package pe.cid.sgai.reportes.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Producto con saldo y sin movimiento en la ventana configurada (HU-44). */
public record BajaRotacionResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal valorizado,
        LocalDate ultimoMovimiento,
        Long diasSinMovimiento
) {
}
