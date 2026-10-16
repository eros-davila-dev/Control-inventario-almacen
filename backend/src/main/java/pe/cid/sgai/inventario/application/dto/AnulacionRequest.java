package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionRequest(
        @NotBlank(message = "Indique el motivo de la anulacion") @Size(max = 500) String motivo
) {
}
