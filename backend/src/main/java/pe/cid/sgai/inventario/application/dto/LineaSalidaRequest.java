package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LineaSalidaRequest(
        @NotNull(message = "Seleccione el producto") Long productoId,
        Long loteId,
        Long ubicacionId,
        @NotNull(message = "Indique la cantidad")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 14, fraction = 4) BigDecimal cantidad,
        @Size(max = 300) String motivoNoFefo
) {
}
