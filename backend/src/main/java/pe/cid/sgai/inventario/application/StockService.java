package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.Ubicacion;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.inventario.application.dto.StockAgregadoDto;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.application.dto.StockResponse;
import pe.cid.sgai.inventario.application.mapper.InventarioMapper;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.repository.MovimientoLineaRepository;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.web.PageResponse;

import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Saldos de inventario (HU-26). Mantiene la celda producto - almacen - lote - ubicacion
 * y expone el saldo consolidado con semaforo de reposicion y cobertura en dias.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    public static final String SIN_STOCK = "AGOTADO";
    public static final String BAJO_MINIMO = "BAJO_MINIMO";
    public static final String EN_REORDEN = "EN_PUNTO_REORDEN";
    public static final String SOBRE_STOCK = "SOBRE_STOCK";
    public static final String NORMAL = "NORMAL";

    private final StockRepository repository;
    private final MovimientoLineaRepository movimientoLineaRepository;
    private final InventarioMapper mapper;
    private final PropiedadesSgai propiedades;

    @Transactional(readOnly = true)
    public PageResponse<StockResponse> buscar(Long almacenId, Long productoId, Long categoriaId,
                                              String texto, boolean soloConSaldo, Pageable pageable) {
        return PageResponse.de(repository.buscar(almacenId, productoId, categoriaId,
                texto == null || texto.isBlank() ? null : texto.trim(), soloConSaldo, pageable),
                mapper::aResponse);
    }

    /** Saldo consolidado con estado de reposicion y cobertura (HU-39, HU-41). */
    @Transactional(readOnly = true)
    public List<StockConsolidadoResponse> consolidado(Long almacenId, Long categoriaId, String texto,
                                                      boolean soloReposicion) {
        int dias = propiedades.inventario().diasConsumoPromedio();
        return repository.consolidado(almacenId, categoriaId,
                        texto == null || texto.isBlank() ? null : texto.trim()).stream()
                .map(dto -> aConsolidado(dto, dias))
                .filter(r -> !soloReposicion || requiereReposicion(r.estadoReposicion()))
                .sorted(Comparator.comparing(StockConsolidadoResponse::productoNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockResponse> porProductoYAlmacen(Long productoId, Long almacenId) {
        return mapper.aStockResponses(repository.buscar(almacenId, productoId, null, null, true,
                Pageable.unpaged()).getContent());
    }

    @Transactional(readOnly = true)
    public BigDecimal disponibleEn(Long productoId, Long almacenId) {
        return CantidadUtil.cantidad(repository.saldoProductoAlmacen(productoId, almacenId));
    }

    /** Consumo promedio diario calculado sobre la ventana configurada (90 dias). */
    @Transactional(readOnly = true)
    public BigDecimal consumoPromedioDiario(Long productoId, Long almacenId) {
        int dias = propiedades.inventario().diasConsumoPromedio();
        BigDecimal consumo = CantidadUtil.nvl(movimientoLineaRepository.consumoDesde(
                productoId, almacenId, FechaUtil.hoy().minusDays(dias)));
        return CantidadUtil.dividirSeguro(consumo, BigDecimal.valueOf(dias), 4);
    }

    // ---------------------------------------------------------------------
    // Operaciones internas usadas por MovimientoService (unico escritor)
    // ---------------------------------------------------------------------

    @Transactional
    public Stock obtenerOCrearCelda(Producto producto, Almacen almacen, Lote lote, Ubicacion ubicacion) {
        return repository.buscarCelda(producto.getId(), almacen.getId(),
                        lote == null ? null : lote.getId(),
                        ubicacion == null ? null : ubicacion.getId())
                .orElseGet(() -> repository.save(Stock.builder()
                        .producto(producto)
                        .almacen(almacen)
                        .lote(lote)
                        .ubicacion(ubicacion)
                        .cantidad(BigDecimal.ZERO)
                        .cantidadReservada(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public Stock ingresar(Producto producto, Almacen almacen, Lote lote, Ubicacion ubicacion,
                          BigDecimal cantidad) {
        Stock stock = obtenerOCrearCelda(producto, almacen, lote, ubicacion);
        stock.incrementar(cantidad);
        return repository.saveAndFlush(stock);
    }

    @Transactional
    public Stock retirar(Producto producto, Almacen almacen, Lote lote, Ubicacion ubicacion,
                         BigDecimal cantidad) {
        Stock stock = repository.buscarCelda(producto.getId(), almacen.getId(),
                        lote == null ? null : lote.getId(),
                        ubicacion == null ? null : ubicacion.getId())
                .orElseThrow(() -> new pe.cid.sgai.shared.exception.NegocioException(
                        pe.cid.sgai.shared.exception.CodigoError.STOCK_INSUFICIENTE,
                        "No existe saldo de %s en %s%s".formatted(producto.getNombre(),
                                almacen.getNombre(),
                                lote == null ? "" : " lote " + lote.getCodigoLote())));
        stock.descontar(cantidad);
        return repository.saveAndFlush(stock);
    }

    /**
     * Reparte la cantidad a retirar entre las celdas de stock que la contienen.
     * Si se indica la ubicacion se usa solo esa celda; si no, se consumen las
     * celdas del lote en orden de ubicacion. Evita el error de buscar el saldo
     * en una celda "sin ubicacion" cuando la mercaderia esta ubicada.
     */
    @Transactional(readOnly = true)
    public List<EstrategiaAsignacionLote.Asignacion> distribuirSalida(
            Producto producto, Almacen almacen, Lote lote, Ubicacion ubicacion, BigDecimal cantidad) {
        BigDecimal pendiente = CantidadUtil.cantidad(cantidad);
        List<Stock> celdas;
        if (ubicacion != null) {
            celdas = repository.buscarCelda(producto.getId(), almacen.getId(),
                            lote == null ? null : lote.getId(), ubicacion.getId())
                    .map(List::of)
                    .orElse(List.of());
        } else {
            celdas = repository.celdasConSaldo(producto.getId(), almacen.getId(),
                    lote == null ? null : lote.getId());
        }
        List<EstrategiaAsignacionLote.Asignacion> asignaciones = new ArrayList<>();
        BigDecimal disponibleTotal = BigDecimal.ZERO;
        for (Stock celda : celdas) {
            BigDecimal disponible = CantidadUtil.cantidad(celda.getCantidad());
            if (!CantidadUtil.esPositiva(disponible)) {
                continue;
            }
            disponibleTotal = disponibleTotal.add(disponible);
            BigDecimal aTomar = disponible.min(pendiente);
            asignaciones.add(new EstrategiaAsignacionLote.Asignacion(celda,
                    CantidadUtil.cantidad(aTomar)));
            pendiente = pendiente.subtract(aTomar);
            if (!CantidadUtil.esPositiva(pendiente)) {
                break;
            }
        }
        if (CantidadUtil.esPositiva(pendiente)) {
            throw new NegocioException(CodigoError.STOCK_INSUFICIENTE,
                    "Stock insuficiente de %s en %s%s: disponible %s, solicitado %s".formatted(
                            producto.getNombre(), almacen.getNombre(),
                            lote == null ? "" : " lote " + lote.getCodigoLote(),
                            disponibleTotal.toPlainString(),
                            CantidadUtil.cantidad(cantidad).toPlainString()))
                    .con("productoId", producto.getId())
                    .con("almacenId", almacen.getId())
                    .con("disponible", CantidadUtil.cantidad(disponibleTotal));
        }
        return asignaciones;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoTotalProducto(Long productoId) {
        return CantidadUtil.cantidad(repository.saldoTotalProducto(productoId));
    }

    private StockConsolidadoResponse aConsolidado(StockAgregadoDto dto, int diasVentana) {
        BigDecimal cantidad = CantidadUtil.cantidad(dto.cantidad());
        BigDecimal reservada = CantidadUtil.cantidad(dto.reservada());
        BigDecimal disponible = CantidadUtil.cantidad(cantidad.subtract(reservada));
        BigDecimal costo = CantidadUtil.nvl(dto.costoPromedio());
        BigDecimal consumoTotal = CantidadUtil.nvl(movimientoLineaRepository.consumoDesde(
                dto.productoId(), dto.almacenId(), FechaUtil.hoy().minusDays(diasVentana)));
        BigDecimal promedioDiario = CantidadUtil.dividirSeguro(consumoTotal,
                BigDecimal.valueOf(diasVentana), 4);
        BigDecimal cobertura = promedioDiario.compareTo(BigDecimal.ZERO) == 0
                ? null : disponible.divide(promedioDiario, 1, RoundingMode.HALF_UP);
        return new StockConsolidadoResponse(
                dto.productoId(), dto.productoCodigo(), dto.productoNombre(), dto.unidadMedida(),
                dto.almacenId(), dto.almacenNombre(), cantidad, reservada, disponible,
                CantidadUtil.cantidad(dto.stockMinimo()), CantidadUtil.cantidad(dto.puntoReorden()),
                CantidadUtil.cantidad(dto.stockMaximo()), CantidadUtil.costo(costo),
                CantidadUtil.importe(cantidad.multiply(costo)), promedioDiario, cobertura,
                Boolean.TRUE.equals(dto.critico()),
                estadoReposicion(disponible, dto.stockMinimo(), dto.puntoReorden(), dto.stockMaximo()));
    }

    /** Semaforo de reposicion usado por el dashboard y el modulo de alertas. */
    public static String estadoReposicion(BigDecimal disponible, BigDecimal minimo,
                                          BigDecimal reorden, BigDecimal maximo) {
        BigDecimal disp = CantidadUtil.nvl(disponible);
        if (disp.compareTo(BigDecimal.ZERO) <= 0) {
            return SIN_STOCK;
        }
        if (CantidadUtil.esPositiva(minimo) && disp.compareTo(minimo) <= 0) {
            return BAJO_MINIMO;
        }
        if (CantidadUtil.esPositiva(reorden) && disp.compareTo(reorden) <= 0) {
            return EN_REORDEN;
        }
        if (CantidadUtil.esPositiva(maximo) && disp.compareTo(maximo) > 0) {
            return SOBRE_STOCK;
        }
        return NORMAL;
    }

    public static boolean requiereReposicion(String estado) {
        return SIN_STOCK.equals(estado) || BAJO_MINIMO.equals(estado) || EN_REORDEN.equals(estado);
    }
}
