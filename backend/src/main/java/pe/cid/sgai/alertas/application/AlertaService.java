package pe.cid.sgai.alertas.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.alertas.application.dto.AlertaResponse;
import pe.cid.sgai.alertas.application.dto.ResumenAlertasResponse;
import pe.cid.sgai.alertas.domain.Alerta;
import pe.cid.sgai.alertas.domain.EstadoAlerta;
import pe.cid.sgai.alertas.domain.SeveridadAlerta;
import pe.cid.sgai.alertas.domain.TipoAlerta;
import pe.cid.sgai.alertas.repository.AlertaRepository;
import pe.cid.sgai.inventario.application.StockService;
import pe.cid.sgai.inventario.application.dto.LoteVencimientoDto;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Generacion y ciclo de vida de las alertas de reposicion y vencimiento
 * (HU-39 a HU-42). Convierte los parametros del maestro de productos en avisos
 * accionables: es el mecanismo que ataca los quiebres (P3) y las mermas (P2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository repository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final PropiedadesSgai propiedades;

    @Transactional(readOnly = true)
    public PageResponse<AlertaResponse> buscar(TipoAlerta tipo, SeveridadAlerta severidad,
                                               EstadoAlerta estado, Long almacenId, Pageable pageable) {
        return PageResponse.de(repository.buscar(tipo, severidad, estado, almacenId, pageable),
                this::aResponse);
    }

    @Transactional(readOnly = true)
    public ResumenAlertasResponse resumen(Long almacenId) {
        long criticas = 0;
        long altas = 0;
        long medias = 0;
        long bajas = 0;
        for (Object[] fila : repository.resumenPorSeveridad(almacenId)) {
            SeveridadAlerta severidad = (SeveridadAlerta) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            switch (severidad) {
                case CRITICA -> criticas = cantidad;
                case ALTA -> altas = cantidad;
                case MEDIA -> medias = cantidad;
                case BAJA -> bajas = cantidad;
            }
        }
        return new ResumenAlertasResponse(criticas + altas + medias + bajas, criticas, altas, medias,
                bajas,
                repository.contarAbiertasPorTipo(TipoAlerta.STOCK_BAJO, almacenId)
                        + repository.contarAbiertasPorTipo(TipoAlerta.STOCK_AGOTADO, almacenId),
                repository.contarAbiertasPorTipo(TipoAlerta.POR_VENCER, almacenId),
                repository.contarAbiertasPorTipo(TipoAlerta.VENCIDO, almacenId));
    }

    @Transactional
    public AlertaResponse marcarLeida(Long id) {
        Alerta alerta = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("alerta", id));
        alerta.marcarLeida();
        return aResponse(repository.save(alerta));
    }

    @Transactional
    public AlertaResponse resolver(Long id) {
        Alerta alerta = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("alerta", id));
        alerta.resolver();
        return aResponse(repository.save(alerta));
    }

    /**
     * Evalua la situacion de reposicion de un producto en un almacen. Se invoca tras
     * cada movimiento (evento de stock) y desde la tarea programada.
     */
    @Transactional
    public void evaluarReposicion(Long productoId, Long almacenId) {
        List<StockConsolidadoResponse> saldos = stockService.consolidado(almacenId, null, null, false)
                .stream()
                .filter(s -> s.productoId().equals(productoId))
                .toList();
        if (saldos.isEmpty()) {
            return;
        }
        StockConsolidadoResponse saldo = saldos.get(0);
        BigDecimal disponible = saldo.disponible();
        BigDecimal minimo = saldo.stockMinimo();

        if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
            abrirOActualizar(TipoAlerta.STOCK_AGOTADO, SeveridadAlerta.CRITICA, saldo, null,
                    "%s sin stock disponible en %s".formatted(saldo.productoNombre(),
                            saldo.almacenNombre()), disponible);
            resolverSiExiste(TipoAlerta.STOCK_BAJO, productoId, almacenId);
            return;
        }
        resolverSiExiste(TipoAlerta.STOCK_AGOTADO, productoId, almacenId);

        if (StockService.requiereReposicion(saldo.estadoReposicion())) {
            SeveridadAlerta severidad;
            if (saldo.critico() && disponible.compareTo(minimo) <= 0) {
                severidad = SeveridadAlerta.CRITICA;
            } else if (StockService.BAJO_MINIMO.equals(saldo.estadoReposicion())) {
                severidad = SeveridadAlerta.ALTA;
            } else {
                severidad = SeveridadAlerta.MEDIA;
            }
            String cobertura = saldo.coberturaDias() == null ? "sin consumo historico"
                    : "cobertura de %s dia(s)".formatted(saldo.coberturaDias().toPlainString());
            abrirOActualizar(TipoAlerta.STOCK_BAJO, severidad, saldo, null,
                    "%s en %s: disponible %s, minimo %s, %s".formatted(saldo.productoNombre(),
                            saldo.almacenNombre(), disponible.toPlainString(),
                            minimo.toPlainString(), cobertura), disponible);
        } else {
            resolverSiExiste(TipoAlerta.STOCK_BAJO, productoId, almacenId);
            if (StockService.SOBRE_STOCK.equals(saldo.estadoReposicion())) {
                abrirOActualizar(TipoAlerta.SOBRE_STOCK, SeveridadAlerta.BAJA, saldo, null,
                        "%s en %s supera el stock maximo (%s)".formatted(saldo.productoNombre(),
                                saldo.almacenNombre(), saldo.stockMaximo().toPlainString()),
                        disponible);
            } else {
                resolverSiExiste(TipoAlerta.SOBRE_STOCK, productoId, almacenId);
            }
        }
    }

    /** Recorre todo el inventario; la ejecuta la tarea programada diaria. */
    @Transactional
    public int evaluarReposicionGlobal() {
        List<StockConsolidadoResponse> enReposicion = stockService.consolidado(null, null, null, true);
        enReposicion.forEach(s -> evaluarReposicion(s.productoId(), s.almacenId()));
        return enReposicion.size();
    }

    /** Alertas de vencimiento por tramos de 90, 60 y 30 dias y vencidos (HU-40). */
    @Transactional
    public int evaluarVencimientos() {
        List<Integer> tramos = propiedades.inventario().tramosVencimientoDias();
        int maximo = tramos.stream().mapToInt(Integer::intValue).max().orElse(90);
        List<LoteVencimientoDto> lotes = stockRepository.vencimientosHasta(
                FechaUtil.hoy().plusDays(maximo), null);

        for (LoteVencimientoDto lote : lotes) {
            long dias = FechaUtil.diasHasta(lote.fechaVencimiento());
            if (dias < 0) {
                abrirOActualizarLote(TipoAlerta.VENCIDO, SeveridadAlerta.CRITICA, lote,
                        "Lote %s de %s vencido el %s con saldo %s en %s".formatted(lote.codigoLote(),
                                lote.productoNombre(), lote.fechaVencimiento(),
                                CantidadUtil.cantidad(lote.cantidad()).toPlainString(),
                                lote.almacenNombre()));
            } else {
                SeveridadAlerta severidad = dias <= 30 ? SeveridadAlerta.ALTA
                        : (dias <= 60 ? SeveridadAlerta.MEDIA : SeveridadAlerta.BAJA);
                abrirOActualizarLote(TipoAlerta.POR_VENCER, severidad, lote,
                        "Lote %s de %s vence en %d dia(s) (%s) con saldo %s en %s".formatted(
                                lote.codigoLote(), lote.productoNombre(), dias,
                                lote.fechaVencimiento(),
                                CantidadUtil.cantidad(lote.cantidad()).toPlainString(),
                                lote.almacenNombre()));
            }
        }
        log.info("Evaluacion de vencimientos: {} lote(s) con saldo dentro de {} dias", lotes.size(),
                maximo);
        return lotes.size();
    }

    private void abrirOActualizar(TipoAlerta tipo, SeveridadAlerta severidad,
                                  StockConsolidadoResponse saldo, Long loteId, String mensaje,
                                  BigDecimal valor) {
        Alerta alerta = repository.buscarAbierta(tipo, saldo.productoId(), saldo.almacenId(), loteId)
                .orElseGet(() -> Alerta.builder()
                        .tipoAlerta(tipo)
                        .productoId(saldo.productoId())
                        .productoNombre(saldo.productoNombre())
                        .almacenId(saldo.almacenId())
                        .almacenNombre(saldo.almacenNombre())
                        .estado(EstadoAlerta.ACTIVA)
                        .fechaGeneracion(Instant.now())
                        .build());
        alerta.setSeveridad(severidad);
        alerta.setMensaje(mensaje);
        alerta.setValorReferencia(valor);
        repository.save(alerta);
    }

    private void abrirOActualizarLote(TipoAlerta tipo, SeveridadAlerta severidad,
                                      LoteVencimientoDto lote, String mensaje) {
        Alerta alerta = repository.buscarAbierta(tipo, lote.productoId(), lote.almacenId(),
                        lote.loteId())
                .orElseGet(() -> Alerta.builder()
                        .tipoAlerta(tipo)
                        .productoId(lote.productoId())
                        .productoNombre(lote.productoNombre())
                        .almacenId(lote.almacenId())
                        .almacenNombre(lote.almacenNombre())
                        .loteId(lote.loteId())
                        .codigoLote(lote.codigoLote())
                        .estado(EstadoAlerta.ACTIVA)
                        .fechaGeneracion(Instant.now())
                        .build());
        alerta.setSeveridad(severidad);
        alerta.setMensaje(mensaje);
        alerta.setValorReferencia(CantidadUtil.cantidad(lote.cantidad()));
        repository.save(alerta);
    }

    private void resolverSiExiste(TipoAlerta tipo, Long productoId, Long almacenId) {
        repository.buscarAbierta(tipo, productoId, almacenId, null).ifPresent(alerta -> {
            alerta.resolver();
            repository.save(alerta);
        });
    }

    private AlertaResponse aResponse(Alerta alerta) {
        return new AlertaResponse(alerta.getId(), alerta.getTipoAlerta(),
                alerta.getTipoAlerta().descripcion(), alerta.getSeveridad(), alerta.getEstado(),
                alerta.getProductoId(), alerta.getProductoNombre(), alerta.getAlmacenId(),
                alerta.getAlmacenNombre(), alerta.getLoteId(), alerta.getCodigoLote(),
                alerta.getMensaje(), alerta.getValorReferencia(), alerta.getFechaGeneracion(),
                alerta.getFechaResolucion());
    }
}
