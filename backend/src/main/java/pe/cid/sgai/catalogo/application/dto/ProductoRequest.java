package pe.cid.sgai.catalogo.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.catalogo.domain.CondicionAlmacenamiento;
import pe.cid.sgai.catalogo.domain.TipoProducto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoRequest(
        @NotBlank(message = "El codigo es obligatorio") @Size(max = 30) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 200) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull(message = "Seleccione la categoria") Long categoriaId,
        @NotNull(message = "Seleccione la unidad de medida") Long unidadMedidaId,
        @NotNull(message = "Seleccione el tipo de producto") TipoProducto tipoProducto,
        @Size(max = 120) String presentacion,
        @Size(max = 120) String marca,
        @Size(max = 60) String registroSanitario,
        LocalDate venceRegistroSanitario,
        @NotNull(message = "Indique la condicion de almacenamiento") CondicionAlmacenamiento condicionAlmacenamiento,
        Boolean controlaLote,
        Boolean controlaVencimiento,
        @Min(0) @Max(3650) Integer vidaUtilMinimaDias,
        ClasificacionAbc clasificacionAbc,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal stockMinimo,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal puntoReorden,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal stockMaximo,
        @Min(1) @Max(365) Integer diasCobertura,
        Boolean critico,
        Boolean activo
) {

    @AssertTrue(message = "Debe cumplirse stock minimo <= punto de reorden <= stock maximo")
    public boolean isParametrosCoherentes() {
        if (stockMinimo == null || puntoReorden == null || stockMaximo == null) {
            return true;
        }
        return stockMinimo.compareTo(puntoReorden) <= 0 && puntoReorden.compareTo(stockMaximo) <= 0;
    }

    @AssertTrue(message = "Los insumos sanitarios y medicamentos exigen control de lote y vencimiento")
    public boolean isTrazabilidadCoherente() {
        if (tipoProducto == null || !tipoProducto.exigeTrazabilidad()) {
            return true;
        }
        return !Boolean.FALSE.equals(controlaLote) && !Boolean.FALSE.equals(controlaVencimiento);
    }
}
