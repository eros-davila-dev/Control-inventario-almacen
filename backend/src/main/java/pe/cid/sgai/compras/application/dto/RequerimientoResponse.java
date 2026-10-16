package pe.cid.sgai.compras.application.dto;

import pe.cid.sgai.compras.domain.EstadoRequerimiento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequerimientoResponse(
        Long id,
        String numero,
        Long almacenId,
        String almacenNombre,
        LocalDate fecha,
        EstadoRequerimiento estado,
        String estadoDescripcion,
        String justificacion,
        String solicitante,
        String aprobador,
        Instant fechaAprobacion,
        String motivoRechazo,
        List<LineaRequerimientoResponse> lineas
) {

    public record LineaRequerimientoResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String unidadMedida,
            BigDecimal cantidadSolicitada,
            BigDecimal cantidadSugerida,
            BigDecimal disponibleActual,
            String observacion
    ) {
    }
}
