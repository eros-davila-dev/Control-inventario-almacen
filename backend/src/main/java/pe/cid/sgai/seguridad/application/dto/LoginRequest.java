package pe.cid.sgai.seguridad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Ingrese su usuario o correo")
        @Size(max = 150)
        String usuario,

        @NotBlank(message = "Ingrese su contrasena")
        @Size(max = 100)
        String password
) {
}
