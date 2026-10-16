package pe.cid.sgai.catalogo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.catalogo.application.dto.CategoriaRequest;
import pe.cid.sgai.catalogo.application.dto.CategoriaResponse;
import pe.cid.sgai.catalogo.application.mapper.CategoriaMapper;
import pe.cid.sgai.catalogo.domain.CategoriaProducto;
import pe.cid.sgai.catalogo.repository.CategoriaProductoRepository;
import pe.cid.sgai.catalogo.repository.ProductoRepository;
import pe.cid.sgai.shared.application.BaseCrudService;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;

import java.util.List;

/** Categorias jerarquicas del catalogo (HU-08). */
@Service
@RequiredArgsConstructor
public class CategoriaService
        extends BaseCrudService<CategoriaProducto, CategoriaRequest, CategoriaResponse> {

    private final CategoriaProductoRepository repository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper mapper;

    @Override
    protected JpaRepository<CategoriaProducto, Long> repositorio() {
        return repository;
    }

    @Override
    protected String nombreEntidad() {
        return "categoria";
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return mapper.aResponses(repository.findAllByActivoTrueOrderByNombreAsc());
    }

    @Override
    protected void validarCreacion(CategoriaRequest request) {
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de categoria ya existe");
        }
    }

    @Override
    protected void validarActualizacion(CategoriaProducto entidad, CategoriaRequest request) {
        if (repository.existsByCodigoIgnoreCaseAndIdNot(request.codigo(), entidad.getId())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de categoria ya existe");
        }
        validarSinCiclo(entidad.getId(), request.categoriaPadreId());
    }

    @Override
    protected void validarEliminacion(CategoriaProducto entidad) {
        if (productoRepository.countByCategoriaId(entidad.getId()) > 0) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "La categoria tiene productos asociados");
        }
        if (repository.countByCategoriaPadreId(entidad.getId()) > 0) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "La categoria tiene subcategorias");
        }
    }

    @Override
    protected CategoriaProducto nuevaEntidad(CategoriaRequest request) {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .codigo(request.codigo().toUpperCase())
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .requiereCadenaFrio(Boolean.TRUE.equals(request.requiereCadenaFrio()))
                .activo(request.activo() == null || request.activo())
                .build();
        categoria.setCategoriaPadre(resolverPadre(request.categoriaPadreId()));
        return categoria;
    }

    @Override
    protected void aplicarCambios(CategoriaProducto entidad, CategoriaRequest request) {
        entidad.setCodigo(request.codigo().toUpperCase());
        entidad.setNombre(request.nombre().trim());
        entidad.setDescripcion(request.descripcion());
        entidad.setRequiereCadenaFrio(Boolean.TRUE.equals(request.requiereCadenaFrio()));
        entidad.setCategoriaPadre(resolverPadre(request.categoriaPadreId()));
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
    }

    @Override
    protected CategoriaResponse aResponse(CategoriaProducto entidad) {
        return mapper.aResponse(entidad);
    }

    private CategoriaProducto resolverPadre(Long padreId) {
        if (padreId == null) {
            return null;
        }
        return repository.findById(padreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("categoria padre", padreId));
    }

    /** Evita que una categoria sea descendiente de si misma. */
    private void validarSinCiclo(Long id, Long padreId) {
        Long actual = padreId;
        int guarda = 0;
        while (actual != null && guarda++ < 20) {
            if (actual.equals(id)) {
                throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                        "La jerarquia de categorias no puede formar un ciclo");
            }
            actual = repository.findById(actual)
                    .map(CategoriaProducto::getCategoriaPadre)
                    .map(CategoriaProducto::getId)
                    .orElse(null);
        }
    }
}
