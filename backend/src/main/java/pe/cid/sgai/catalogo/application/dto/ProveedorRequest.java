package pe.cid.sgai.catalogo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 digitos") String ruc,
        @NotBlank(message = "La razon social es obligatoria") @Size(max = 200) String razonSocial,
        @Size(max = 150) String nombreComercial,
        @Size(max = 120) String contacto,
        @Size(max = 40) String telefono,
        @Email(message = "Correo invalido") @Size(max = 150) String email,
        @Size(max = 250) String direccion,
        @Size(max = 60) String condicionPago,
        @Min(value = 0, message = "El lead time no puede ser negativo")
        @Max(value = 365, message = "Lead time maximo de 365 dias") Integer leadTimeDias,
        Boolean activo
) {
}
