package pe.cid.sgai.catalogo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El codigo es obligatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @Size(max = 250) String descripcion,
        Long categoriaPadreId,
        Boolean requiereCadenaFrio,
        Boolean activo
) {
}
