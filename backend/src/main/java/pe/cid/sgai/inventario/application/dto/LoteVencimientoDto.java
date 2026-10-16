package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.EstadoLote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteVencimientoDto(
        Long loteId, String codigoLote, LocalDate fechaVencimiento, EstadoLote estadoLote,
        Long productoId, String productoCodigo, String productoNombre,
        Long almacenId, String almacenNombre, BigDecimal cantidad, BigDecimal valorExpuesto
) {
}
