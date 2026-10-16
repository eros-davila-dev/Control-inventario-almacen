package pe.cid.sgai.despacho.application.dto;

import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.despacho.domain.Prioridad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RequisicionResponse(
        Long id,
        String numero,
        Long almacenSolicitanteId,
        String almacenSolicitanteNombre,
        Long almacenAtencionId,
        String almacenAtencionNombre,
        String centroCosto,
        String area,
        Prioridad prioridad,
        LocalDate fecha,
        LocalDate fechaRequerida,
        EstadoRequisicion estado,
        String estadoDescripcion,
        String solicitante,
        String observacion,
        List<LineaRequisicionResponse> lineas
) {

    public record LineaRequisicionResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String unidadMedida,
            boolean controlaLote,
            BigDecimal cantidadSolicitada,
            BigDecimal cantidadAtendida,
            BigDecimal pendiente,
            String observacion
    ) {
    }
}
