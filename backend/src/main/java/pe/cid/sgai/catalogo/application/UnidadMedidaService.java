package pe.cid.sgai.catalogo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.catalogo.application.dto.UnidadMedidaRequest;
import pe.cid.sgai.catalogo.application.dto.UnidadMedidaResponse;
import pe.cid.sgai.catalogo.application.mapper.UnidadMedidaMapper;
import pe.cid.sgai.catalogo.domain.UnidadMedida;
import pe.cid.sgai.catalogo.repository.ProductoRepository;
import pe.cid.sgai.catalogo.repository.UnidadMedidaRepository;
import pe.cid.sgai.shared.application.BaseCrudService;
import pe.cid.sgai.shared.config.ConfiguracionCache;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.util.List;

/** Unidades de medida y su factor de conversion a la unidad base (HU-09). */
@Service
@RequiredArgsConstructor
public class UnidadMedidaService
        extends BaseCrudService<UnidadMedida, UnidadMedidaRequest, UnidadMedidaResponse> {

    private final UnidadMedidaRepository repository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaMapper mapper;

    @Override
    protected JpaRepository<UnidadMedida, Long> repositorio() {
        return repository;
    }

    @Override
    protected String nombreEntidad() {
        return "unidad de medida";
    }

    @Transactional(readOnly = true)
    public List<UnidadMedidaResponse> listarActivas() {
        return mapper.aResponses(repository.findAllByActivoTrueOrderByNombreAsc());
    }

    @Override
    @CacheEvict(value = ConfiguracionCache.UNIDADES_MEDIDA, allEntries = true)
    public UnidadMedidaResponse crear(UnidadMedidaRequest request) {
        return super.crear(request);
    }

    @Override
    @CacheEvict(value = ConfiguracionCache.UNIDADES_MEDIDA, allEntries = true)
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request) {
        return super.actualizar(id, request);
    }

    @Override
    protected void validarCreacion(UnidadMedidaRequest request) {
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de unidad ya existe");
        }
    }

    @Override
    protected void validarActualizacion(UnidadMedida entidad, UnidadMedidaRequest request) {
        if (repository.existsByCodigoIgnoreCaseAndIdNot(request.codigo(), entidad.getId())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de unidad ya existe");
        }
        if (request.umBaseId() != null && request.umBaseId().equals(entidad.getId())) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "Una unidad no puede ser su propia unidad base");
        }
    }

    @Override
    protected void validarEliminacion(UnidadMedida entidad) {
        if (productoRepository.countByUnidadMedidaId(entidad.getId()) > 0) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "La unidad tiene productos asociados: desactivela en lugar de eliminarla");
        }
        if (repository.countByUmBaseId(entidad.getId()) > 0) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "Existen unidades que convierten hacia esta unidad base");
        }
    }

    @Override
    protected UnidadMedida nuevaEntidad(UnidadMedidaRequest request) {
        UnidadMedida unidad = UnidadMedida.builder()
                .codigo(request.codigo().toUpperCase())
                .nombre(request.nombre().trim())
                .simbolo(request.simbolo())
                .factorABase(request.factorABase() == null ? BigDecimal.ONE : request.factorABase())
                .activo(request.activo() == null || request.activo())
                .build();
        unidad.setUmBase(resolverBase(request.umBaseId()));
        return unidad;
    }

    @Override
    protected void aplicarCambios(UnidadMedida entidad, UnidadMedidaRequest request) {
        entidad.setCodigo(request.codigo().toUpperCase());
        entidad.setNombre(request.nombre().trim());
        entidad.setSimbolo(request.simbolo());
        entidad.setFactorABase(request.factorABase() == null ? BigDecimal.ONE : request.factorABase());
        entidad.setUmBase(resolverBase(request.umBaseId()));
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
    }

    @Override
    protected UnidadMedidaResponse aResponse(UnidadMedida entidad) {
        return mapper.aResponse(entidad);
    }

    private UnidadMedida resolverBase(Long umBaseId) {
        if (umBaseId == null) {
            return null;
        }
        return repository.findById(umBaseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("unidad de medida base", umBaseId));
    }
}
