package pe.cid.sgai.compras.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.compras.application.dto.RecepcionRequest;
import pe.cid.sgai.compras.application.dto.RecepcionResponse;
import pe.cid.sgai.compras.application.mapper.ComprasMapper;
import pe.cid.sgai.compras.domain.OrdenCompra;
import pe.cid.sgai.compras.domain.OrdenCompraLinea;
import pe.cid.sgai.compras.domain.Recepcion;
import pe.cid.sgai.compras.domain.RecepcionLinea;
import pe.cid.sgai.compras.domain.ResultadoCalidad;
import pe.cid.sgai.compras.repository.RecepcionRepository;
import pe.cid.sgai.inventario.application.LoteService;
import pe.cid.sgai.inventario.application.MovimientoService;
import pe.cid.sgai.inventario.application.dto.IngresoRequest;
import pe.cid.sgai.inventario.application.dto.LineaIngresoRequest;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.TipoMovimiento;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Recepcion de mercaderia contra orden de compra (HU-30) con control de calidad
 * sanitario (HU-31): registro sanitario vigente, vida util remanente minima,
 * cadena de frio e integridad del empaque. El resultado determina si la mercaderia
 * ingresa al almacen destino, a cuarentena o no ingresa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecepcionService {

    private static final BigDecimal TEMP_MIN_FRIO = new BigDecimal("2");
    private static final BigDecimal TEMP_MAX_FRIO = new BigDecimal("8");
    private static final String REFERENCIA = "RECEPCION";

    private final RecepcionRepository repository;
    private final OrdenCompraService ordenCompraService;
    private final AlmacenService almacenService;
    private final LoteService loteService;
    private final MovimientoService movimientoService;
    private final CorrelativoService correlativoService;
    private final ComprasMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RecepcionResponse> buscar(Long almacenId, Long ordenId, Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, ordenId, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public RecepcionResponse obtener(Long id) {
        return mapper.aResponse(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("recepcion", id)));
    }

    @Transactional(readOnly = true)
    public List<RecepcionResponse> porOrden(Long ordenId) {
        return repository.findAllByOrdenIdOrderByFechaAsc(ordenId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    @Transactional
    @Auditado(accion = "REGISTRAR_RECEPCION", entidad = "recepcion")
    public RecepcionResponse registrar(RecepcionRequest request) {
        OrdenCompra orden = ordenCompraService.buscarEntidad(request.ordenId());
        if (!orden.getEstado().admiteRecepcion()) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La orden %s esta en estado %s y no admite recepcion"
                            .formatted(orden.getNumero(), orden.getEstado()));
        }
        Almacen almacenOrden = almacenService.buscarOperable(orden.getAlmacenDestino().getId());

        Recepcion recepcion = Recepcion.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.REC))
                .orden(orden)
                .almacen(almacenOrden)
                .fecha(request.fecha() == null ? FechaUtil.hoy() : request.fecha())
                .guiaRemision(request.guiaRemision())
                .factura(request.factura())
                .transportista(request.transportista())
                .temperaturaLlegada(request.temperaturaLlegada())
                .empaqueIntegro(!Boolean.FALSE.equals(request.empaqueIntegro()))
                .resultadoCalidad(request.resultadoCalidad())
                .observacion(request.observacion())
                .recibidoPor(ContextoSeguridad.username())
                .build();

        List<LineaIngresoRequest> lineasIngreso = new ArrayList<>();

        for (RecepcionRequest.LineaRecepcionRequest linea : request.lineas()) {
            OrdenCompraLinea ordenLinea = orden.getLineas().stream()
                    .filter(l -> l.getId().equals(linea.ordenLineaId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("linea de orden de compra",
                            linea.ordenLineaId()));
            Producto producto = ordenLinea.getProducto();
            BigDecimal cantidad = CantidadUtil.cantidad(linea.cantidad());

            controlarCalidad(producto, linea, request, recepcion.getResultadoCalidad());
            ordenLinea.registrarRecepcion(cantidad);

            Lote lote = null;
            if (producto.isControlaLote() && request.resultadoCalidad().generaIngreso()) {
                lote = loteService.obtenerOCrear(producto, linea.codigoLote(), orden.getProveedor(),
                        linea.fechaFabricacion(), linea.fechaVencimiento(),
                        ordenLinea.getPrecioUnitario(), true);
            }

            recepcion.agregarLinea(RecepcionLinea.builder()
                    .ordenLinea(ordenLinea)
                    .producto(producto)
                    .codigoLote(linea.codigoLote())
                    .fechaVencimiento(linea.fechaVencimiento())
                    .cantidad(cantidad)
                    .precioUnitario(ordenLinea.getPrecioUnitario())
                    .lote(lote)
                    .conforme(!Boolean.FALSE.equals(linea.conforme()))
                    .observacion(linea.observacion())
                    .build());

            if (request.resultadoCalidad().generaIngreso()) {
                lineasIngreso.add(new LineaIngresoRequest(producto.getId(), linea.codigoLote(),
                        linea.fechaFabricacion(), linea.fechaVencimiento(),
                        lote == null ? null : lote.getId(),
                        request.resultadoCalidad() == ResultadoCalidad.CONFORME
                                ? request.ubicacionId() : null,
                        cantidad, ordenLinea.getPrecioUnitario()));
            }
        }

        Recepcion guardada = repository.save(recepcion);

        // Lo observado entra al almacen de cuarentena de la sede: no se mezcla con
        // mercaderia liberada hasta que el QF resuelva (HU-31).
        if (request.resultadoCalidad().generaIngreso() && !lineasIngreso.isEmpty()) {
            Almacen almacenIngreso = request.resultadoCalidad() == ResultadoCalidad.CONFORME
                    ? almacenOrden : almacenService.cuarentenaDe(almacenOrden);
            Movimiento movimiento = movimientoService.registrarIngresoInterno(new IngresoRequest(
                    almacenIngreso.getId(), TipoMovimiento.INGRESO_COMPRA, guardada.getFecha(),
                    "GUIA_REMISION", request.guiaRemision(), orden.getProveedor().getId(), null,
                    "Recepcion %s de la orden %s".formatted(guardada.getNumero(), orden.getNumero()),
                    REFERENCIA, guardada.getId(), lineasIngreso), null);
            guardada.setMovimientoId(movimiento.getId());
            repository.save(guardada);
        } else {
            log.warn("Recepcion {} rechazada en control de calidad: {}", guardada.getNumero(),
                    request.observacion());
        }

        orden.actualizarEstadoPorRecepcion();
        ordenCompraService.guardar(orden);
        return mapper.aResponse(guardada);
    }

    /**
     * Control de calidad sanitario. Si el usuario declara CONFORME pero las
     * condiciones no se cumplen, la operacion se rechaza: el sistema no permite
     * ingresar al inventario mercaderia que no cumple.
     */
    private void controlarCalidad(Producto producto, RecepcionRequest.LineaRecepcionRequest linea,
                                  RecepcionRequest request, ResultadoCalidad resultado) {
        if (resultado != ResultadoCalidad.CONFORME) {
            if (request.observacion() == null || request.observacion().isBlank()) {
                throw new NegocioException(CodigoError.VALIDACION,
                        "Una recepcion observada o rechazada exige registrar la observacion");
            }
            return;
        }
        if (!producto.registroSanitarioVigente()) {
            throw new NegocioException(CodigoError.REGISTRO_SANITARIO_VENCIDO,
                    "El registro sanitario de %s vencio el %s".formatted(producto.getNombre(),
                            producto.getVenceRegistroSanitario()));
        }
        if (producto.isControlaVencimiento() && linea.fechaVencimiento() == null) {
            throw new NegocioException(CodigoError.PRODUCTO_REQUIERE_LOTE,
                    "Registre la fecha de vencimiento de " + producto.getNombre());
        }
        if (!producto.vidaUtilSuficiente(linea.fechaVencimiento())) {
            throw new NegocioException(CodigoError.VIDA_UTIL_INSUFICIENTE,
                    "El lote de %s vence el %s: la vida util remanente es menor a %d dias"
                            .formatted(producto.getNombre(), linea.fechaVencimiento(),
                                    producto.getVidaUtilMinimaDias()));
        }
        if (producto.requiereCadenaFrio()) {
            BigDecimal temperatura = request.temperaturaLlegada();
            if (temperatura == null) {
                throw new NegocioException(CodigoError.VALIDACION,
                        "Registre la temperatura de llegada: %s requiere cadena de frio"
                                .formatted(producto.getNombre()));
            }
            if (temperatura.compareTo(TEMP_MIN_FRIO) < 0 || temperatura.compareTo(TEMP_MAX_FRIO) > 0) {
                throw new NegocioException(CodigoError.CALIDAD_RECHAZADA,
                        ("La temperatura de llegada (%s C) esta fuera del rango 2-8 C: "
                                + "marque la recepcion como OBSERVADA o RECHAZADA")
                                .formatted(temperatura.toPlainString()));
            }
        }
        if (!recepcionConEmpaqueIntegro(request)) {
            throw new NegocioException(CodigoError.CALIDAD_RECHAZADA,
                    "El empaque no esta integro: marque la recepcion como OBSERVADA o RECHAZADA");
        }
    }

    private boolean recepcionConEmpaqueIntegro(RecepcionRequest request) {
        return !Boolean.FALSE.equals(request.empaqueIntegro());
    }
}
