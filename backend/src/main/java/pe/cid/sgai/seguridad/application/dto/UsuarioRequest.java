package pe.cid.sgai.seguridad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 4, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Solo se permiten letras, numeros, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo invalido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 120)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 120)
        String apellidos,

        @Pattern(regexp = "^$|^[0-9]{8,15}$", message = "DNI invalido")
        String dni,

        @Size(max = 120)
        String cargo,

        @Size(max = 30)
        String telefono,

        Boolean activo,

        @NotEmpty(message = "Asigne al menos un rol")
        Set<Long> rolIds,

        Set<Long> almacenIds
) {
}
