package pe.cid.sgai.catalogo.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UnidadMedidaRequest(
        @NotBlank(message = "El codigo es obligatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @Size(max = 10) String simbolo,
        Long umBaseId,
        @NotNull(message = "Indique el factor de conversion")
        @DecimalMin(value = "0.000001", message = "El factor debe ser mayor a cero")
        @Digits(integer = 12, fraction = 6) BigDecimal factorABase,
        Boolean activo
) {
}
