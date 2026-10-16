package pe.cid.sgai.reportes.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.inventario.application.KardexService;
import pe.cid.sgai.inventario.application.StockService;
import pe.cid.sgai.inventario.application.dto.ConsumoMensualDto;
import pe.cid.sgai.inventario.application.dto.LoteVencimientoDto;
import pe.cid.sgai.inventario.application.dto.RotacionProductoDto;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.application.dto.ValorizacionCategoriaDto;
import pe.cid.sgai.inventario.domain.Kardex;
import pe.cid.sgai.inventario.repository.MovimientoLineaRepository;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.reportes.application.dto.BajaRotacionResponse;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Reportes operativos y de gestion (HU-44). Todas las consultas son de solo lectura
 * y se exportan en CSV compatible con Excel (separador ';' y BOM UTF-8).
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final String SEP = ";";
    private static final String BOM = "﻿";

    private final StockRepository stockRepository;
    private final MovimientoLineaRepository movimientoLineaRepository;
    private final StockService stockService;
    private final KardexService kardexService;
    private final PropiedadesSgai propiedades;

    @Transactional(readOnly = true)
    public List<LoteVencimientoDto> vencimientos(Long almacenId, Integer dias) {
        int ventana = dias == null ? 90 : dias;
        return stockRepository.vencimientosHasta(FechaUtil.hoy().plusDays(ventana), almacenId);
    }

    @Transactional(readOnly = true)
    public List<ValorizacionCategoriaDto> valorizacion(Long almacenId) {
        return stockRepository.valorizacionPorCategoria(almacenId);
    }

    @Transactional(readOnly = true)
    public List<ConsumoMensualDto> consumo(Long almacenId, Integer meses) {
        int ventana = meses == null ? 12 : meses;
        return movimientoLineaRepository.consumoMensual(
                FechaUtil.hoy().minusMonths(ventana).withDayOfMonth(1), almacenId);
    }

    @Transactional(readOnly = true)
    public List<RotacionProductoDto> rotacion(Long almacenId, Integer dias) {
        int ventana = dias == null ? propiedades.inventario().diasConsumoPromedio() : dias;
        return movimientoLineaRepository.rotacion(FechaUtil.hoy().minusDays(ventana), almacenId);
    }

    /** Productos con saldo inmovilizado: candidatos a redistribucion entre sedes. */
    @Transactional(readOnly = true)
    public List<BajaRotacionResponse> bajaRotacion(Long almacenId) {
        int diasLimite = propiedades.inventario().diasSinMovimiento();
        return stockService.consolidado(almacenId, null, null, false).stream()
                .map(saldo -> {
                    LocalDate ultimo = movimientoLineaRepository.ultimaFechaMovimiento(
                            saldo.productoId(), saldo.almacenId());
                    Long dias = ultimo == null ? null : FechaUtil.hoy().toEpochDay() - ultimo.toEpochDay();
                    return new BajaRotacionResponse(saldo.productoId(), saldo.productoCodigo(),
                            saldo.productoNombre(), saldo.almacenId(), saldo.almacenNombre(),
                            saldo.cantidad(), saldo.valorizado(), ultimo, dias);
                })
                .filter(r -> CantidadUtil.esPositiva(r.cantidad()))
                .filter(r -> r.diasSinMovimiento() == null || r.diasSinMovimiento() >= diasLimite)
                .sorted(Comparator.comparing(BajaRotacionResponse::valorizado).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockConsolidadoResponse> reposicion(Long almacenId) {
        return stockService.consolidado(almacenId, null, null, true);
    }

    // ------------------------------------------------------------------
    // Exportaciones CSV
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] kardexCsv(Long productoId, Long almacenId, LocalDate desde, LocalDate hasta) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(SEP, "Fecha", "Documento", "Tipo", "Lote", "Entrada", "Salida",
                "Saldo", "Costo unitario", "Costo promedio", "Valor saldo", "Usuario")).append('\n');
        for (Kardex k : kardexService.completo(productoId, almacenId, desde, hasta)) {
            sb.append(texto(k.getFecha())).append(SEP)
                    .append(texto(k.getNumeroDocumento())).append(SEP)
                    .append(texto(k.getTipoMovimiento().descripcion())).append(SEP)
                    .append(texto(k.getCodigoLote())).append(SEP)
                    .append(numero(k.getEntrada())).append(SEP)
                    .append(numero(k.getSalida())).append(SEP)
                    .append(numero(k.getSaldo())).append(SEP)
                    .append(numero(k.getCostoUnitario())).append(SEP)
                    .append(numero(k.getCostoPromedio())).append(SEP)
                    .append(numero(k.getValorSaldo())).append(SEP)
                    .append(texto(k.getUsuario())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] stockCsv(Long almacenId) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(SEP, "Codigo", "Producto", "Unidad", "Almacen", "Cantidad",
                "Reservada", "Disponible", "Minimo", "Reorden", "Maximo", "Costo promedio",
                "Valorizado", "Cobertura dias", "Estado")).append('\n');
        for (StockConsolidadoResponse s : stockService.consolidado(almacenId, null, null, false)) {
            sb.append(texto(s.productoCodigo())).append(SEP)
                    .append(texto(s.productoNombre())).append(SEP)
                    .append(texto(s.unidadMedida())).append(SEP)
                    .append(texto(s.almacenNombre())).append(SEP)
                    .append(numero(s.cantidad())).append(SEP)
                    .append(numero(s.reservada())).append(SEP)
                    .append(numero(s.disponible())).append(SEP)
                    .append(numero(s.stockMinimo())).append(SEP)
                    .append(numero(s.puntoReorden())).append(SEP)
                    .append(numero(s.stockMaximo())).append(SEP)
                    .append(numero(s.costoPromedio())).append(SEP)
                    .append(numero(s.valorizado())).append(SEP)
                    .append(numero(s.coberturaDias())).append(SEP)
                    .append(texto(s.estadoReposicion())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] vencimientosCsv(Long almacenId, Integer dias) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(SEP, "Lote", "Vencimiento", "Dias", "Estado", "Codigo", "Producto",
                "Almacen", "Cantidad", "Valor expuesto")).append('\n');
        for (LoteVencimientoDto l : vencimientos(almacenId, dias)) {
            sb.append(texto(l.codigoLote())).append(SEP)
                    .append(texto(l.fechaVencimiento())).append(SEP)
                    .append(FechaUtil.diasHasta(l.fechaVencimiento())).append(SEP)
                    .append(texto(l.estadoLote())).append(SEP)
                    .append(texto(l.productoCodigo())).append(SEP)
                    .append(texto(l.productoNombre())).append(SEP)
                    .append(texto(l.almacenNombre())).append(SEP)
                    .append(numero(l.cantidad())).append(SEP)
                    .append(numero(l.valorExpuesto())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] consumoCsv(Long almacenId, Integer meses) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(SEP, "Anio", "Mes", "Area", "Centro de costo", "Cantidad", "Valor"))
                .append('\n');
        for (ConsumoMensualDto c : consumo(almacenId, meses)) {
            sb.append(c.anio()).append(SEP)
                    .append(c.mes()).append(SEP)
                    .append(texto(c.area())).append(SEP)
                    .append(texto(c.centroCosto())).append(SEP)
                    .append(numero(c.cantidad())).append(SEP)
                    .append(numero(c.valor())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Escapa el separador y las comillas para que el CSV no se rompa. */
    private String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        if (texto.contains(SEP) || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    /** Coma decimal: es el formato que espera Excel en configuracion regional peruana. */
    private String numero(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }

    @Transactional(readOnly = true)
    public Pageable sinPaginacion() {
        return Pageable.unpaged();
    }
}
