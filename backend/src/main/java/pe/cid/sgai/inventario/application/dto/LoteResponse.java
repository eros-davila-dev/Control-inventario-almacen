package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.EstadoLote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoteResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String codigoLote,
        Long proveedorId,
        String proveedorNombre,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        Long diasParaVencer,
        EstadoLote estadoLote,
        String estadoDescripcion,
        String motivoEstado,
        Instant fechaLiberacion,
        String liberadoPor,
        BigDecimal costoUnitario,
        BigDecimal saldoTotal,
        boolean despachable,
        boolean vencido
) {
}
