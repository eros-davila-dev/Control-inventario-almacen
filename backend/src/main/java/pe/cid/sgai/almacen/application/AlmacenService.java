package pe.cid.sgai.almacen.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.dto.AlmacenRequest;
import pe.cid.sgai.almacen.application.dto.AlmacenResponse;
import pe.cid.sgai.almacen.application.mapper.AlmacenMapper;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.TipoAlmacen;
import pe.cid.sgai.almacen.repository.AlmacenRepository;
import pe.cid.sgai.seguridad.infrastructure.ContextoSeguridad;
import pe.cid.sgai.seguridad.infrastructure.UsuarioAutenticado;
import pe.cid.sgai.shared.application.BaseCrudService;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;

import java.util.List;

/**
 * Red de almacenes (HU-12). Expone ademas la verificacion de alcance por almacen
 * que usan los modulos de inventario, compras, despacho y conteo (HU-14).
 */
@Service
@RequiredArgsConstructor
public class AlmacenService extends BaseCrudService<Almacen, AlmacenRequest, AlmacenResponse> {

    private final AlmacenRepository repository;
    private final AlmacenMapper mapper;

    @Override
    protected JpaRepository<Almacen, Long> repositorio() {
        return repository;
    }

    @Override
    protected String nombreEntidad() {
        return "almacen";
    }

    @Transactional(readOnly = true)
    public List<AlmacenResponse> listarActivos() {
        return mapper.aResponses(repository.findAllByActivoTrueOrderByNombreAsc());
    }

    /** Almacenes que el usuario autenticado puede operar. */
    @Transactional(readOnly = true)
    public List<AlmacenResponse> listarAutorizados() {
        UsuarioAutenticado usuario = ContextoSeguridad.requerido();
        List<Almacen> almacenes = repository.findAllByActivoTrueOrderByNombreAsc();
        if (usuario.tieneVisionGlobal()) {
            return mapper.aResponses(almacenes);
        }
        return mapper.aResponses(almacenes.stream()
                .filter(a -> usuario.almacenIds().contains(a.getId()))
                .toList());
    }

    @Transactional(readOnly = true)
    public Almacen buscarOperable(Long id) {
        Almacen almacen = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("almacen", id));
        if (!almacen.isActivo()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "El almacen " + almacen.getNombre() + " esta inactivo");
        }
        ContextoSeguridad.verificarAlmacen(id);
        return almacen;
    }

    /** Almacen de cuarentena de la sede, destino de las recepciones observadas (HU-31). */
    @Transactional(readOnly = true)
    public Almacen cuarentenaDe(Almacen almacen) {
        return repository.findFirstByTipoAlmacenAndSedeIgnoreCaseAndActivoTrue(
                        TipoAlmacen.CUARENTENA, almacen.getSede())
                .orElseThrow(() -> new NegocioException(CodigoError.REGLA_NEGOCIO,
                        "No existe un almacen de cuarentena configurado para la sede "
                                + almacen.getSede()));
    }

    @Override
    protected void validarCreacion(AlmacenRequest request) {
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de almacen ya existe");
        }
    }

    @Override
    protected void validarActualizacion(Almacen entidad, AlmacenRequest request) {
        if (repository.existsByCodigoIgnoreCaseAndIdNot(request.codigo(), entidad.getId())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de almacen ya existe");
        }
    }

    @Override
    protected Almacen nuevaEntidad(AlmacenRequest request) {
        return Almacen.builder()
                .codigo(request.codigo().toUpperCase())
                .nombre(request.nombre().trim())
                .sede(request.sede().trim())
                .direccion(request.direccion())
                .tipoAlmacen(request.tipoAlmacen())
                .responsableId(request.responsableId())
                .telefono(request.telefono())
                .permiteNegativo(Boolean.TRUE.equals(request.permiteNegativo()))
                .activo(request.activo() == null || request.activo())
                .build();
    }

    @Override
    protected void aplicarCambios(Almacen entidad, AlmacenRequest request) {
        entidad.setCodigo(request.codigo().toUpperCase());
        entidad.setNombre(request.nombre().trim());
        entidad.setSede(request.sede().trim());
        entidad.setDireccion(request.direccion());
        entidad.setTipoAlmacen(request.tipoAlmacen());
        entidad.setResponsableId(request.responsableId());
        entidad.setTelefono(request.telefono());
        if (request.permiteNegativo() != null) {
            entidad.setPermiteNegativo(request.permiteNegativo());
        }
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
    }

    @Override
    protected AlmacenResponse aResponse(Almacen entidad) {
        return mapper.aResponse(entidad);
    }
}
