package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.inventario.domain.EstadoLote;

/** Liberacion, retencion o rechazo de un lote por el quimico farmaceutico (HU-16). */
public record CambioEstadoLoteRequest(
        @NotNull(message = "Indique el nuevo estado") EstadoLote estado,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 300) String motivo
) {
}
