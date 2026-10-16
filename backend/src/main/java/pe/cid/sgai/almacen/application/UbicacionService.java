package pe.cid.sgai.almacen.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.dto.UbicacionRequest;
import pe.cid.sgai.almacen.application.dto.UbicacionResponse;
import pe.cid.sgai.almacen.application.mapper.AlmacenMapper;
import pe.cid.sgai.almacen.domain.TipoUbicacion;
import pe.cid.sgai.almacen.domain.Ubicacion;
import pe.cid.sgai.almacen.repository.AlmacenRepository;
import pe.cid.sgai.almacen.repository.UbicacionRepository;
import pe.cid.sgai.shared.application.BaseCrudService;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

/** Ubicaciones fisicas por almacen (HU-13). */
@Service
@RequiredArgsConstructor
public class UbicacionService extends BaseCrudService<Ubicacion, UbicacionRequest, UbicacionResponse> {

    private final UbicacionRepository repository;
    private final AlmacenRepository almacenRepository;
    private final AlmacenMapper mapper;

    @Override
    protected JpaRepository<Ubicacion, Long> repositorio() {
        return repository;
    }

    @Override
    protected String nombreEntidad() {
        return "ubicacion";
    }

    @Transactional(readOnly = true)
    public PageResponse<UbicacionResponse> buscar(Long almacenId, String texto, Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId,
                texto == null || texto.isBlank() ? null : texto.trim(), pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public List<UbicacionResponse> porAlmacen(Long almacenId) {
        return mapper.aUbicacionResponses(
                repository.findAllByAlmacenIdAndActivoTrueOrderByCodigoAsc(almacenId));
    }

    @Override
    protected void validarCreacion(UbicacionRequest request) {
        if (repository.existsByAlmacenIdAndCodigoIgnoreCase(request.almacenId(), request.codigo())) {
            throw new NegocioException(CodigoError.DUPLICADO,
                    "La ubicacion ya existe en el almacen seleccionado");
        }
    }

    @Override
    protected void validarActualizacion(Ubicacion entidad, UbicacionRequest request) {
        if (repository.existsByAlmacenIdAndCodigoIgnoreCaseAndIdNot(
                request.almacenId(), request.codigo(), entidad.getId())) {
            throw new NegocioException(CodigoError.DUPLICADO,
                    "La ubicacion ya existe en el almacen seleccionado");
        }
    }

    @Override
    protected Ubicacion nuevaEntidad(UbicacionRequest request) {
        return Ubicacion.builder()
                .almacen(almacenRepository.findById(request.almacenId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("almacen", request.almacenId())))
                .codigo(request.codigo().toUpperCase())
                .descripcion(request.descripcion())
                .zona(request.zona())
                .rack(request.rack())
                .nivel(request.nivel())
                .tipoUbicacion(request.tipoUbicacion() == null
                        ? TipoUbicacion.PICKING : request.tipoUbicacion())
                .activo(request.activo() == null || request.activo())
                .build();
    }

    @Override
    protected void aplicarCambios(Ubicacion entidad, UbicacionRequest request) {
        entidad.setAlmacen(almacenRepository.findById(request.almacenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("almacen", request.almacenId())));
        entidad.setCodigo(request.codigo().toUpperCase());
        entidad.setDescripcion(request.descripcion());
        entidad.setZona(request.zona());
        entidad.setRack(request.rack());
        entidad.setNivel(request.nivel());
        if (request.tipoUbicacion() != null) {
            entidad.setTipoUbicacion(request.tipoUbicacion());
        }
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
    }

    @Override
    protected UbicacionResponse aResponse(Ubicacion entidad) {
        return mapper.aResponse(entidad);
    }
}
