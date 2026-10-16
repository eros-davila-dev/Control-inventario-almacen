package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Envio de una transferencia entre almacenes (HU-22). */
public record TransferenciaRequest(
        @NotNull(message = "Seleccione el almacen de origen") Long almacenOrigenId,
        @NotNull(message = "Seleccione el almacen de destino") Long almacenDestinoId,
        LocalDate fechaEnvio,
        @Size(max = 150) String transportista,
        @Size(max = 60) String guiaRemision,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos una linea") @Valid List<LineaTransferenciaRequest> lineas
) {

    public record LineaTransferenciaRequest(
            @NotNull Long productoId,
            Long loteId,
            Long ubicacionId,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad
    ) {
    }
}
