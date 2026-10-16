package pe.cid.sgai.seguridad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = "Ingrese su contrasena actual")
        String passwordActual,

        @NotBlank(message = "Ingrese la nueva contrasena")
        @Size(min = 10, max = 100, message = "La contrasena debe tener al menos 10 caracteres")
        String passwordNueva,

        @NotBlank(message = "Confirme la nueva contrasena")
        String confirmacion
) {
    public boolean coincide() {
        return passwordNueva != null && passwordNueva.equals(confirmacion);
    }
}
