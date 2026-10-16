package pe.cid.sgai.despacho.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Atencion de una requisicion. Si no se indica lote, el sistema reparte la
 * cantidad por FEFO; si se indica, se exige motivo cuando no es el lote FEFO.
 */
public record DespachoRequest(
        @NotNull(message = "Indique la requisicion") Long requisicionId,
        LocalDate fecha,
        @Size(max = 150) String entregadoA,
        @Size(max = 500) String observacion,
        Long ubicacionId,
        @NotEmpty(message = "Detalle lo que se despacha") @Valid List<LineaDespachoRequest> lineas
) {

    public record LineaDespachoRequest(
            @NotNull Long requisicionLineaId,
            Long loteId,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad,
            @Size(max = 300) String motivoNoFefo
    ) {
    }
}
