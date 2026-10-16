package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linea de kardex valorizado por costo promedio ponderado (HU-25). */
public record KardexResponse(
        Long id,
        LocalDate fecha,
        String numeroDocumento,
        TipoMovimiento tipoMovimiento,
        String tipoDescripcion,
        String codigoLote,
        BigDecimal entrada,
        BigDecimal salida,
        BigDecimal saldo,
        BigDecimal costoUnitario,
        BigDecimal costoPromedio,
        BigDecimal valorSaldo,
        String usuario
) {
}
