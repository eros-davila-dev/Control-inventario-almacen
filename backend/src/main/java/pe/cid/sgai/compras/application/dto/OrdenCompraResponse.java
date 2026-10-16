package pe.cid.sgai.compras.application.dto;

import pe.cid.sgai.compras.domain.EstadoOrdenCompra;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrdenCompraResponse(
        Long id,
        String numero,
        Long proveedorId,
        String proveedorRuc,
        String proveedorRazonSocial,
        Long requerimientoId,
        String requerimientoNumero,
        Long almacenDestinoId,
        String almacenDestinoNombre,
        LocalDate fechaEmision,
        LocalDate fechaEsperada,
        String moneda,
        String condicionPago,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        EstadoOrdenCompra estado,
        String estadoDescripcion,
        String creadoPor,
        String aprobador,
        Instant fechaAprobacion,
        String observacion,
        List<LineaOrdenCompraResponse> lineas
) {

    public record LineaOrdenCompraResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String unidadMedida,
            boolean controlaLote,
            boolean controlaVencimiento,
            BigDecimal cantidad,
            BigDecimal cantidadRecibida,
            BigDecimal pendiente,
            BigDecimal precioUnitario,
            BigDecimal importe,
            String observacion
    ) {
    }
}
