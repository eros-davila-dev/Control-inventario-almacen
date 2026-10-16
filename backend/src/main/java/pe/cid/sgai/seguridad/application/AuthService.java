package pe.cid.sgai.seguridad.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.seguridad.application.dto.CambiarPasswordRequest;
import pe.cid.sgai.seguridad.application.dto.LoginRequest;
import pe.cid.sgai.seguridad.application.dto.TokenResponse;
import pe.cid.sgai.seguridad.application.mapper.UsuarioMapper;
import pe.cid.sgai.seguridad.domain.IntentoAcceso;
import pe.cid.sgai.seguridad.domain.RefreshToken;
import pe.cid.sgai.seguridad.domain.Usuario;
import pe.cid.sgai.seguridad.infrastructure.ContextoSeguridad;
import pe.cid.sgai.seguridad.infrastructure.JwtService;
import pe.cid.sgai.seguridad.repository.IntentoAccesoRepository;
import pe.cid.sgai.seguridad.repository.RefreshTokenRepository;
import pe.cid.sgai.seguridad.repository.UsuarioRepository;
import pe.cid.sgai.shared.audit.AuditoriaService;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;

import java.time.Duration;
import java.time.Instant;

/**
 * Autenticacion, renovacion transparente de sesion y cambio de contrasena
 * (HU-01 a HU-03).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IntentoAccesoRepository intentoAccesoRepository;
    private final ServicioPoliticaPassword politicaPassword;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioMapper usuarioMapper;
    private final AuditoriaService auditoriaService;
    private final PropiedadesSgai propiedades;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String identificador = request.usuario().trim();
        Usuario usuario = usuarioRepository.buscarPorUsernameOEmail(identificador).orElse(null);

        if (usuario == null) {
            // Respuesta identica a credenciales erroneas: no se revela si el usuario existe.
            registrarIntento(identificador, false, "USUARIO_INEXISTENTE");
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS);
        }
        if (usuario.estaBloqueado()) {
            registrarIntento(identificador, false, "CUENTA_BLOQUEADA");
            long minutos = Math.max(1, Duration.between(Instant.now(), usuario.getBloqueadoHasta()).toMinutes());
            throw new NegocioException(CodigoError.CUENTA_BLOQUEADA,
                    "Cuenta bloqueada. Intente nuevamente en " + minutos + " minuto(s)");
        }
        if (!usuario.isActivo()) {
            registrarIntento(identificador, false, "CUENTA_INACTIVA");
            throw new NegocioException(CodigoError.CUENTA_INACTIVA);
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            usuario.registrarIntentoFallido(propiedades.seguridad().maxIntentosFallidos(),
                    propiedades.seguridad().bloqueoMinutos());
            usuarioRepository.save(usuario);
            registrarIntento(identificador, false, "PASSWORD_INCORRECTO");
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS);
        }

        usuario.registrarAccesoExitoso();
        usuarioRepository.save(usuario);
        registrarIntento(usuario.getUsername(), true, null);
        auditoriaService.registrar("LOGIN", "usuario", usuario.getId(), "inicio de sesion");
        return emitirTokens(usuario);
    }

    /**
     * Renovacion del access token con rotacion del refresh token. El reuso de un
     * token ya rotado revoca toda la sesion del usuario (deteccion de robo).
     */
    @Transactional
    public TokenResponse refrescar(String refreshToken) {
        String hash = jwtService.hashear(refreshToken);
        RefreshToken almacenado = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new NegocioException(CodigoError.TOKEN_INVALIDO));

        if (!almacenado.esVigente()) {
            refreshTokenRepository.revocarTodosDe(almacenado.getUsuario().getId());
            auditoriaService.registrar("REFRESH_REUSO", "usuario", almacenado.getUsuario().getId(),
                    "intento de reuso de refresh token revocado o expirado", false);
            throw new NegocioException(CodigoError.TOKEN_INVALIDO);
        }

        Usuario usuario = almacenado.getUsuario();
        if (!usuario.isActivo()) {
            throw new NegocioException(CodigoError.CUENTA_INACTIVA);
        }
        almacenado.setRevocado(true);
        refreshTokenRepository.save(almacenado);
        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(jwtService.hashear(refreshToken)).ifPresent(token -> {
            token.setRevocado(true);
            refreshTokenRepository.save(token);
            auditoriaService.registrar("LOGOUT", "usuario", token.getUsuario().getId(), "cierre de sesion");
        });
    }

    @Transactional
    public void cambiarPassword(CambiarPasswordRequest request) {
        if (!request.coincide()) {
            throw new NegocioException(CodigoError.VALIDACION, "La confirmacion no coincide");
        }
        Long usuarioId = ContextoSeguridad.idUsuarioActual();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", usuarioId));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            auditoriaService.registrar("CAMBIO_PASSWORD", "usuario", usuarioId,
                    "contrasena actual incorrecta", false);
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS,
                    "La contrasena actual no es correcta");
        }
        politicaPassword.validar(request.passwordNueva());
        politicaPassword.validarNoReutilizada(usuarioId, request.passwordNueva());

        String nuevoHash = passwordEncoder.encode(request.passwordNueva());
        usuario.setPasswordHash(nuevoHash);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
        politicaPassword.registrarEnHistorico(usuarioId, nuevoHash);
        // Al cambiar la clave se invalidan las sesiones abiertas en otros dispositivos.
        refreshTokenRepository.revocarTodosDe(usuarioId);
        auditoriaService.registrar("CAMBIO_PASSWORD", "usuario", usuarioId, "contrasena actualizada");
    }

    @Transactional(readOnly = true)
    public pe.cid.sgai.seguridad.application.dto.UsuarioResponse perfilActual() {
        Long usuarioId = ContextoSeguridad.idUsuarioActual();
        return usuarioMapper.aResponse(usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", usuarioId)));
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshPlano = jwtService.generarRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(jwtService.hashear(refreshPlano))
                .expiraEn(Instant.now().plus(Duration.ofHours(propiedades.jwt().refreshTokenHoras())))
                .ip(AuditoriaService.ipActual())
                .userAgent(AuditoriaService.cabecera("User-Agent"))
                .build());
        return TokenResponse.de(accessToken, refreshPlano, jwtService.segundosDeVigencia(),
                usuarioMapper.aResponse(usuario));
    }

    private void registrarIntento(String username, boolean exitoso, String motivo) {
        intentoAccesoRepository.save(IntentoAcceso.builder()
                .username(username.length() > 60 ? username.substring(0, 60) : username)
                .exitoso(exitoso)
                .motivo(motivo)
                .ip(AuditoriaService.ipActual())
                .userAgent(AuditoriaService.cabecera("User-Agent"))
                .fecha(Instant.now())
                .build());
    }
}
