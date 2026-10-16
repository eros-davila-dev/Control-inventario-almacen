package pe.cid.sgai.catalogo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.catalogo.application.dto.ProductoRequest;
import pe.cid.sgai.catalogo.application.dto.ProductoResponse;
import pe.cid.sgai.catalogo.application.dto.ProductoResumenResponse;
import pe.cid.sgai.catalogo.application.mapper.ProductoMapper;
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.catalogo.domain.CondicionAlmacenamiento;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.catalogo.domain.TipoProducto;
import pe.cid.sgai.catalogo.repository.CategoriaProductoRepository;
import pe.cid.sgai.catalogo.repository.ProductoRepository;
import pe.cid.sgai.catalogo.repository.UnidadMedidaRepository;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogo de productos del almacen clinico (HU-07, HU-11). Centraliza las reglas
 * sanitarias del maestro: trazabilidad obligatoria, condicion de almacenamiento y
 * parametros de reposicion.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repository;
    private final CategoriaProductoRepository categoriaRepository;
    private final UnidadMedidaRepository unidadRepository;
    private final ProductoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> buscar(String texto, Long categoriaId, TipoProducto tipo,
                                                 ClasificacionAbc clasificacion, Boolean critico,
                                                 Boolean activo, Pageable pageable) {
        return PageResponse.de(repository.buscar(
                texto == null || texto.isBlank() ? null : texto.trim(),
                categoriaId, tipo, clasificacion, critico, activo, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<ProductoResumenResponse> listarResumen() {
        return mapper.aResumenes(repository.findAllByActivoTrueOrderByNombreAsc());
    }

    @Transactional(readOnly = true)
    public Producto buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("producto", id));
    }

    @Transactional
    @Auditado(accion = "CREAR_PRODUCTO", entidad = "producto")
    public ProductoResponse crear(ProductoRequest request) {
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de producto ya existe");
        }
        Producto producto = new Producto();
        aplicar(producto, request);
        producto.setCostoPromedio(BigDecimal.ZERO);
        return mapper.aResponse(repository.save(producto));
    }

    @Transactional
    @Auditado(accion = "ACTUALIZAR_PRODUCTO", entidad = "producto")
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidad(id);
        if (repository.existsByCodigoIgnoreCaseAndIdNot(request.codigo(), id)) {
            throw new NegocioException(CodigoError.DUPLICADO, "El codigo de producto ya existe");
        }
        aplicar(producto, request);
        return mapper.aResponse(repository.save(producto));
    }

    @Transactional
    @Auditado(accion = "CAMBIAR_ESTADO_PRODUCTO", entidad = "producto")
    public ProductoResponse cambiarEstado(Long id, boolean activo) {
        Producto producto = buscarEntidad(id);
        producto.setActivo(activo);
        return mapper.aResponse(repository.save(producto));
    }

    /** Actualiza el costo promedio ponderado; lo invoca el modulo de inventario. */
    @Transactional
    public void actualizarCostoPromedio(Long productoId, BigDecimal nuevoCosto) {
        Producto producto = buscarEntidad(productoId);
        producto.setCostoPromedio(nuevoCosto);
        repository.save(producto);
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setCodigo(request.codigo().toUpperCase().trim());
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("categoria", request.categoriaId())));
        producto.setUnidadMedida(unidadRepository.findById(request.unidadMedidaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("unidad de medida",
                        request.unidadMedidaId())));
        producto.setTipoProducto(request.tipoProducto());
        producto.setPresentacion(request.presentacion());
        producto.setMarca(request.marca());
        producto.setRegistroSanitario(request.registroSanitario());
        producto.setVenceRegistroSanitario(request.venceRegistroSanitario());
        producto.setCondicionAlmacenamiento(request.condicionAlmacenamiento() == null
                ? CondicionAlmacenamiento.AMBIENTE : request.condicionAlmacenamiento());
        boolean exigeTrazabilidad = request.tipoProducto().exigeTrazabilidad();
        producto.setControlaLote(exigeTrazabilidad || Boolean.TRUE.equals(request.controlaLote()));
        producto.setControlaVencimiento(exigeTrazabilidad
                || Boolean.TRUE.equals(request.controlaVencimiento()));
        producto.setVidaUtilMinimaDias(request.vidaUtilMinimaDias() == null
                ? 180 : request.vidaUtilMinimaDias());
        producto.setClasificacionAbc(request.clasificacionAbc() == null
                ? ClasificacionAbc.C : request.clasificacionAbc());
        producto.setStockMinimo(request.stockMinimo());
        producto.setPuntoReorden(request.puntoReorden());
        producto.setStockMaximo(request.stockMaximo());
        producto.setDiasCobertura(request.diasCobertura() == null ? 30 : request.diasCobertura());
        producto.setCritico(Boolean.TRUE.equals(request.critico()));
        if (request.activo() != null) {
            producto.setActivo(request.activo());
        }
        // Un producto que exige cadena de frio no puede clasificarse como no sanitario.
        if (producto.requiereCadenaFrio() && !producto.isControlaLote()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "Un producto de cadena de frio debe controlar lote y vencimiento");
        }
    }
}
