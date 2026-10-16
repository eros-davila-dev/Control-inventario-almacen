package pe.cid.sgai.compras.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrdenCompraRequest(
        @NotNull(message = "Seleccione el proveedor") Long proveedorId,
        Long requerimientoId,
        @NotNull(message = "Seleccione el almacen destino") Long almacenDestinoId,
        LocalDate fechaEmision,
        LocalDate fechaEsperada,
        @Pattern(regexp = "^(PEN|USD)$", message = "Moneda soportada: PEN o USD") String moneda,
        @Size(max = 60) String condicionPago,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos una linea") @Valid List<LineaOrdenCompra> lineas
) {

    public record LineaOrdenCompra(
            @NotNull Long productoId,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad,
            @NotNull @DecimalMin(value = "0", message = "El precio no puede ser negativo")
            BigDecimal precioUnitario,
            @Size(max = 300) String observacion
    ) {
    }
}
