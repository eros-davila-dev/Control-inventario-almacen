package pe.cid.sgai.seguridad.application.dto;

/**
 * Respuesta de autenticacion. El access token es de vida corta y el refresh token
 * permite la renovacion transparente de la sesion (login transitivo, HU-01).
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {
    public static TokenResponse de(String accessToken, String refreshToken, long expiraEnSegundos,
                                   UsuarioResponse usuario) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiraEnSegundos, usuario);
    }
}
