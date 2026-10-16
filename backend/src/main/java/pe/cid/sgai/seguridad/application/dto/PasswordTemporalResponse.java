package pe.cid.sgai.seguridad.application.dto;

/** Contrasena temporal entregada al administrador para su comunicacion al usuario. */
public record PasswordTemporalResponse(Long usuarioId, String username, String passwordTemporal) {
}
