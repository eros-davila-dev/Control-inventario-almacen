package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteRequest(
        @NotNull(message = "Seleccione el producto") Long productoId,
        @NotBlank(message = "Ingrese el codigo de lote del fabricante") @Size(max = 60) String codigoLote,
        Long proveedorId,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        @DecimalMin(value = "0", message = "El costo no puede ser negativo") BigDecimal costoUnitario,
        Boolean iniciarEnCuarentena
) {
}
