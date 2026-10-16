package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.EstadoMovimiento;
import pe.cid.sgai.inventario.domain.MotivoAjuste;
import pe.cid.sgai.inventario.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MovimientoResponse(
        Long id,
        String numero,
        TipoMovimiento tipoMovimiento,
        String tipoDescripcion,
        EstadoMovimiento estadoMovimiento,
        LocalDate fecha,
        Long almacenOrigenId,
        String almacenOrigenNombre,
        Long almacenDestinoId,
        String almacenDestinoNombre,
        MotivoAjuste motivoAjuste,
        String documentoTipo,
        String documentoNumero,
        Long proveedorId,
        String proveedorNombre,
        String centroCosto,
        String areaDestino,
        String observacion,
        Long movimientoAnuladoId,
        String referenciaTipo,
        Long referenciaId,
        BigDecimal valorTotal,
        String registradoPor,
        Instant registradoEn,
        List<MovimientoLineaResponse> lineas
) {
}
