package pe.cid.sgai.compras.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.compras.domain.ResultadoCalidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Recepcion total o parcial con control de calidad (HU-30, HU-31). */
public record RecepcionRequest(
        @NotNull(message = "Indique la orden de compra") Long ordenId,
        LocalDate fecha,
        @Size(max = 60) String guiaRemision,
        @Size(max = 60) String factura,
        @Size(max = 150) String transportista,
        BigDecimal temperaturaLlegada,
        Boolean empaqueIntegro,
        @NotNull(message = "Indique el resultado del control de calidad") ResultadoCalidad resultadoCalidad,
        @Size(max = 500) String observacion,
        Long ubicacionId,
        @NotEmpty(message = "Detalle lo recibido") @Valid List<LineaRecepcionRequest> lineas
) {

    public record LineaRecepcionRequest(
            @NotNull Long ordenLineaId,
            @Size(max = 60) String codigoLote,
            LocalDate fechaFabricacion,
            LocalDate fechaVencimiento,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad,
            Boolean conforme,
            @Size(max = 300) String observacion
    ) {
    }
}
