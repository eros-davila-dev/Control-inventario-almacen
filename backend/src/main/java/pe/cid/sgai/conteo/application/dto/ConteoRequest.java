package pe.cid.sgai.conteo.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Registro del conteo fisico; el segundo conteo se usa en las diferencias (HU-37). */
public record ConteoRequest(
        @NotEmpty(message = "Registre al menos un conteo") @Valid List<LineaConteo> lineas
) {

    public record LineaConteo(
            @NotNull Long lineaId,
            @NotNull @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
            BigDecimal cantidadContada,
            Boolean esSegundoConteo,
            @Size(max = 300) String observacion
    ) {
    }
}
