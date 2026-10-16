package pe.cid.sgai.despacho.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.despacho.application.dto.DespachoRequest;
import pe.cid.sgai.despacho.application.dto.DespachoResponse;
import pe.cid.sgai.despacho.application.mapper.DespachoMapper;
import pe.cid.sgai.despacho.domain.Despacho;
import pe.cid.sgai.despacho.domain.DespachoLinea;
import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.despacho.domain.Requisicion;
import pe.cid.sgai.despacho.domain.RequisicionLinea;
import pe.cid.sgai.despacho.repository.DespachoRepository;
import pe.cid.sgai.inventario.application.AsignacionLoteService;
import pe.cid.sgai.inventario.application.LoteService;
import pe.cid.sgai.inventario.application.MovimientoService;
import pe.cid.sgai.inventario.application.dto.LineaSalidaRequest;
import pe.cid.sgai.inventario.application.dto.SalidaRequest;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atencion de requisiciones con politica FEFO (HU-33). Resuelve el problema P5:
 * el almacenero ya no elige el lote mas accesible, el sistema propone y registra
 * el que vence primero, y cualquier desviacion queda justificada y auditada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DespachoService {

    private static final String REFERENCIA = "DESPACHO";

    private final DespachoRepository repository;
    private final RequisicionService requisicionService;
    private final AlmacenService almacenService;
    private final MovimientoService movimientoService;
    private final AsignacionLoteService asignacionLoteService;
    private final LoteService loteService;
    private final CorrelativoService correlativoService;
    private final DespachoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<DespachoResponse> buscar(Long almacenId, LocalDate desde, LocalDate hasta,
                                                 Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId,
                desde == null ? LocalDate.of(2000, 1, 1) : desde,
                hasta == null ? LocalDate.of(2999, 12, 31) : hasta, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public DespachoResponse obtener(Long id) {
        return mapper.aResponse(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("despacho", id)));
    }

    @Transactional(readOnly = true)
    public List<DespachoResponse> porRequisicion(Long requisicionId) {
        return repository.findAllByRequisicionIdOrderByFechaAsc(requisicionId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    /** Atiende total o parcialmente una requisicion y genera el movimiento de salida. */
    @Transactional
    @Auditado(accion = "REGISTRAR_DESPACHO", entidad = "despacho")
    public DespachoResponse atender(DespachoRequest request) {
        Requisicion requisicion = requisicionService.buscarEntidad(request.requisicionId());
        if (!requisicion.getEstado().admiteAtencion()) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La requisicion %s esta en estado %s y no admite atencion"
                            .formatted(requisicion.getNumero(), requisicion.getEstado()));
        }
        Almacen almacen = almacenService.buscarOperable(requisicion.getAlmacenAtencion().getId());

        // Se resuelven los lotes aqui (FEFO si no se indico), de modo que cada linea
        // de salida queda explicita y se puede enlazar con la linea de requisicion.
        List<PreparacionLinea> preparacion = new ArrayList<>();
        for (DespachoRequest.LineaDespachoRequest linea : request.lineas()) {
            RequisicionLinea requisicionLinea = requisicion.getLineas().stream()
                    .filter(l -> l.getId().equals(linea.requisicionLineaId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("linea de requisicion",
                            linea.requisicionLineaId()));
            BigDecimal cantidad = CantidadUtil.cantidad(linea.cantidad());
            requisicionLinea.registrarAtencion(cantidad);

            // Se resuelve lote Y ubicacion concretos: cada linea preparada
            // corresponde a una celda de stock, de modo que el picking indica
            // exactamente de donde se toma el material.
            if (linea.loteId() == null) {
                asignacionLoteService.asignar(requisicionLinea.getProducto().getId(),
                                almacen.getId(), cantidad)
                        .forEach(asignacion -> preparacion.add(new PreparacionLinea(requisicionLinea,
                                asignacion.stock().getLote(), asignacion.stock().getUbicacion(),
                                CantidadUtil.cantidad(asignacion.cantidad()), true, null)));
            } else {
                Lote lote = loteService.buscarEntidad(linea.loteId());
                // El estado sanitario se valida antes que FEFO: un lote retenido no
                // es un problema de politica de consumo, es mercaderia no despachable.
                if (!lote.esDespachable()) {
                    throw new NegocioException(CodigoError.LOTE_NO_DISPONIBLE,
                            "El lote %s esta en estado %s y no puede despacharse"
                                    .formatted(lote.getCodigoLote(), lote.getEstadoLote()))
                            .con("loteId", lote.getId())
                            .con("estado", lote.getEstadoLote().name());
                }
                boolean esFefo = asignacionLoteService.esLoteFefo(
                        requisicionLinea.getProducto().getId(), almacen.getId(), lote.getId());
                if (!esFefo && (linea.motivoNoFefo() == null || linea.motivoNoFefo().isBlank())) {
                    throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                            "El lote elegido no es el proximo a vencer: indique el motivo "
                                    + "de la desviacion FEFO");
                }
                asignacionLoteService.asignarDeLote(requisicionLinea.getProducto().getId(),
                                almacen.getId(), lote.getId(), cantidad)
                        .forEach(asignacion -> preparacion.add(new PreparacionLinea(requisicionLinea,
                                asignacion.stock().getLote(), asignacion.stock().getUbicacion(),
                                CantidadUtil.cantidad(asignacion.cantidad()), esFefo,
                                esFefo ? null : linea.motivoNoFefo())));
            }
        }

        List<LineaSalidaRequest> lineasSalida = preparacion.stream()
                .map(p -> new LineaSalidaRequest(p.requisicionLinea().getProducto().getId(),
                        p.lote() == null ? null : p.lote().getId(),
                        p.ubicacion() == null ? null : p.ubicacion().getId(),
                        p.cantidad(), p.motivoNoFefo()))
                .toList();

        Movimiento movimiento = movimientoService.registrarSalidaInterna(new SalidaRequest(
                almacen.getId(), TipoMovimiento.SALIDA_DESPACHO,
                request.fecha() == null ? FechaUtil.hoy() : request.fecha(),
                requisicion.getCentroCosto(), requisicion.getArea(), null, "REQUISICION",
                requisicion.getNumero(), request.observacion(), REFERENCIA, requisicion.getId(),
                lineasSalida), null);

        Map<Long, BigDecimal> costos = new LinkedHashMap<>();
        movimiento.getLineas().forEach(l -> costos.putIfAbsent(
                l.getLote() == null ? -1L : l.getLote().getId(), l.getCostoUnitario()));

        Despacho despacho = Despacho.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.DES))
                .requisicion(requisicion)
                .almacen(almacen)
                .fecha(request.fecha() == null ? FechaUtil.hoy() : request.fecha())
                .movimientoId(movimiento.getId())
                .entregadoA(request.entregadoA())
                .despachadoPor(ContextoSeguridad.username())
                .observacion(request.observacion())
                .build();

        preparacion.forEach(p -> despacho.agregarLinea(DespachoLinea.builder()
                .requisicionLinea(p.requisicionLinea())
                .producto(p.requisicionLinea().getProducto())
                .lote(p.lote())
                .cantidad(p.cantidad())
                .costoUnitario(costos.getOrDefault(p.lote() == null ? -1L : p.lote().getId(),
                        p.requisicionLinea().getProducto().getCostoPromedio()))
                .fefoRespetado(p.fefoRespetado())
                .motivoNoFefo(p.motivoNoFefo())
                .build()));

        Despacho guardado = repository.save(despacho);
        requisicion.actualizarEstadoPorAtencion();
        requisicionService.guardar(requisicion);

        if (guardado.getLineas().stream().anyMatch(l -> !l.isFefoRespetado())) {
            log.warn("Despacho {} con desviacion FEFO en requisicion {}", guardado.getNumero(),
                    requisicion.getNumero());
        }
        return mapper.aResponse(guardado);
    }

    @Transactional
    public void marcarEnAtencion(Long requisicionId) {
        Requisicion requisicion = requisicionService.buscarEntidad(requisicionId);
        if (requisicion.getEstado() == EstadoRequisicion.ENVIADA) {
            requisicion.cambiarEstado(EstadoRequisicion.EN_ATENCION);
            requisicionService.guardar(requisicion);
        }
    }

    /** Linea ya resuelta a lote concreto, lista para generar la salida. */
    private record PreparacionLinea(RequisicionLinea requisicionLinea, Lote lote,
                                    pe.cid.sgai.almacen.domain.Ubicacion ubicacion,
                                    BigDecimal cantidad, boolean fefoRespetado, String motivoNoFefo) {
    }
}
