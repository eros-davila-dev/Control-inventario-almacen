package pe.cid.sgai.seguridad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.seguridad.application.dto.PermisoResponse;
import pe.cid.sgai.seguridad.application.dto.RolRequest;
import pe.cid.sgai.seguridad.application.dto.RolResponse;
import pe.cid.sgai.seguridad.application.mapper.PermisoMapper;
import pe.cid.sgai.seguridad.domain.Permiso;
import pe.cid.sgai.seguridad.domain.Rol;
import pe.cid.sgai.seguridad.repository.PermisoRepository;
import pe.cid.sgai.seguridad.repository.RolRepository;
import pe.cid.sgai.seguridad.repository.UsuarioRepository;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Administracion de roles y sus permisos granulares (HU-05). */
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermisoMapper permisoMapper;

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return rolRepository.findAll(org.springframework.data.domain.Sort.by("nombre")).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    @Transactional
    @Auditado(accion = "CREAR_ROL", entidad = "rol")
    public RolResponse crear(RolRequest request) {
        if (rolRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El rol ya existe");
        }
        Rol rol = Rol.builder()
                .nombre(request.nombre().toUpperCase())
                .descripcion(request.descripcion())
                .activo(request.activo() == null || request.activo())
                .esSistema(false)
                .permisos(resolverPermisos(request.permisoIds()))
                .build();
        return aResponse(rolRepository.save(rol));
    }

    @Transactional
    @Auditado(accion = "ACTUALIZAR_ROL", entidad = "rol")
    public RolResponse actualizar(Long id, RolRequest request) {
        Rol rol = buscar(id);
        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new NegocioException(CodigoError.DUPLICADO, "El rol ya existe");
        }
        if (rol.isEsSistema() && !rol.getNombre().equalsIgnoreCase(request.nombre())) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "No se puede renombrar un rol del sistema");
        }
        rol.setNombre(request.nombre().toUpperCase());
        rol.setDescripcion(request.descripcion());
        if (request.activo() != null) {
            if (!request.activo() && rol.isEsSistema()) {
                throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                        "No se puede desactivar un rol del sistema");
            }
            rol.setActivo(request.activo());
        }
        rol.reemplazarPermisos(resolverPermisos(request.permisoIds()));
        return aResponse(rolRepository.save(rol));
    }

    @Transactional
    @Auditado(accion = "ELIMINAR_ROL", entidad = "rol")
    public void eliminar(Long id) {
        Rol rol = buscar(id);
        if (rol.isEsSistema()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "Los roles del sistema no se pueden eliminar");
        }
        long usuarios = usuarioRepository.contarPorRol(id);
        if (usuarios > 0) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "El rol tiene " + usuarios + " usuario(s) asignado(s)");
        }
        rolRepository.delete(rol);
    }

    private Set<Permiso> resolverPermisos(Set<Long> permisoIds) {
        Set<Permiso> permisos = new LinkedHashSet<>();
        permisoIds.forEach(permisoId -> permisos.add(permisoRepository.findById(permisoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("permiso", permisoId))));
        return permisos;
    }

    private RolResponse aResponse(Rol rol) {
        List<PermisoResponse> permisos = rol.getPermisos().stream()
                .sorted(Comparator.comparing(Permiso::getModulo).thenComparing(Permiso::getCodigo))
                .map(permisoMapper::aResponse)
                .toList();
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isEsSistema(),
                rol.isActivo(), usuarioRepository.contarPorRol(rol.getId()), permisos);
    }

    private Rol buscar(Long id) {
        return rolRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("rol", id));
    }
}
