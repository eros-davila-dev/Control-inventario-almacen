package pe.cid.sgai.seguridad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.seguridad.application.dto.PasswordTemporalResponse;
import pe.cid.sgai.seguridad.application.dto.UsuarioRequest;
import pe.cid.sgai.seguridad.application.dto.UsuarioResponse;
import pe.cid.sgai.seguridad.application.mapper.UsuarioMapper;
import pe.cid.sgai.seguridad.domain.Rol;
import pe.cid.sgai.seguridad.domain.Usuario;
import pe.cid.sgai.seguridad.repository.RefreshTokenRepository;
import pe.cid.sgai.seguridad.repository.RolRepository;
import pe.cid.sgai.seguridad.repository.UsuarioRepository;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Alta, edicion y desactivacion de usuarios con sus roles y almacenes (HU-04). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ServicioPoliticaPassword politicaPassword;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String texto, Long rolId, Boolean activo, Pageable pageable) {
        Page<Usuario> pagina = usuarioRepository.buscar(
                texto == null || texto.isBlank() ? null : texto.trim(), rolId, activo, pageable);
        return PageResponse.de(pagina, usuarioMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return usuarioMapper.aResponse(buscar(id));
    }

    @Transactional
    @Auditado(accion = "CREAR_USUARIO", entidad = "usuario")
    public PasswordTemporalResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El usuario ya esta registrado");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El correo ya esta registrado");
        }
        String passwordTemporal = politicaPassword.generarTemporal();
        String hash = passwordEncoder.encode(passwordTemporal);

        Usuario usuario = Usuario.builder()
                .username(request.username().toLowerCase())
                .email(request.email().toLowerCase())
                .passwordHash(hash)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .dni(vacioANulo(request.dni()))
                .cargo(vacioANulo(request.cargo()))
                .telefono(vacioANulo(request.telefono()))
                .activo(request.activo() == null || request.activo())
                .debeCambiarPassword(true)
                .roles(resolverRoles(request.rolIds()))
                .almacenIds(request.almacenIds() == null ? new HashSet<>() : new HashSet<>(request.almacenIds()))
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        politicaPassword.registrarEnHistorico(guardado.getId(), hash);
        return new PasswordTemporalResponse(guardado.getId(), guardado.getUsername(), passwordTemporal);
    }

    @Transactional
    @Auditado(accion = "ACTUALIZAR_USUARIO", entidad = "usuario")
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscar(id);
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(request.username(), id)) {
            throw new NegocioException(CodigoError.DUPLICADO, "El usuario ya esta registrado");
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new NegocioException(CodigoError.DUPLICADO, "El correo ya esta registrado");
        }
        usuario.setUsername(request.username().toLowerCase());
        usuario.setEmail(request.email().toLowerCase());
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setDni(vacioANulo(request.dni()));
        usuario.setCargo(vacioANulo(request.cargo()));
        usuario.setTelefono(vacioANulo(request.telefono()));
        usuario.setRoles(resolverRoles(request.rolIds()));
        usuario.setAlmacenIds(request.almacenIds() == null ? new HashSet<>() : new HashSet<>(request.almacenIds()));
        if (request.activo() != null) {
            cambiarEstado(usuario, request.activo());
        }
        return usuarioMapper.aResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    @Auditado(accion = "CAMBIAR_ESTADO_USUARIO", entidad = "usuario")
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscar(id);
        cambiarEstado(usuario, activo);
        return usuarioMapper.aResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    @Auditado(accion = "RESETEAR_PASSWORD", entidad = "usuario")
    public PasswordTemporalResponse resetearPassword(Long id) {
        Usuario usuario = buscar(id);
        String temporal = politicaPassword.generarTemporal();
        String hash = passwordEncoder.encode(temporal);
        usuario.setPasswordHash(hash);
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        politicaPassword.registrarEnHistorico(id, hash);
        refreshTokenRepository.revocarTodosDe(id);
        return new PasswordTemporalResponse(id, usuario.getUsername(), temporal);
    }

    @Transactional
    @Auditado(accion = "DESBLOQUEAR_USUARIO", entidad = "usuario")
    public UsuarioResponse desbloquear(Long id) {
        Usuario usuario = buscar(id);
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
        return usuarioMapper.aResponse(usuarioRepository.save(usuario));
    }

    private void cambiarEstado(Usuario usuario, boolean activo) {
        usuario.setActivo(activo);
        if (!activo) {
            refreshTokenRepository.revocarTodosDe(usuario.getId());
        }
    }

    private Set<Rol> resolverRoles(Set<Long> rolIds) {
        Set<Rol> roles = new LinkedHashSet<>();
        rolIds.forEach(rolId -> roles.add(rolRepository.findById(rolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("rol", rolId))));
        return roles;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", id));
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
