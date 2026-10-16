package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.Ubicacion;
import pe.cid.sgai.almacen.repository.UbicacionRepository;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.catalogo.repository.ProveedorRepository;
import pe.cid.sgai.inventario.application.dto.AjusteRequest;
import pe.cid.sgai.inventario.application.dto.AnulacionRequest;
import pe.cid.sgai.inventario.application.dto.IngresoRequest;
import pe.cid.sgai.inventario.application.dto.LineaIngresoRequest;
import pe.cid.sgai.inventario.application.dto.LineaSalidaRequest;
import pe.cid.sgai.inventario.application.dto.MovimientoResponse;
import pe.cid.sgai.inventario.application.dto.SalidaRequest;
import pe.cid.sgai.inventario.application.mapper.InventarioMapper;
import pe.cid.sgai.inventario.domain.EstadoMovimiento;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.MotivoAjuste;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.MovimientoLinea;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.domain.StockActualizadoEvent;
import pe.cid.sgai.inventario.domain.TipoMovimiento;
import pe.cid.sgai.inventario.repository.MovimientoRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de movimientos de inventario: ingresos, salidas, ajustes y anulaciones
 * (HU-19 a HU-24). Es el UNICO componente que modifica el saldo: concentra la
 * transaccionalidad, el calculo del costo promedio ponderado, el asiento de kardex
 * y la publicacion del evento de stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    /** Limites por defecto del rango de fechas en las consultas. */
    public static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);
    public static final LocalDate FECHA_MAXIMA = LocalDate.of(2999, 12, 31);

    private final MovimientoRepository repository;
    private final StockService stockService;
    private final KardexService kardexService;
    private final LoteService loteService;
    private final AsignacionLoteService asignacionLoteService;
    private final ProductoService productoService;
    private final AlmacenService almacenService;
    private final UbicacionRepository ubicacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final CorrelativoService correlativoService;
    private final ApplicationEventPublisher eventPublisher;
    private final InventarioMapper mapper;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<MovimientoResponse> buscar(Long almacenId, TipoMovimiento tipo,
                                                   EstadoMovimiento estado, LocalDate desde,
                                                   LocalDate hasta, String texto, Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, tipo, estado,
                desde == null ? FECHA_MINIMA : desde, hasta == null ? FECHA_MAXIMA : hasta,
                texto == null || texto.isBlank() ? null : texto.trim(), pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public MovimientoResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public Movimiento buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("movimiento", id));
    }

    // ------------------------------------------------------------------
    // Ingresos (HU-19)
    // ------------------------------------------------------------------

    @Transactional
    @Auditado(accion = "REGISTRAR_INGRESO", entidad = "movimiento")
    public MovimientoResponse registrarIngreso(IngresoRequest request, String idempotencyKey) {
        return mapper.aResponse(registrarIngresoInterno(request, idempotencyKey));
    }

    /** Variante para uso de otros modulos (recepcion de compras, transferencias, conteo). */
    @Transactional
    public Movimiento registrarIngresoInterno(IngresoRequest request, String idempotencyKey) {
        Movimiento existente = porIdempotencia(idempotencyKey);
        if (existente != null) {
            return existente;
        }
        if (!request.tipoMovimiento().esIngreso()) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "El tipo %s no corresponde a un ingreso".formatted(request.tipoMovimiento()));
        }
        Almacen almacen = almacenService.buscarOperable(request.almacenId());
        Proveedor proveedor = resolverProveedor(request.proveedorId());

        Movimiento movimiento = Movimiento.builder()
                .numero(correlativoService.siguiente(correlativoDe(request.tipoMovimiento())))
                .tipoMovimiento(request.tipoMovimiento())
                .estadoMovimiento(EstadoMovimiento.REGISTRADO)
                .fecha(request.fecha() == null ? FechaUtil.hoy() : request.fecha())
                .almacenOrigen(almacen)
                .motivoAjuste(request.motivoAjuste())
                .documentoTipo(request.documentoTipo())
                .documentoNumero(request.documentoNumero())
                .proveedor(proveedor)
                .observacion(request.observacion())
                .referenciaTipo(request.referenciaTipo())
                .referenciaId(request.referenciaId())
                .idempotencyKey(idempotencyKey)
                .usuarioId(ContextoSeguridad.usuarioActual()
                        .map(u -> u.id()).orElse(null))
                .build();

        for (LineaIngresoRequest linea : request.lineas()) {
            movimiento.agregarLinea(construirLineaIngreso(linea, request, proveedor));
        }
        Movimiento guardado = repository.save(movimiento);

        // Aplicacion de saldos: costo promedio -> stock -> kardex -> evento.
        for (MovimientoLinea linea : guardado.getLineas()) {
            BigDecimal nuevoCosto = recalcularCostoPromedio(linea.getProducto(),
                    linea.getCantidadBase(), linea.getCostoUnitario());
            stockService.ingresar(linea.getProducto(), almacen, linea.getLote(),
                    linea.getUbicacion(), linea.getCantidadBase());
            kardexService.registrar(guardado, linea, almacen.getId(), true, nuevoCosto);
            publicarEvento(linea.getProducto().getId(), almacen.getId());
        }
        log.info("Ingreso {} registrado en {} con {} linea(s)", guardado.getNumero(),
                almacen.getCodigo(), guardado.getLineas().size());
        return guardado;
    }

    // ------------------------------------------------------------------
    // Salidas (HU-20, HU-21)
    // ------------------------------------------------------------------

    @Transactional
    @Auditado(accion = "REGISTRAR_SALIDA", entidad = "movimiento")
    public MovimientoResponse registrarSalida(SalidaRequest request, String idempotencyKey) {
        validarDesviacionFefo(request);
        return mapper.aResponse(registrarSalidaInterna(request, idempotencyKey));
    }

    /**
     * Cuando el usuario elige manualmente un lote distinto al que FEFO propone,
     * se exige el motivo de la desviacion (HU-21). Solo aplica a las salidas que
     * llegan por la API: las que generan despacho, transferencia o conteo ya
     * vienen con los lotes resueltos por la propia politica FEFO.
     */
    private void validarDesviacionFefo(SalidaRequest request) {
        // Primero el estado sanitario del lote: da un error mas preciso que FEFO.
        request.lineas().stream()
                .filter(linea -> linea.loteId() != null)
                .map(linea -> loteService.buscarEntidad(linea.loteId()))
                .filter(lote -> !lote.esDespachable())
                .findFirst()
                .ifPresent(lote -> {
                    throw new NegocioException(CodigoError.LOTE_NO_DISPONIBLE,
                            "El lote %s esta en estado %s y no puede despacharse"
                                    .formatted(lote.getCodigoLote(), lote.getEstadoLote()))
                            .con("loteId", lote.getId())
                            .con("estado", lote.getEstadoLote().name());
                });
        request.lineas().stream()
                .filter(linea -> linea.loteId() != null)
                .filter(linea -> !asignacionLoteService.esLoteFefo(linea.productoId(),
                        request.almacenId(), linea.loteId()))
                .filter(linea -> linea.motivoNoFefo() == null || linea.motivoNoFefo().isBlank())
                .findFirst()
                .ifPresent(linea -> {
                    throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                            "El lote elegido no es el proximo a vencer: indique el motivo "
                                    + "de la desviacion FEFO").con("loteId", linea.loteId());
                });
    }

    @Transactional
    public Movimiento registrarSalidaInterna(SalidaRequest request, String idempotencyKey) {
        Movimiento existente = porIdempotencia(idempotencyKey);
        if (existente != null) {
            return existente;
        }
        if (!request.tipoMovimiento().esSalida()) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "El tipo %s no corresponde a una salida".formatted(request.tipoMovimiento()));
        }
        Almacen almacen = almacenService.buscarOperable(request.almacenId());
        if (request.tipoMovimiento() == TipoMovimiento.SALIDA_DESPACHO && !almacen.esDespachable()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "No se puede despachar desde un almacen de tipo "
                            + almacen.getTipoAlmacen().descripcion());
        }

        Movimiento movimiento = Movimiento.builder()
                .numero(correlativoService.siguiente(correlativoDe(request.tipoMovimiento())))
                .tipoMovimiento(request.tipoMovimiento())
                .estadoMovimiento(EstadoMovimiento.REGISTRADO)
                .fecha(request.fecha() == null ? FechaUtil.hoy() : request.fecha())
                .almacenOrigen(almacen)
                .motivoAjuste(request.motivoAjuste())
                .documentoTipo(request.documentoTipo())
                .documentoNumero(request.documentoNumero())
                .centroCosto(request.centroCosto())
                .areaDestino(request.areaDestino())
                .observacion(request.observacion())
                .referenciaTipo(request.referenciaTipo())
                .referenciaId(request.referenciaId())
                .idempotencyKey(idempotencyKey)
                .usuarioId(ContextoSeguridad.usuarioActual().map(u -> u.id()).orElse(null))
                .build();

        for (LineaSalidaRequest linea : request.lineas()) {
            construirLineasSalida(linea, almacen).forEach(movimiento::agregarLinea);
        }
        Movimiento guardado = repository.save(movimiento);

        for (MovimientoLinea linea : guardado.getLineas()) {
            stockService.retirar(linea.getProducto(), almacen, linea.getLote(),
                    linea.getUbicacion(), linea.getCantidadBase());
            kardexService.registrar(guardado, linea, almacen.getId(), false,
                    linea.getProducto().getCostoPromedio());
            publicarEvento(linea.getProducto().getId(), almacen.getId());
        }
        log.info("Salida {} registrada en {} con {} linea(s)", guardado.getNumero(),
                almacen.getCodigo(), guardado.getLineas().size());
        return guardado;
    }

    // ------------------------------------------------------------------
    // Ajustes (HU-23)
    // ------------------------------------------------------------------

    @Transactional
    @Auditado(accion = "REGISTRAR_AJUSTE", entidad = "movimiento")
    public MovimientoResponse registrarAjuste(AjusteRequest request) {
        return mapper.aResponse(registrarAjusteInterno(request, null));
    }

    @Transactional
    public Movimiento registrarAjusteInterno(AjusteRequest request, String referenciaTipo) {
        boolean incrementa = Boolean.TRUE.equals(request.incrementa());
        if (incrementa) {
            List<LineaIngresoRequest> lineas = request.lineas().stream()
                    .map(l -> new LineaIngresoRequest(l.productoId(), null, null, null, l.loteId(),
                            l.ubicacionId(), l.cantidad(),
                            l.costoUnitario() == null
                                    ? productoService.buscarEntidad(l.productoId()).getCostoPromedio()
                                    : l.costoUnitario()))
                    .toList();
            return registrarIngresoInterno(new IngresoRequest(request.almacenId(),
                    TipoMovimiento.INGRESO_AJUSTE, FechaUtil.hoy(), "AJUSTE", null, null,
                    request.motivoAjuste(), request.observacion(), referenciaTipo, null, lineas), null);
        }
        List<LineaSalidaRequest> lineas = request.lineas().stream()
                .map(l -> new LineaSalidaRequest(l.productoId(), l.loteId(), l.ubicacionId(),
                        l.cantidad(), "Ajuste por " + request.motivoAjuste().descripcion()))
                .toList();
        TipoMovimiento tipo = request.motivoAjuste() == MotivoAjuste.VENCIMIENTO
                ? TipoMovimiento.SALIDA_VENCIMIENTO
                : (request.motivoAjuste() == MotivoAjuste.MERMA || request.motivoAjuste() == MotivoAjuste.ROTURA
                ? TipoMovimiento.SALIDA_MERMA : TipoMovimiento.SALIDA_AJUSTE);
        return registrarSalidaInterna(new SalidaRequest(request.almacenId(), tipo, FechaUtil.hoy(),
                null, null, request.motivoAjuste(), "AJUSTE", null, request.observacion(),
                referenciaTipo, null, lineas), null);
    }

    // ------------------------------------------------------------------
    // Anulacion con movimiento inverso (HU-24)
    // ------------------------------------------------------------------

    @Transactional
    @Auditado(accion = "ANULAR_MOVIMIENTO", entidad = "movimiento")
    public MovimientoResponse anular(Long id, AnulacionRequest request) {
        Movimiento original = buscarEntidad(id);
        if (original.estaAnulado()) {
            throw new NegocioException(CodigoError.MOVIMIENTO_ANULADO,
                    "El movimiento %s ya fue anulado".formatted(original.getNumero()));
        }
        if (repository.existsByMovimientoAnuladoId(id)) {
            throw new NegocioException(CodigoError.CONFLICTO,
                    "El movimiento ya tiene un documento de anulacion asociado");
        }
        Almacen almacen = almacenService.buscarOperable(original.getAlmacenOrigen().getId());
        TipoMovimiento tipoInverso = original.getTipoMovimiento().inverso();

        Movimiento inverso = Movimiento.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.AJU))
                .tipoMovimiento(tipoInverso)
                .estadoMovimiento(EstadoMovimiento.REGISTRADO)
                .fecha(FechaUtil.hoy())
                .almacenOrigen(almacen)
                .motivoAjuste(MotivoAjuste.ERROR_REGISTRO)
                .documentoTipo("ANULACION")
                .documentoNumero(original.getNumero())
                .observacion("Anulacion de %s: %s".formatted(original.getNumero(), request.motivo()))
                .movimientoAnuladoId(original.getId())
                .referenciaTipo("MOVIMIENTO")
                .referenciaId(original.getId())
                .usuarioId(ContextoSeguridad.usuarioActual().map(u -> u.id()).orElse(null))
                .build();

        original.getLineas().forEach(linea -> inverso.agregarLinea(MovimientoLinea.builder()
                .producto(linea.getProducto())
                .lote(linea.getLote())
                .ubicacion(linea.getUbicacion())
                .cantidad(linea.getCantidad())
                .cantidadBase(linea.getCantidadBase())
                .costoUnitario(linea.getCostoUnitario())
                .costoTotal(linea.getCostoTotal())
                .fefoRespetado(true)
                .motivoNoFefo("Movimiento de anulacion")
                .build()));

        Movimiento guardado = repository.save(inverso);
        boolean esEntrada = tipoInverso.esIngreso();
        for (MovimientoLinea linea : guardado.getLineas()) {
            if (esEntrada) {
                stockService.ingresar(linea.getProducto(), almacen, linea.getLote(),
                        linea.getUbicacion(), linea.getCantidadBase());
            } else {
                stockService.retirar(linea.getProducto(), almacen, linea.getLote(),
                        linea.getUbicacion(), linea.getCantidadBase());
            }
            kardexService.registrar(guardado, linea, almacen.getId(), esEntrada,
                    linea.getProducto().getCostoPromedio());
            publicarEvento(linea.getProducto().getId(), almacen.getId());
        }

        original.setEstadoMovimiento(EstadoMovimiento.ANULADO);
        original.setObservacion((original.getObservacion() == null ? "" : original.getObservacion() + " | ")
                + "ANULADO por " + guardado.getNumero() + ": " + request.motivo());
        repository.save(original);
        return mapper.aResponse(guardado);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private MovimientoLinea construirLineaIngreso(LineaIngresoRequest linea, IngresoRequest request,
                                                  Proveedor proveedor) {
        Producto producto = productoService.buscarEntidad(linea.productoId());
        Lote lote = null;
        if (producto.isControlaLote()) {
            if (linea.loteId() != null) {
                lote = loteService.buscarEntidad(linea.loteId());
            } else {
                lote = loteService.obtenerOCrear(producto, linea.codigoLote(), proveedor,
                        linea.fechaFabricacion(), linea.fechaVencimiento(),
                        linea.costoUnitario(),
                        request.tipoMovimiento() == TipoMovimiento.INGRESO_COMPRA);
            }
            if (request.tipoMovimiento() == TipoMovimiento.INGRESO_COMPRA && lote.estaVencido()) {
                throw new NegocioException(CodigoError.LOTE_VENCIDO,
                        "No se puede recepcionar el lote %s: vencio el %s"
                                .formatted(lote.getCodigoLote(), lote.getFechaVencimiento()));
            }
        }
        MovimientoLinea entidad = MovimientoLinea.builder()
                .producto(producto)
                .lote(lote)
                .ubicacion(resolverUbicacion(linea.ubicacionId(), request.almacenId()))
                .cantidad(CantidadUtil.cantidad(linea.cantidad()))
                .cantidadBase(CantidadUtil.cantidad(linea.cantidad()))
                .costoUnitario(CantidadUtil.costo(linea.costoUnitario()))
                .fefoRespetado(true)
                .build();
        entidad.calcularCostoTotal();
        return entidad;
    }

    /**
     * Construye las lineas de una salida. Si el producto controla lote y no se
     * indico uno, el sistema reparte la cantidad por FEFO; si se indico un lote
     * distinto al que FEFO propone, se exige motivo y queda registrado (HU-21).
     */
    private List<MovimientoLinea> construirLineasSalida(LineaSalidaRequest linea, Almacen almacen) {
        Producto producto = productoService.buscarEntidad(linea.productoId());
        List<MovimientoLinea> resultado = new ArrayList<>();

        if (producto.isControlaLote() && linea.loteId() == null) {
            asignacionLoteService.asignar(producto.getId(), almacen.getId(), linea.cantidad())
                    .forEach(asignacion -> resultado.add(
                            lineaDesdeStock(asignacion.stock(), asignacion.cantidad(), true, null)));
            return resultado;
        }

        Lote lote = linea.loteId() == null ? null : loteService.buscarEntidad(linea.loteId());
        if (lote != null && !lote.esDespachable()) {
            throw new NegocioException(CodigoError.LOTE_NO_DISPONIBLE,
                    "El lote %s esta en estado %s y no puede despacharse"
                            .formatted(lote.getCodigoLote(), lote.getEstadoLote()))
                    .con("loteId", lote.getId())
                    .con("estado", lote.getEstadoLote().name());
        }
        // Se deja constancia de si el lote consumido era el propuesto por FEFO.
        boolean esFefo = lote == null || asignacionLoteService.esLoteFefo(producto.getId(),
                almacen.getId(), lote.getId());
        // El saldo puede estar repartido en varias ubicaciones del mismo lote:
        // se consume celda por celda para no dejar el stock descuadrado.
        Ubicacion ubicacion = resolverUbicacion(linea.ubicacionId(), almacen.getId());
        stockService.distribuirSalida(producto, almacen, lote, ubicacion, linea.cantidad())
                .forEach(asignacion -> resultado.add(lineaDesdeStock(asignacion.stock(),
                        asignacion.cantidad(), esFefo, esFefo ? null : linea.motivoNoFefo())));
        return resultado;
    }

    private MovimientoLinea lineaDesdeStock(Stock stock, BigDecimal cantidad, boolean fefoRespetado,
                                            String motivoNoFefo) {
        BigDecimal costo = stock.getLote() != null
                && stock.getLote().getCostoUnitario().compareTo(BigDecimal.ZERO) > 0
                ? stock.getLote().getCostoUnitario() : stock.getProducto().getCostoPromedio();
        MovimientoLinea linea = MovimientoLinea.builder()
                .producto(stock.getProducto())
                .lote(stock.getLote())
                .ubicacion(stock.getUbicacion())
                .cantidad(CantidadUtil.cantidad(cantidad))
                .cantidadBase(CantidadUtil.cantidad(cantidad))
                .costoUnitario(CantidadUtil.costo(costo))
                .fefoRespetado(fefoRespetado)
                .motivoNoFefo(motivoNoFefo)
                .build();
        linea.calcularCostoTotal();
        return linea;
    }

    /**
     * Costo promedio ponderado: CPP' = (saldo x CPP + cantidad x costo) / (saldo + cantidad).
     * Se calcula ANTES de aplicar el ingreso, con el saldo vigente del producto.
     */
    private BigDecimal recalcularCostoPromedio(Producto producto, BigDecimal cantidadIngreso,
                                               BigDecimal costoUnitario) {
        BigDecimal saldoAnterior = stockService.saldoTotalProducto(producto.getId());
        BigDecimal cppAnterior = CantidadUtil.nvl(producto.getCostoPromedio());
        BigDecimal nuevoSaldo = saldoAnterior.add(CantidadUtil.nvl(cantidadIngreso));
        BigDecimal nuevoCpp;
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {
            nuevoCpp = CantidadUtil.costo(costoUnitario);
        } else {
            BigDecimal valorAnterior = saldoAnterior.multiply(cppAnterior);
            BigDecimal valorIngreso = CantidadUtil.nvl(cantidadIngreso)
                    .multiply(CantidadUtil.nvl(costoUnitario));
            nuevoCpp = valorAnterior.add(valorIngreso)
                    .divide(nuevoSaldo, CantidadUtil.ESCALA_COSTO, RoundingMode.HALF_UP);
        }
        producto.setCostoPromedio(nuevoCpp);
        productoService.actualizarCostoPromedio(producto.getId(), nuevoCpp);
        return nuevoCpp;
    }

    private Ubicacion resolverUbicacion(Long ubicacionId, Long almacenId) {
        if (ubicacionId == null) {
            return null;
        }
        Ubicacion ubicacion = ubicacionRepository.findById(ubicacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("ubicacion", ubicacionId));
        if (!ubicacion.getAlmacen().getId().equals(almacenId)) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "La ubicacion %s no pertenece al almacen indicado".formatted(ubicacion.getCodigo()));
        }
        return ubicacion;
    }

    private Proveedor resolverProveedor(Long proveedorId) {
        if (proveedorId == null) {
            return null;
        }
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("proveedor", proveedorId));
    }

    private Movimiento porIdempotencia(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return repository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private void publicarEvento(Long productoId, Long almacenId) {
        eventPublisher.publishEvent(new StockActualizadoEvent(productoId, almacenId,
                stockService.disponibleEn(productoId, almacenId)));
    }

    private static TipoCorrelativo correlativoDe(TipoMovimiento tipo) {
        return switch (tipo) {
            case INGRESO_AJUSTE, SALIDA_AJUSTE -> TipoCorrelativo.AJU;
            case INGRESO_TRANSFERENCIA, SALIDA_TRANSFERENCIA -> TipoCorrelativo.TRA;
            case INGRESO_COMPRA, INGRESO_DEVOLUCION -> TipoCorrelativo.ING;
            default -> TipoCorrelativo.SAL;
        };
    }
}
