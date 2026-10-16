package pe.cid.sgai.compras.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.catalogo.repository.ProveedorRepository;
import pe.cid.sgai.compras.application.dto.OrdenCompraRequest;
import pe.cid.sgai.compras.application.dto.OrdenCompraResponse;
import pe.cid.sgai.compras.application.mapper.ComprasMapper;
import pe.cid.sgai.compras.domain.EstadoOrdenCompra;
import pe.cid.sgai.compras.domain.OrdenCompra;
import pe.cid.sgai.compras.domain.OrdenCompraLinea;
import pe.cid.sgai.compras.domain.RequerimientoCompra;
import pe.cid.sgai.compras.repository.OrdenCompraRepository;
import pe.cid.sgai.seguridad.infrastructure.ContextoSeguridad;
import pe.cid.sgai.shared.audit.Auditado;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.CorrelativoService;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.util.TipoCorrelativo;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.Instant;

/** Ordenes de compra con totales calculados en servidor y aprobacion segregada (HU-28, HU-29). */
@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository repository;
    private final ProveedorRepository proveedorRepository;
    private final RequerimientoCompraService requerimientoService;
    private final ProductoService productoService;
    private final AlmacenService almacenService;
    private final CorrelativoService correlativoService;
    private final ComprasMapper mapper;
    private final PropiedadesSgai propiedades;

    @Transactional(readOnly = true)
    public PageResponse<OrdenCompraResponse> buscar(Long almacenId, Long proveedorId,
                                                    EstadoOrdenCompra estado, String texto,
                                                    Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, proveedorId, estado,
                texto == null || texto.isBlank() ? null : texto.trim(), pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse obtener(Long id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public OrdenCompra buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("orden de compra", id));
    }

    @Transactional
    @Auditado(accion = "CREAR_ORDEN_COMPRA", entidad = "orden_compra")
    public OrdenCompraResponse crear(OrdenCompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("proveedor", request.proveedorId()));
        if (!proveedor.isActivo()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO, "El proveedor esta inactivo");
        }
        RequerimientoCompra requerimiento = request.requerimientoId() == null ? null
                : requerimientoService.buscarEntidad(request.requerimientoId());

        OrdenCompra orden = OrdenCompra.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.OC))
                .proveedor(proveedor)
                .requerimiento(requerimiento)
                .almacenDestino(almacenService.buscarEntidad(request.almacenDestinoId()))
                .fechaEmision(request.fechaEmision() == null ? FechaUtil.hoy() : request.fechaEmision())
                .fechaEsperada(request.fechaEsperada() == null
                        ? FechaUtil.hoy().plusDays(proveedor.getLeadTimeDias()) : request.fechaEsperada())
                .moneda(request.moneda() == null ? propiedades.moneda() : request.moneda())
                .condicionPago(request.condicionPago() == null
                        ? proveedor.getCondicionPago() : request.condicionPago())
                .estado(EstadoOrdenCompra.BORRADOR)
                .observacion(request.observacion())
                .build();

        request.lineas().forEach(linea -> {
            OrdenCompraLinea entidad = OrdenCompraLinea.builder()
                    .producto(productoService.buscarEntidad(linea.productoId()))
                    .cantidad(CantidadUtil.cantidad(linea.cantidad()))
                    .precioUnitario(CantidadUtil.costo(linea.precioUnitario()))
                    .observacion(linea.observacion())
                    .build();
            entidad.calcularImporte();
            orden.agregarLinea(entidad);
        });
        orden.calcularTotales(propiedades.igv());

        OrdenCompra guardada = repository.save(orden);
        if (requerimiento != null) {
            requerimientoService.marcarAtendido(requerimiento);
        }
        return mapper.aResponse(guardada);
    }

    @Transactional
    @Auditado(accion = "APROBAR_ORDEN_COMPRA", entidad = "orden_compra")
    public OrdenCompraResponse aprobar(Long id) {
        OrdenCompra orden = buscarEntidad(id);
        var usuario = ContextoSeguridad.requerido();
        if (!usuario.esAdministrador() && usuario.username().equalsIgnoreCase(orden.getCreatedBy())) {
            throw new NegocioException(CodigoError.SEGREGACION_FUNCIONES,
                    "Quien registra la orden de compra no puede aprobarla");
        }
        orden.cambiarEstado(EstadoOrdenCompra.APROBADA);
        orden.setAprobador(usuario.username());
        orden.setFechaAprobacion(Instant.now());
        return mapper.aResponse(repository.save(orden));
    }

    @Transactional
    @Auditado(accion = "RECHAZAR_ORDEN_COMPRA", entidad = "orden_compra")
    public OrdenCompraResponse rechazar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException(CodigoError.VALIDACION, "Indique el motivo del rechazo");
        }
        OrdenCompra orden = buscarEntidad(id);
        orden.cambiarEstado(EstadoOrdenCompra.RECHAZADA);
        orden.setAprobador(ContextoSeguridad.username());
        orden.setFechaAprobacion(Instant.now());
        orden.setObservacion((orden.getObservacion() == null ? "" : orden.getObservacion() + " | ")
                + "Rechazada: " + motivo);
        return mapper.aResponse(repository.save(orden));
    }

    @Transactional
    @Auditado(accion = "ANULAR_ORDEN_COMPRA", entidad = "orden_compra")
    public OrdenCompraResponse anular(Long id, String motivo) {
        OrdenCompra orden = buscarEntidad(id);
        orden.cambiarEstado(EstadoOrdenCompra.ANULADA);
        orden.setObservacion((orden.getObservacion() == null ? "" : orden.getObservacion() + " | ")
                + "Anulada: " + motivo);
        return mapper.aResponse(repository.save(orden));
    }

    @Transactional
    public OrdenCompra guardar(OrdenCompra orden) {
        return repository.save(orden);
    }
}
