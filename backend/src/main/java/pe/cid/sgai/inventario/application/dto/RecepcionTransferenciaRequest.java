package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Confirmacion en destino; la diferencia genera merma en transito con motivo (HU-22). */
public record RecepcionTransferenciaRequest(
        Long ubicacionDestinoId,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Confirme las cantidades recibidas") @Valid List<LineaRecepcion> lineas
) {

    public record LineaRecepcion(
            @NotNull Long transferenciaLineaId,
            @NotNull @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
            BigDecimal cantidadRecibida,
            @Size(max = 300) String observacion
    ) {
    }
}
