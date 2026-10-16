package pe.cid.sgai.reportes.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.dto.ConsumoMensualDto;
import pe.cid.sgai.inventario.application.dto.LoteVencimientoDto;
import pe.cid.sgai.inventario.application.dto.RotacionProductoDto;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.application.dto.ValorizacionCategoriaDto;
import pe.cid.sgai.reportes.application.ReporteService;
import pe.cid.sgai.reportes.application.dto.BajaRotacionResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.util.FechaUtil;
import pe.cid.sgai.shared.web.ApiRutas;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Reportes", description = "Reportes de gestion con exportacion a CSV")
@RestController
@RequestMapping(ApiRutas.REPORTES)
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;

    @Operation(summary = "Lotes por vencer y vencidos con valor expuesto")
    @GetMapping("/vencimientos")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<LoteVencimientoDto>> vencimientos(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(service.vencimientos(almacenId, dias));
    }

    @Operation(summary = "Valorizacion del inventario por categoria")
    @GetMapping("/valorizacion")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<ValorizacionCategoriaDto>> valorizacion(
            @RequestParam(required = false) Long almacenId) {
        return ResponseEntity.ok(service.valorizacion(almacenId));
    }

    @Operation(summary = "Consumo por area y centro de costo (mensual)")
    @GetMapping("/consumo")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<ConsumoMensualDto>> consumo(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Integer meses) {
        return ResponseEntity.ok(service.consumo(almacenId, meses));
    }

    @Operation(summary = "Rotacion de productos por consumo")
    @GetMapping("/rotacion")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<RotacionProductoDto>> rotacion(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(service.rotacion(almacenId, dias));
    }

    @Operation(summary = "Productos con saldo inmovilizado (sin movimiento en el periodo)")
    @GetMapping("/baja-rotacion")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<BajaRotacionResponse>> bajaRotacion(
            @RequestParam(required = false) Long almacenId) {
        return ResponseEntity.ok(service.bajaRotacion(almacenId));
    }

    @Operation(summary = "Productos que requieren reposicion")
    @GetMapping("/reposicion")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<List<StockConsolidadoResponse>> reposicion(
            @RequestParam(required = false) Long almacenId) {
        return ResponseEntity.ok(service.reposicion(almacenId));
    }

    @Operation(summary = "Exporta el kardex valorizado a CSV")
    @GetMapping("/kardex.csv")
    @PreAuthorize("hasAuthority('" + Permisos.KARDEX_LEER + "')")
    public ResponseEntity<byte[]> kardexCsv(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(service.kardexCsv(productoId, almacenId, desde, hasta), "kardex");
    }

    @Operation(summary = "Exporta el saldo consolidado a CSV")
    @GetMapping("/stock.csv")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<byte[]> stockCsv(@RequestParam(required = false) Long almacenId) {
        return csv(service.stockCsv(almacenId), "stock");
    }

    @Operation(summary = "Exporta el reporte de vencimientos a CSV")
    @GetMapping("/vencimientos.csv")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<byte[]> vencimientosCsv(@RequestParam(required = false) Long almacenId,
                                                  @RequestParam(required = false) Integer dias) {
        return csv(service.vencimientosCsv(almacenId, dias), "vencimientos");
    }

    @Operation(summary = "Exporta el consumo por area a CSV")
    @GetMapping("/consumo.csv")
    @PreAuthorize("hasAuthority('" + Permisos.REPORTE_LEER + "')")
    public ResponseEntity<byte[]> consumoCsv(@RequestParam(required = false) Long almacenId,
                                             @RequestParam(required = false) Integer meses) {
        return csv(service.consumoCsv(almacenId, meses), "consumo");
    }

    private ResponseEntity<byte[]> csv(byte[] contenido, String nombre) {
        String archivo = "%s-%s.csv".formatted(nombre, FechaUtil.hoy());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(contenido);
    }
}
