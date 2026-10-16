package pe.cid.sgai.inventario.application.dto;

import pe.cid.sgai.inventario.domain.EstadoTransferencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransferenciaResponse(
        Long id,
        String numero,
        Long almacenOrigenId,
        String almacenOrigenNombre,
        Long almacenDestinoId,
        String almacenDestinoNombre,
        LocalDate fechaEnvio,
        LocalDate fechaRecepcion,
        EstadoTransferencia estado,
        String estadoDescripcion,
        String transportista,
        String guiaRemision,
        String observacion,
        String enviadoPor,
        String recibidoPor,
        Long movimientoSalidaId,
        Long movimientoIngresoId,
        boolean tieneDiferencias,
        List<LineaTransferenciaResponse> lineas
) {

    public record LineaTransferenciaResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            Long loteId,
            String codigoLote,
            LocalDate fechaVencimiento,
            BigDecimal cantidadEnviada,
            BigDecimal cantidadRecibida,
            BigDecimal diferencia,
            BigDecimal costoUnitario,
            String observacion
    ) {
    }
}
