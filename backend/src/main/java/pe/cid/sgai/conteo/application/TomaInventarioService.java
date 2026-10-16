package pe.cid.sgai.conteo.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.conteo.application.dto.ConteoRequest;
import pe.cid.sgai.conteo.application.dto.TomaInventarioRequest;
import pe.cid.sgai.conteo.application.dto.TomaInventarioResponse;
import pe.cid.sgai.conteo.domain.AlcanceToma;
import pe.cid.sgai.conteo.domain.EstadoToma;
import pe.cid.sgai.conteo.domain.TomaInventario;
import pe.cid.sgai.conteo.domain.TomaInventarioLinea;
import pe.cid.sgai.conteo.repository.TomaInventarioRepository;
import pe.cid.sgai.inventario.application.MovimientoService;
import pe.cid.sgai.inventario.application.dto.AjusteRequest;
import pe.cid.sgai.inventario.domain.MotivoAjuste;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.repository.StockRepository;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Toma de inventario fisico y ajuste de diferencias (HU-36 a HU-38).
 * Al abrir la toma congela el saldo teorico; durante el conteo la pantalla no lo
 * muestra (conteo ciego) y al aprobar genera los ajustes con motivo CONTEO_FISICO
 * y calcula el ERI del almacen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TomaInventarioService {

    private final TomaInventarioRepository repository;
    private final StockRepository stockRepository;
    private final AlmacenService almacenService;
    private final MovimientoService movimientoService;
    private final CorrelativoService correlativoService;

    @Transactional(readOnly = true)
    public PageResponse<TomaInventarioResponse> buscar(Long almacenId, EstadoToma estado,
                                                       Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, estado, pageable),
                toma -> aResponse(toma, false));
    }

    @Transactional(readOnly = true)
    public TomaInventarioResponse obtener(Long id) {
        TomaInventario toma = buscarEntidad(id);
        // Conteo ciego: mientras se cuenta no se expone la cantidad teorica.
        boolean ocultarTeorico = toma.getEstado() == EstadoToma.EN_PROCESO;
        return aResponse(toma, ocultarTeorico);
    }

    @Transactional(readOnly = true)
    public TomaInventario buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("toma de inventario", id));
    }

    @Transactional
    @Auditado(accion = "PROGRAMAR_TOMA", entidad = "toma_inventario")
    public TomaInventarioResponse programar(TomaInventarioRequest request) {
        Almacen almacen = almacenService.buscarOperable(request.almacenId());
        if (request.alcance() == AlcanceToma.POR_CATEGORIA && request.categoriaId() == null) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "Seleccione la categoria a contar");
        }
        if (request.alcance() == AlcanceToma.CICLICO_ABC && request.clasificacionAbc() == null) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "Seleccione la clasificacion ABC a contar");
        }
        TomaInventario toma = TomaInventario.builder()
                .numero(correlativoService.siguiente(TipoCorrelativo.TOM))
                .almacen(almacen)
                .alcance(request.alcance())
                .categoriaId(request.categoriaId())
                .clasificacionAbc(request.clasificacionAbc())
                .fechaProgramada(request.fechaProgramada() == null
                        ? FechaUtil.hoy() : request.fechaProgramada())
                .estado(EstadoToma.PROGRAMADA)
                .responsable(ContextoSeguridad.username())
                .observacion(request.observacion())
                .build();
        return aResponse(repository.save(toma), false);
    }

    /** Abre la toma y congela el saldo teorico de cada item del alcance. */
    @Transactional
    @Auditado(accion = "ABRIR_TOMA", entidad = "toma_inventario")
    public TomaInventarioResponse abrir(Long id) {
        TomaInventario toma = buscarEntidad(id);
        almacenService.buscarOperable(toma.getAlmacen().getId());
        toma.cambiarEstado(EstadoToma.EN_PROCESO);
        toma.setFechaInicio(FechaUtil.hoy());
        toma.getLineas().clear();

        List<Stock> saldos = stockRepository.buscar(toma.getAlmacen().getId(), null,
                        toma.getCategoriaId(), null, true, Pageable.unpaged()).getContent().stream()
                .filter(s -> toma.getClasificacionAbc() == null
                        || toma.getClasificacionAbc() == s.getProducto().getClasificacionAbc())
                .toList();

        if (saldos.isEmpty()) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "No hay items con saldo en el alcance seleccionado");
        }
        saldos.forEach(stock -> toma.agregarLinea(TomaInventarioLinea.builder()
                .producto(stock.getProducto())
                .lote(stock.getLote())
                .ubicacion(stock.getUbicacion())
                .cantidadTeorica(CantidadUtil.cantidad(stock.getCantidad()))
                .build()));

        log.info("Toma {} abierta con {} items", toma.getNumero(), toma.getLineas().size());
        return aResponse(repository.save(toma), true);
    }

    /** Registra el conteo (primero o segundo) de las lineas indicadas. */
    @Transactional
    @Auditado(accion = "REGISTRAR_CONTEO", entidad = "toma_inventario")
    public TomaInventarioResponse registrarConteo(Long id, ConteoRequest request) {
        TomaInventario toma = buscarEntidad(id);
        if (toma.getEstado() != EstadoToma.EN_PROCESO && toma.getEstado() != EstadoToma.CONTADA) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La toma debe estar en proceso para registrar conteos");
        }
        almacenService.buscarOperable(toma.getAlmacen().getId());

        for (ConteoRequest.LineaConteo conteo : request.lineas()) {
            TomaInventarioLinea linea = toma.getLineas().stream()
                    .filter(l -> l.getId().equals(conteo.lineaId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("linea de toma",
                            conteo.lineaId()));
            if (Boolean.TRUE.equals(conteo.esSegundoConteo())) {
                linea.setSegundoConteo(CantidadUtil.cantidad(conteo.cantidadContada()));
            } else {
                linea.setCantidadContada(CantidadUtil.cantidad(conteo.cantidadContada()));
            }
            linea.setContadoPor(ContextoSeguridad.username());
            linea.setFechaConteo(Instant.now());
            if (conteo.observacion() != null) {
                linea.setObservacion(conteo.observacion());
            }
        }
        return aResponse(repository.save(toma), true);
    }

    /** Cierra el conteo y habilita la revision de diferencias. */
    @Transactional
    @Auditado(accion = "CERRAR_CONTEO", entidad = "toma_inventario")
    public TomaInventarioResponse marcarContada(Long id) {
        TomaInventario toma = buscarEntidad(id);
        long sinContar = toma.getLineas().stream().filter(l -> !l.fueContada()).count();
        if (sinContar > 0) {
            throw new NegocioException(CodigoError.REGLA_NEGOCIO,
                    "Quedan %d item(s) sin contar".formatted(sinContar));
        }
        toma.cambiarEstado(EstadoToma.CONTADA);
        toma.setEri(toma.calcularEri());
        return aResponse(repository.save(toma), false);
    }

    /**
     * Aprueba las diferencias y genera los ajustes de inventario. Quien ejecuto el
     * conteo no puede aprobarlo (segregacion de funciones).
     */
    @Transactional
    @Auditado(accion = "APROBAR_TOMA", entidad = "toma_inventario")
    public TomaInventarioResponse aprobar(Long id) {
        TomaInventario toma = buscarEntidad(id);
        if (toma.getEstado() != EstadoToma.CONTADA) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "Solo se aprueba una toma en estado CONTADA");
        }
        var usuario = ContextoSeguridad.requerido();
        if (!usuario.esAdministrador() && usuario.username().equalsIgnoreCase(toma.getResponsable())) {
            throw new NegocioException(CodigoError.SEGREGACION_FUNCIONES,
                    "Quien ejecuto el conteo no puede aprobar sus ajustes");
        }

        List<AjusteRequest.LineaAjusteRequest> sobrantes = new ArrayList<>();
        List<AjusteRequest.LineaAjusteRequest> faltantes = new ArrayList<>();
        toma.getLineas().stream().filter(TomaInventarioLinea::tieneDiferencia).forEach(linea -> {
            BigDecimal diferencia = linea.diferencia();
            AjusteRequest.LineaAjusteRequest ajuste = new AjusteRequest.LineaAjusteRequest(
                    linea.getProducto().getId(),
                    linea.getLote() == null ? null : linea.getLote().getId(),
                    linea.getUbicacion() == null ? null : linea.getUbicacion().getId(),
                    diferencia.abs(), linea.getProducto().getCostoPromedio());
            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                sobrantes.add(ajuste);
            } else {
                faltantes.add(ajuste);
            }
        });

        String detalle = "Ajuste por toma de inventario " + toma.getNumero();
        if (!sobrantes.isEmpty()) {
            movimientoService.registrarAjusteInterno(new AjusteRequest(toma.getAlmacen().getId(),
                    MotivoAjuste.CONTEO_FISICO, true, detalle + " (sobrantes)", sobrantes),
                    "TOMA_INVENTARIO");
        }
        if (!faltantes.isEmpty()) {
            movimientoService.registrarAjusteInterno(new AjusteRequest(toma.getAlmacen().getId(),
                    MotivoAjuste.CONTEO_FISICO, false, detalle + " (faltantes)", faltantes),
                    "TOMA_INVENTARIO");
        }

        toma.cambiarEstado(EstadoToma.APROBADA);
        toma.setAprobador(usuario.username());
        toma.setFechaCierre(FechaUtil.hoy());
        toma.setEri(toma.calcularEri());
        log.info("Toma {} aprobada con ERI {}% ({} sobrantes, {} faltantes)", toma.getNumero(),
                toma.getEri(), sobrantes.size(), faltantes.size());
        return aResponse(repository.save(toma), false);
    }

    @Transactional
    @Auditado(accion = "ANULAR_TOMA", entidad = "toma_inventario")
    public TomaInventarioResponse anular(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException(CodigoError.VALIDACION, "Indique el motivo de la anulacion");
        }
        TomaInventario toma = buscarEntidad(id);
        toma.cambiarEstado(EstadoToma.ANULADA);
        toma.setObservacion((toma.getObservacion() == null ? "" : toma.getObservacion() + " | ")
                + "Anulada: " + motivo);
        return aResponse(repository.save(toma), false);
    }

    private TomaInventarioResponse aResponse(TomaInventario toma, boolean ocultarTeorico) {
        List<TomaInventarioResponse.LineaTomaResponse> lineas = toma.getLineas().stream()
                .map(l -> new TomaInventarioResponse.LineaTomaResponse(
                        l.getId(), l.getProducto().getId(), l.getProducto().getCodigo(),
                        l.getProducto().getNombre(), l.getProducto().getUnidadMedida().getCodigo(),
                        l.getLote() == null ? null : l.getLote().getId(),
                        l.getLote() == null ? null : l.getLote().getCodigoLote(),
                        l.getLote() == null ? null : l.getLote().getFechaVencimiento(),
                        l.getUbicacion() == null ? null : l.getUbicacion().getId(),
                        l.getUbicacion() == null ? null : l.getUbicacion().getCodigo(),
                        ocultarTeorico ? null : l.getCantidadTeorica(),
                        l.getCantidadContada(), l.getSegundoConteo(),
                        ocultarTeorico ? null : l.diferencia(),
                        ocultarTeorico ? null : l.valorDiferencia(),
                        l.getContadoPor(), l.getObservacion()))
                .toList();

        int contados = (int) toma.getLineas().stream().filter(TomaInventarioLinea::fueContada).count();
        int conDiferencia = (int) toma.getLineas().stream()
                .filter(TomaInventarioLinea::tieneDiferencia).count();
        BigDecimal valorDiferencias = CantidadUtil.importe(toma.getLineas().stream()
                .map(TomaInventarioLinea::valorDiferencia)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return new TomaInventarioResponse(toma.getId(), toma.getNumero(), toma.getAlmacen().getId(),
                toma.getAlmacen().getNombre(), toma.getAlcance(), toma.getAlcance().descripcion(),
                toma.getCategoriaId(), toma.getClasificacionAbc(), toma.getFechaProgramada(),
                toma.getFechaInicio(), toma.getFechaCierre(), toma.getEstado(),
                toma.getEstado().descripcion(), toma.getResponsable(), toma.getAprobador(),
                toma.getEri(), toma.getObservacion(), toma.getLineas().size(), contados,
                ocultarTeorico ? 0 : conDiferencia,
                ocultarTeorico ? BigDecimal.ZERO : valorDiferencias, lineas);
    }
}
