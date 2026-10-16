package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.inventario.domain.MotivoAjuste;

import java.util.List;

/** Ajuste de inventario con motivo tipificado y comentario obligatorio (HU-23). */
public record AjusteRequest(
        @NotNull(message = "Seleccione el almacen") Long almacenId,
        @NotNull(message = "Indique el motivo del ajuste") MotivoAjuste motivoAjuste,
        @NotNull(message = "Indique si el ajuste suma o resta inventario") Boolean incrementa,
        @NotBlank(message = "El comentario del ajuste es obligatorio") @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos una linea") @Valid List<LineaAjusteRequest> lineas
) {

    public record LineaAjusteRequest(
            @NotNull Long productoId,
            Long loteId,
            Long ubicacionId,
            @NotNull @jakarta.validation.constraints.DecimalMin(value = "0.0001",
                    message = "La cantidad debe ser mayor a cero") java.math.BigDecimal cantidad,
            java.math.BigDecimal costoUnitario
    ) {
    }
}
