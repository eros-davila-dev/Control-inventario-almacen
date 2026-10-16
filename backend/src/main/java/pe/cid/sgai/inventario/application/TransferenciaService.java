package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.repository.AlmacenRepository;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.inventario.application.dto.IngresoRequest;
import pe.cid.sgai.inventario.application.dto.LineaIngresoRequest;
import pe.cid.sgai.inventario.application.dto.LineaSalidaRequest;
import pe.cid.sgai.inventario.application.dto.RecepcionTransferenciaRequest;
import pe.cid.sgai.inventario.application.dto.SalidaRequest;
import pe.cid.sgai.inventario.application.dto.TransferenciaRequest;
import pe.cid.sgai.inventario.application.dto.TransferenciaResponse;
import pe.cid.sgai.inventario.application.mapper.InventarioMapper;
import pe.cid.sgai.inventario.domain.EstadoTransferencia;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.MotivoAjuste;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.TipoMovimiento;
import pe.cid.sgai.inventario.domain.Transferencia;
import pe.cid.sgai.inventario.domain.TransferenciaLinea;
import pe.cid.sgai.inventario.repository.TransferenciaRepository;
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
 * Transferencias entre almacenes de la red con confirmacion en destino (HU-22).
 * Mientras la transferencia esta ENVIADA el stock ya salio del origen y aun no se
 * acredita en destino: eso hace visible el inventario en transito y explica las
 * diferencias que antes se perdian entre sedes (problema P6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private static final String REFERENCIA = "TRANSFERENCIA";

    private final TransferenciaRepository repository;
    private final AlmacenRepository almacenRepository;
    private final AlmacenService almacenService;
    private final ProductoService productoService;
    private final LoteService loteService;
    private final MovimientoService movimientoService;
    private final AsignacionLoteService asignacionLoteService;
    private final CorrelativoService correlativoService;
    private final InventarioMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<TransferenciaResponse> buscar(Long almacenId, EstadoTransferencia estado,
                                                      Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, estado, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public TransferenciaResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    /** Envio: descuenta el origen y deja la mercaderia en transito. */
    @Transactional
    @Auditado(accion = "ENVIAR_TRANSFERENCIA", entidad = "transferencia")
    public TransferenciaResponse enviar(TransferenciaRequest request) {
        if (request.almacenOrigenId().equals(request.almacenDestinoId())) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "El almacen de origen y destino deben ser distintos");
        }
        Almacen origen = almacenService.buscarOperable(request.almacenOrigenId());
        Almacen destino = almacenRepository.findById(request.almacenDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("almacen destino",
                        request.almacenDestinoId()));
        if (!destino.isActivo()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO, "El almacen destino esta inactivo");
        }

        Transferencia transferencia = Transferencia.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.TRA))
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .fechaEnvio(request.fechaEnvio() == null ? FechaUtil.hoy() : request.fechaEnvio())
                .estado(EstadoTransferencia.ENVIADA)
                .transportista(request.transportista())
                .guiaRemision(request.guiaRemision())
                .observacion(request.observacion())
                .enviadoPor(ContextoSeguridad.username())
                .build();

        // Cada linea se resuelve a lotes concretos (FEFO si no se indico lote),
        // de modo que la transferencia viaja con trazabilidad por lote.
        for (TransferenciaRequest.LineaTransferenciaRequest linea : request.lineas()) {
            Producto producto = productoService.buscarEntidad(linea.productoId());
            if (producto.isControlaLote() && linea.loteId() == null) {
                asignacionLoteService.asignar(producto.getId(), origen.getId(), linea.cantidad())
                        .forEach(asignacion -> transferencia.agregarLinea(TransferenciaLinea.builder()
                                .producto(producto)
                                .lote(asignacion.stock().getLote())
                                .cantidadEnviada(CantidadUtil.cantidad(asignacion.cantidad()))
                                .costoUnitario(costoDe(producto, asignacion.stock().getLote()))
                                .build()));
            } else {
                Lote lote = linea.loteId() == null ? null : loteService.buscarEntidad(linea.loteId());
                transferencia.agregarLinea(TransferenciaLinea.builder()
                        .producto(producto)
                        .lote(lote)
                        .cantidadEnviada(CantidadUtil.cantidad(linea.cantidad()))
                        .costoUnitario(costoDe(producto, lote))
                        .build());
            }
        }

        Transferencia guardada = repository.save(transferencia);

        List<LineaSalidaRequest> lineasSalida = guardada.getLineas().stream()
                .map(l -> new LineaSalidaRequest(l.getProducto().getId(),
                        l.getLote() == null ? null : l.getLote().getId(), null,
                        l.getCantidadEnviada(), "Transferencia " + guardada.getNumero()))
                .toList();
        Movimiento salida = movimientoService.registrarSalidaInterna(new SalidaRequest(
                origen.getId(), TipoMovimiento.SALIDA_TRANSFERENCIA, guardada.getFechaEnvio(),
                null, destino.getNombre(), null, "TRANSFERENCIA", guardada.getNumero(),
                request.observacion(), REFERENCIA, guardada.getId(), lineasSalida), null);

        guardada.setMovimientoSalidaId(salida.getId());
        log.info("Transferencia {} enviada de {} a {}", guardada.getNumero(), origen.getCodigo(),
                destino.getCodigo());
        return mapper.aResponse(repository.save(guardada));
    }

    /**
     * Recepcion en destino. Acredita lo enviado y, si lo recibido es menor, genera
     * una merma en transito con motivo (requisito de HU-22): la diferencia nunca
     * queda sin explicacion.
     */
    @Transactional
    @Auditado(accion = "RECIBIR_TRANSFERENCIA", entidad = "transferencia")
    public TransferenciaResponse recibir(Long id, RecepcionTransferenciaRequest request) {
        Transferencia transferencia = buscarEntidad(id);
        if (transferencia.getEstado() != EstadoTransferencia.ENVIADA) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La transferencia %s ya fue procesada (%s)".formatted(transferencia.getNumero(),
                            transferencia.getEstado()));
        }
        Almacen destino = almacenService.buscarOperable(transferencia.getAlmacenDestino().getId());

        for (RecepcionTransferenciaRequest.LineaRecepcion recibida : request.lineas()) {
            TransferenciaLinea linea = transferencia.getLineas().stream()
                    .filter(l -> l.getId().equals(recibida.transferenciaLineaId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("linea de transferencia",
                            recibida.transferenciaLineaId()));
            if (recibida.cantidadRecibida().compareTo(linea.getCantidadEnviada()) > 0) {
                throw new NegocioException(CodigoError.CANTIDAD_EXCEDE_PENDIENTE,
                        "No se puede recibir mas de lo enviado para %s"
                                .formatted(linea.getProducto().getNombre()));
            }
            linea.setCantidadRecibida(CantidadUtil.cantidad(recibida.cantidadRecibida()));
            linea.setObservacion(recibida.observacion());
        }

        List<LineaIngresoRequest> lineasIngreso = transferencia.getLineas().stream()
                .map(l -> new LineaIngresoRequest(l.getProducto().getId(), null, null, null,
                        l.getLote() == null ? null : l.getLote().getId(), request.ubicacionDestinoId(),
                        l.getCantidadEnviada(), l.getCostoUnitario()))
                .toList();
        Movimiento ingreso = movimientoService.registrarIngresoInterno(new IngresoRequest(
                destino.getId(), TipoMovimiento.INGRESO_TRANSFERENCIA, FechaUtil.hoy(),
                "TRANSFERENCIA", transferencia.getNumero(), null, null,
                "Recepcion de " + transferencia.getNumero(), REFERENCIA, transferencia.getId(),
                lineasIngreso), null);

        List<LineaSalidaRequest> mermas = new ArrayList<>();
        transferencia.getLineas().stream()
                .filter(TransferenciaLinea::tieneDiferencia)
                .forEach(l -> mermas.add(new LineaSalidaRequest(l.getProducto().getId(),
                        l.getLote() == null ? null : l.getLote().getId(),
                        request.ubicacionDestinoId(), l.diferencia(),
                        "Merma en transito " + transferencia.getNumero())));

        if (!mermas.isEmpty()) {
            if (request.observacion() == null || request.observacion().isBlank()) {
                throw new NegocioException(CodigoError.VALIDACION,
                        "Existen diferencias entre lo enviado y lo recibido: indique la observacion");
            }
            movimientoService.registrarSalidaInterna(new SalidaRequest(destino.getId(),
                    TipoMovimiento.SALIDA_MERMA, FechaUtil.hoy(), null, "TRANSITO",
                    MotivoAjuste.MERMA_TRANSITO, "TRANSFERENCIA", transferencia.getNumero(),
                    "Diferencia en recepcion de %s: %s".formatted(transferencia.getNumero(),
                            request.observacion()), REFERENCIA, transferencia.getId(), mermas), null);
        }

        transferencia.setEstado(transferencia.tieneDiferencias()
                ? EstadoTransferencia.RECIBIDA_PARCIAL : EstadoTransferencia.RECIBIDA);
        transferencia.setFechaRecepcion(FechaUtil.hoy());
        transferencia.setRecibidoPor(ContextoSeguridad.username());
        transferencia.setMovimientoIngresoId(ingreso.getId());
        if (request.observacion() != null && !request.observacion().isBlank()) {
            transferencia.setObservacion((transferencia.getObservacion() == null ? ""
                    : transferencia.getObservacion() + " | ") + request.observacion());
        }
        return mapper.aResponse(repository.save(transferencia));
    }

    @Transactional(readOnly = true)
    public Transferencia buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("transferencia", id));
    }

    private BigDecimal costoDe(Producto producto, Lote lote) {
        if (lote != null && CantidadUtil.esPositiva(lote.getCostoUnitario())) {
            return CantidadUtil.costo(lote.getCostoUnitario());
        }
        return CantidadUtil.costo(producto.getCostoPromedio());
    }
}
