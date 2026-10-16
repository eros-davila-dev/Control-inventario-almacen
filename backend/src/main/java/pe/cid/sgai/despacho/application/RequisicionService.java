package pe.cid.sgai.despacho.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.despacho.application.dto.RequisicionRequest;
import pe.cid.sgai.despacho.application.dto.RequisicionResponse;
import pe.cid.sgai.despacho.application.mapper.DespachoMapper;
import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.despacho.domain.Prioridad;
import pe.cid.sgai.despacho.domain.Requisicion;
import pe.cid.sgai.despacho.domain.RequisicionLinea;
import pe.cid.sgai.despacho.repository.RequisicionRepository;
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

/** Requisiciones internas de las areas asistenciales (HU-32, HU-35). */
@Service
@RequiredArgsConstructor
public class RequisicionService {

    private final RequisicionRepository repository;
    private final AlmacenService almacenService;
    private final ProductoService productoService;
    private final CorrelativoService correlativoService;
    private final DespachoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RequisicionResponse> buscar(Long almacenAtencionId, Long almacenSolicitanteId,
                                                    EstadoRequisicion estado, boolean soloMias,
                                                    Pageable pageable) {
        Long solicitanteId = soloMias ? ContextoSeguridad.idUsuarioActual() : null;
        return PageResponse.de(repository.buscar(almacenAtencionId, almacenSolicitanteId, estado,
                solicitanteId, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public RequisicionResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public Requisicion buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("requisicion", id));
    }

    @Transactional
    @Auditado(accion = "CREAR_REQUISICION", entidad = "requisicion")
    public RequisicionResponse crear(RequisicionRequest request) {
        Almacen solicitante = almacenService.buscarEntidad(request.almacenSolicitanteId());
        Almacen atencion = almacenService.buscarEntidad(request.almacenAtencionId());
        if (!atencion.esDespachable()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "El almacen %s no puede atender requisiciones (%s)".formatted(atencion.getNombre(),
                            atencion.getTipoAlmacen().descripcion()));
        }

        Requisicion requisicion = Requisicion.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.REQ))
                .almacenSolicitante(solicitante)
                .almacenAtencion(atencion)
                .centroCosto(request.centroCosto().trim().toUpperCase())
                .area(request.area().trim())
                .prioridad(request.prioridad() == null ? Prioridad.NORMAL : request.prioridad())
                .fecha(FechaUtil.hoy())
                .fechaRequerida(request.fechaRequerida() == null
                        ? FechaUtil.hoy() : request.fechaRequerida())
                .estado(EstadoRequisicion.BORRADOR)
                .solicitanteId(ContextoSeguridad.idUsuarioActual())
                .observacion(request.observacion())
                .build();

        request.lineas().forEach(linea -> requisicion.agregarLinea(RequisicionLinea.builder()
                .producto(productoService.buscarEntidad(linea.productoId()))
                .cantidadSolicitada(CantidadUtil.cantidad(linea.cantidadSolicitada()))
                .cantidadAtendida(java.math.BigDecimal.ZERO)
                .observacion(linea.observacion())
                .build()));

        return mapper.aResponse(repository.save(requisicion));
    }

    @Transactional
    @Auditado(accion = "ENVIAR_REQUISICION", entidad = "requisicion")
    public RequisicionResponse enviar(Long id) {
        Requisicion requisicion = buscarEntidad(id);
        requisicion.cambiarEstado(EstadoRequisicion.ENVIADA);
        return mapper.aResponse(repository.save(requisicion));
    }

    @Transactional
    @Auditado(accion = "ANULAR_REQUISICION", entidad = "requisicion")
    public RequisicionResponse anular(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException(CodigoError.VALIDACION, "Indique el motivo de la anulacion");
        }
        Requisicion requisicion = buscarEntidad(id);
        requisicion.cambiarEstado(EstadoRequisicion.ANULADA);
        requisicion.setObservacion((requisicion.getObservacion() == null ? ""
                : requisicion.getObservacion() + " | ") + "Anulada: " + motivo);
        return mapper.aResponse(repository.save(requisicion));
    }

    @Transactional
    public Requisicion guardar(Requisicion requisicion) {
        return repository.save(requisicion);
    }
}
