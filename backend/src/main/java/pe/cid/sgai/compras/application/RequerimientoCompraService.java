package pe.cid.sgai.compras.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.compras.application.dto.RequerimientoRequest;
import pe.cid.sgai.compras.application.dto.RequerimientoResponse;
import pe.cid.sgai.compras.application.dto.SugerenciaReposicionResponse;
import pe.cid.sgai.compras.application.mapper.ComprasMapper;
import pe.cid.sgai.compras.domain.EstadoRequerimiento;
import pe.cid.sgai.compras.domain.RequerimientoCompra;
import pe.cid.sgai.compras.domain.RequerimientoCompraLinea;
import pe.cid.sgai.compras.repository.RequerimientoCompraRepository;
import pe.cid.sgai.inventario.application.StockService;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.seguridad.infrastructure.ContextoSeguridad;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.CorrelativoService;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.util.TipoCorrelativo;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Requerimientos de compra. La reposicion la propone el sistema a partir del punto
 * de reorden y del consumo promedio, de modo que el quiebre de insumos criticos deja
 * de depender de la memoria del almacenero (problema P3).
 */
@Service
@RequiredArgsConstructor
public class RequerimientoCompraService {

    private final RequerimientoCompraRepository repository;
    private final AlmacenService almacenService;
    private final ProductoService productoService;
    private final StockService stockService;
    private final CorrelativoService correlativoService;
    private final ComprasMapper mapper;

    /** Propuesta de reposicion del almacen (HU-27). */
    @Transactional(readOnly = true)
    public List<SugerenciaReposicionResponse> sugerirReposicion(Long almacenId) {
        almacenService.buscarOperable(almacenId);
        return stockService.consolidado(almacenId, null, null, true).stream()
                .map(this::aSugerencia)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RequerimientoResponse> buscar(Long almacenId, EstadoRequerimiento estado,
                                                      Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, estado, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public RequerimientoResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public RequerimientoCompra buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("requerimiento de compra", id));
    }

    @Transactional
    @Auditado(accion = "CREAR_REQUERIMIENTO", entidad = "requerimiento_compra")
    public RequerimientoResponse crear(RequerimientoRequest request) {
        Almacen almacen = almacenService.buscarOperable(request.almacenId());
        RequerimientoCompra requerimiento = RequerimientoCompra.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.RQC))
                .almacen(almacen)
                .fecha(FechaUtil.hoy())
                .estado(EstadoRequerimiento.BORRADOR)
                .justificacion(request.justificacion())
                .solicitanteId(ContextoSeguridad.idUsuarioActual())
                .build();

        request.lineas().forEach(linea -> {
            var producto = productoService.buscarEntidad(linea.productoId());
            BigDecimal disponible = stockService.disponibleEn(producto.getId(), almacen.getId());
            requerimiento.agregarLinea(RequerimientoCompraLinea.builder()
                    .producto(producto)
                    .cantidadSolicitada(CantidadUtil.cantidad(linea.cantidadSolicitada()))
                    .cantidadSugerida(calcularSugerida(producto.getStockMaximo(), disponible,
                            stockService.consumoPromedioDiario(producto.getId(), almacen.getId()),
                            producto.getDiasCobertura()))
                    .disponibleActual(disponible)
                    .observacion(linea.observacion())
                    .build());
        });
        return mapper.aResponse(repository.save(requerimiento));
    }

    @Transactional
    @Auditado(accion = "ENVIAR_REQUERIMIENTO", entidad = "requerimiento_compra")
    public RequerimientoResponse enviar(Long id) {
        RequerimientoCompra requerimiento = buscarEntidad(id);
        requerimiento.cambiarEstado(EstadoRequerimiento.ENVIADO);
        return mapper.aResponse(repository.save(requerimiento));
    }

    @Transactional
    @Auditado(accion = "APROBAR_REQUERIMIENTO", entidad = "requerimiento_compra")
    public RequerimientoResponse aprobar(Long id) {
        RequerimientoCompra requerimiento = buscarEntidad(id);
        verificarSegregacion(requerimiento.getCreatedBy());
        requerimiento.cambiarEstado(EstadoRequerimiento.APROBADO);
        requerimiento.setAprobador(ContextoSeguridad.username());
        requerimiento.setFechaAprobacion(Instant.now());
        return mapper.aResponse(repository.save(requerimiento));
    }

    @Transactional
    @Auditado(accion = "RECHAZAR_REQUERIMIENTO", entidad = "requerimiento_compra")
    public RequerimientoResponse rechazar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException(CodigoError.VALIDACION, "Indique el motivo del rechazo");
        }
        RequerimientoCompra requerimiento = buscarEntidad(id);
        requerimiento.cambiarEstado(EstadoRequerimiento.RECHAZADO);
        requerimiento.setAprobador(ContextoSeguridad.username());
        requerimiento.setFechaAprobacion(Instant.now());
        requerimiento.setMotivoRechazo(motivo);
        return mapper.aResponse(repository.save(requerimiento));
    }

    /** Marca el requerimiento como atendido cuando se genera su orden de compra. */
    @Transactional
    public void marcarAtendido(RequerimientoCompra requerimiento) {
        if (requerimiento.getEstado() == EstadoRequerimiento.APROBADO) {
            requerimiento.cambiarEstado(EstadoRequerimiento.ATENDIDO);
            repository.save(requerimiento);
        }
    }

    private SugerenciaReposicionResponse aSugerencia(StockConsolidadoResponse stock) {
        BigDecimal sugerida = calcularSugerida(stock.stockMaximo(), stock.disponible(),
                stock.consumoPromedioDiario(), diasCoberturaDe(stock.productoId()));
        return new SugerenciaReposicionResponse(stock.productoId(), stock.productoCodigo(),
                stock.productoNombre(), stock.unidadMedida(), stock.critico(), stock.disponible(),
                stock.stockMinimo(), stock.puntoReorden(), stock.stockMaximo(),
                stock.consumoPromedioDiario(), stock.coberturaDias(), sugerida,
                stock.estadoReposicion());
    }

    private int diasCoberturaDe(Long productoId) {
        return productoService.buscarEntidad(productoId).getDiasCobertura();
    }

    /** cantidad = max(stockMaximo - disponible, consumoPromedioDiario x diasCobertura). */
    private BigDecimal calcularSugerida(BigDecimal stockMaximo, BigDecimal disponible,
                                        BigDecimal consumoPromedioDiario, int diasCobertura) {
        BigDecimal porMaximo = CantidadUtil.nvl(stockMaximo).subtract(CantidadUtil.nvl(disponible));
        BigDecimal porConsumo = CantidadUtil.nvl(consumoPromedioDiario)
                .multiply(BigDecimal.valueOf(diasCobertura))
                .subtract(CantidadUtil.nvl(disponible));
        BigDecimal sugerida = porMaximo.max(porConsumo);
        return CantidadUtil.cantidad(sugerida.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO : sugerida);
    }

    /** Segregacion de funciones: quien registra no aprueba (salvo ADMIN). */
    private void verificarSegregacion(String creadoPor) {
        var usuario = ContextoSeguridad.requerido();
        if (!usuario.esAdministrador() && usuario.username().equalsIgnoreCase(creadoPor)) {
            throw new NegocioException(CodigoError.SEGREGACION_FUNCIONES,
                    "Quien registra el documento no puede aprobarlo");
        }
    }
}
