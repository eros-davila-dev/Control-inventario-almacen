package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.catalogo.repository.ProveedorRepository;
import pe.cid.sgai.inventario.application.dto.CambioEstadoLoteRequest;
import pe.cid.sgai.inventario.application.dto.LoteRequest;
import pe.cid.sgai.inventario.application.dto.LoteResponse;
import pe.cid.sgai.inventario.application.dto.TrazabilidadLoteResponse;
import pe.cid.sgai.inventario.application.mapper.InventarioMapper;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.repository.LoteRepository;
import pe.cid.sgai.inventario.repository.MovimientoLineaRepository;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.seguridad.infrastructure.ContextoSeguridad;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lotes, su estado sanitario y la trazabilidad asociada (HU-15 a HU-18).
 * Solo el quimico farmaceutico libera o retiene lotes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository repository;
    private final StockRepository stockRepository;
    private final MovimientoLineaRepository movimientoLineaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoService productoService;
    private final InventarioMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<LoteResponse> buscar(Long productoId, String texto, EstadoLote estado,
                                             LocalDate venceHasta, Pageable pageable) {
        return PageResponse.de(repository.buscar(productoId,
                texto == null || texto.isBlank() ? null : texto.trim(), estado,
                venceHasta == null ? LocalDate.of(2999, 12, 31) : venceHasta, pageable),
                this::aResponse);
    }

    @Transactional(readOnly = true)
    public LoteResponse obtener(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public Lote buscarEntidad(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("lote", id));
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> liberadosDe(Long productoId) {
        return repository.findAllByProductoIdAndEstadoLoteOrderByFechaVencimientoAsc(
                        productoId, EstadoLote.LIBERADO).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    @Auditado(accion = "CREAR_LOTE", entidad = "lote")
    public LoteResponse crear(LoteRequest request) {
        Producto producto = productoService.buscarEntidad(request.productoId());
        Lote lote = obtenerOCrear(producto, request.codigoLote(), resolverProveedor(request.proveedorId()),
                request.fechaFabricacion(), request.fechaVencimiento(),
                request.costoUnitario() == null ? BigDecimal.ZERO : request.costoUnitario(),
                !Boolean.FALSE.equals(request.iniciarEnCuarentena()));
        return aResponse(lote);
    }

    /**
     * Devuelve el lote existente o lo crea. Es el unico punto de creacion de lotes,
     * usado tambien por la recepcion de mercaderia (HU-30).
     */
    @Transactional
    public Lote obtenerOCrear(Producto producto, String codigoLote, Proveedor proveedor,
                              LocalDate fechaFabricacion, LocalDate fechaVencimiento,
                              BigDecimal costoUnitario, boolean iniciarEnCuarentena) {
        validarDatosLote(producto, codigoLote, fechaVencimiento);
        String codigo = codigoLote.trim().toUpperCase();
        return repository.buscarExistente(producto.getId(), codigo,
                        proveedor == null ? null : proveedor.getId())
                .map(existente -> {
                    if (fechaVencimiento != null && existente.getFechaVencimiento() == null) {
                        existente.setFechaVencimiento(fechaVencimiento);
                    }
                    if (CantidadUtil.esPositiva(costoUnitario)) {
                        existente.setCostoUnitario(CantidadUtil.costo(costoUnitario));
                    }
                    return existente;
                })
                .orElseGet(() -> repository.save(Lote.builder()
                        .producto(producto)
                        .codigoLote(codigo)
                        .proveedor(proveedor)
                        .fechaFabricacion(fechaFabricacion)
                        .fechaVencimiento(fechaVencimiento)
                        .costoUnitario(CantidadUtil.costo(costoUnitario))
                        .estadoLote(iniciarEnCuarentena && producto.getTipoProducto().exigeTrazabilidad()
                                ? EstadoLote.CUARENTENA : EstadoLote.LIBERADO)
                        .motivoEstado(iniciarEnCuarentena && producto.getTipoProducto().exigeTrazabilidad()
                                ? "Ingreso a cuarentena pendiente de liberacion del QF"
                                : "Liberado automaticamente: producto sin control sanitario")
                        .build()));
    }

    /** Liberacion, retencion o rechazo con motivo obligatorio (HU-16, HU-17). */
    @Transactional
    @Auditado(accion = "CAMBIAR_ESTADO_LOTE", entidad = "lote")
    public LoteResponse cambiarEstado(Long id, CambioEstadoLoteRequest request) {
        Lote lote = buscarEntidad(id);
        if (request.estado() == EstadoLote.LIBERADO && lote.estaVencido()) {
            throw new NegocioException(CodigoError.LOTE_VENCIDO,
                    "No se puede liberar un lote vencido");
        }
        lote.cambiarEstado(request.estado(), request.motivo(), ContextoSeguridad.username());
        return aResponse(repository.save(lote));
    }

    /** Reconstruye la historia del lote: donde esta y donde se consumio (HU-45). */
    @Transactional(readOnly = true)
    public TrazabilidadLoteResponse trazabilidad(Long id) {
        Lote lote = buscarEntidad(id);
        List<TrazabilidadLoteResponse.MovimientoLoteItem> movimientos =
                movimientoLineaRepository.historialDeLote(id).stream()
                        .map(linea -> new TrazabilidadLoteResponse.MovimientoLoteItem(
                                linea.getMovimiento().getId(),
                                linea.getMovimiento().getNumero(),
                                linea.getMovimiento().getTipoMovimiento().name(),
                                linea.getMovimiento().getTipoMovimiento().descripcion(),
                                linea.getMovimiento().getFecha(),
                                linea.getMovimiento().getAlmacenOrigen().getNombre(),
                                linea.getMovimiento().getAreaDestino(),
                                linea.getMovimiento().getCentroCosto(),
                                linea.getCantidad(),
                                linea.getMovimiento().getDocumentoNumero(),
                                linea.getMovimiento().getCreatedBy(),
                                linea.getMovimiento().getEstadoMovimiento().name()))
                        .toList();
        return new TrazabilidadLoteResponse(aResponse(lote),
                mapper.aStockResponses(stockRepository.porLote(id)), movimientos);
    }

    /** Tarea diaria: marca como VENCIDO todo lote cuya fecha de vencimiento paso (HU-18). */
    @Transactional
    public int marcarVencidos() {
        List<Lote> porVencer = repository.porMarcarVencidos(FechaUtil.hoy(),
                List.of(EstadoLote.VENCIDO, EstadoLote.RECHAZADO));
        porVencer.forEach(Lote::marcarVencido);
        repository.saveAll(porVencer);
        if (!porVencer.isEmpty()) {
            log.info("Se marcaron {} lote(s) como vencidos", porVencer.size());
        }
        return porVencer.size();
    }

    private void validarDatosLote(Producto producto, String codigoLote, LocalDate fechaVencimiento) {
        if (producto.isControlaLote() && (codigoLote == null || codigoLote.isBlank())) {
            throw new NegocioException(CodigoError.PRODUCTO_REQUIERE_LOTE,
                    "El producto %s exige registrar el codigo de lote".formatted(producto.getNombre()));
        }
        if (producto.isControlaVencimiento() && fechaVencimiento == null) {
            throw new NegocioException(CodigoError.PRODUCTO_REQUIERE_LOTE,
                    "El producto %s exige registrar la fecha de vencimiento"
                            .formatted(producto.getNombre()));
        }
    }

    private Proveedor resolverProveedor(Long proveedorId) {
        if (proveedorId == null) {
            return null;
        }
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("proveedor", proveedorId));
    }

    private LoteResponse aResponse(Lote lote) {
        BigDecimal saldo = stockRepository.porLote(lote.getId()).stream()
                .map(s -> CantidadUtil.nvl(s.getCantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return mapper.aResponse(lote, CantidadUtil.cantidad(saldo));
    }
}
