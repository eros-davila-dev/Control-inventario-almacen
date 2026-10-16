package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineaIngresoRequest(
        @NotNull(message = "Seleccione el producto") Long productoId,
        @Size(max = 60) String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        Long loteId,
        Long ubicacionId,
        @NotNull(message = "Indique la cantidad")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 14, fraction = 4) BigDecimal cantidad,
        @NotNull(message = "Indique el costo unitario")
        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        @Digits(integer = 12, fraction = 6) BigDecimal costoUnitario
) {
}
