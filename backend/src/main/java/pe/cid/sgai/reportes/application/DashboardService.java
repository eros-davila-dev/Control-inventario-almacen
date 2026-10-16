package pe.cid.sgai.reportes.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.alertas.domain.EstadoAlerta;
import pe.cid.sgai.alertas.domain.SeveridadAlerta;
import pe.cid.sgai.alertas.repository.AlertaRepository;
import pe.cid.sgai.compras.domain.EstadoOrdenCompra;
import pe.cid.sgai.compras.repository.OrdenCompraRepository;
import pe.cid.sgai.conteo.domain.EstadoToma;
import pe.cid.sgai.conteo.repository.TomaInventarioRepository;
import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.despacho.repository.DespachoRepository;
import pe.cid.sgai.despacho.repository.RequisicionRepository;
import pe.cid.sgai.inventario.application.StockService;
import pe.cid.sgai.inventario.application.dto.LoteVencimientoDto;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.domain.EstadoMovimiento;
import pe.cid.sgai.inventario.domain.EstadoTransferencia;
import pe.cid.sgai.inventario.repository.MovimientoRepository;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.inventario.repository.TransferenciaRepository;
import pe.cid.sgai.reportes.application.dto.DashboardResponse;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores del tablero de control (HU-43). Responde las preguntas que hoy exigen
 * revisar varias hojas de calculo: cuanto vale el inventario, que falta, que esta por
 * vencer y que esta pendiente de atender.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final MovimientoRepository movimientoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final RequisicionRepository requisicionRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final TomaInventarioRepository tomaInventarioRepository;
    private final DespachoRepository despachoRepository;
    private final AlertaRepository alertaRepository;
    private final ReporteService reporteService;

    @Transactional(readOnly = true)
    public DashboardResponse indicadores(Long almacenId) {
        List<StockConsolidadoResponse> saldos = stockService.consolidado(almacenId, null, null, false);
        long bajoMinimo = saldos.stream()
                .filter(s -> StockService.BAJO_MINIMO.equals(s.estadoReposicion())
                        || StockService.EN_REORDEN.equals(s.estadoReposicion()))
                .count();
        long agotados = saldos.stream()
                .filter(s -> StockService.SIN_STOCK.equals(s.estadoReposicion()))
                .count();

        List<LoteVencimientoDto> por90 = reporteService.vencimientos(almacenId, 90);
        LocalDate hoy = FechaUtil.hoy();
        long vencidos = por90.stream().filter(l -> l.fechaVencimiento().isBefore(hoy)).count();
        long en30 = por90.stream().filter(l -> dentroDe(l, hoy, -1, 30)).count();
        long en60 = por90.stream().filter(l -> dentroDe(l, hoy, 30, 60)).count();
        long en90 = por90.stream().filter(l -> dentroDe(l, hoy, 60, 90)).count();
        BigDecimal valorExpuesto = CantidadUtil.importe(por90.stream()
                .map(LoteVencimientoDto::valorExpuesto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return new DashboardResponse(
                CantidadUtil.importe(stockRepository.valorInventario(almacenId)),
                almacenId == null ? saldos.size() : stockRepository.contarItemsConSaldo(almacenId),
                bajoMinimo,
                agotados,
                en30, en60, en90, vencidos, valorExpuesto,
                requisicionRepository.countByEstadoIn(List.of(EstadoRequisicion.ENVIADA,
                        EstadoRequisicion.EN_ATENCION, EstadoRequisicion.ATENDIDA_PARCIAL)),
                ordenCompraRepository.countByEstadoIn(List.of(EstadoOrdenCompra.APROBADA,
                        EstadoOrdenCompra.RECIBIDA_PARCIAL)),
                transferenciaRepository.countByEstado(EstadoTransferencia.ENVIADA),
                tomaInventarioRepository.countByEstadoIn(List.of(EstadoToma.EN_PROCESO,
                        EstadoToma.CONTADA)),
                movimientoRepository.contarPorFecha(hoy, EstadoMovimiento.REGISTRADO, almacenId),
                alertaRepository.buscar(null, SeveridadAlerta.CRITICA, EstadoAlerta.ACTIVA, almacenId,
                        org.springframework.data.domain.Pageable.ofSize(1)).getTotalElements(),
                despachoRepository.contarDesviacionesFefo(hoy.minusDays(30)),
                stockRepository.valorizacionPorCategoria(almacenId),
                reporteService.consumo(almacenId, 12),
                reporteService.rotacion(almacenId, 90).stream().limit(10).toList());
    }

    /** Lotes cuyo vencimiento cae en el tramo (desde, hasta] dias contados desde hoy. */
    private boolean dentroDe(LoteVencimientoDto lote, LocalDate hoy, int desdeExclusivo, int hastaInclusivo) {
        long dias = lote.fechaVencimiento().toEpochDay() - hoy.toEpochDay();
        return dias > desdeExclusivo && dias <= hastaInclusivo;
    }
}
