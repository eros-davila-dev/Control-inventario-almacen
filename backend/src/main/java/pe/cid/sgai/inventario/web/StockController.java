package pe.cid.sgai.inventario.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.AsignacionLoteService;
import pe.cid.sgai.inventario.application.StockService;
import pe.cid.sgai.inventario.application.dto.StockConsolidadoResponse;
import pe.cid.sgai.inventario.application.dto.StockResponse;
import pe.cid.sgai.inventario.application.dto.SugerenciaLoteResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Stock", description = "Saldos en linea por producto, almacen, lote y ubicacion")
@RestController
@RequestMapping(ApiRutas.STOCK)
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final AsignacionLoteService asignacionLoteService;

    @Operation(summary = "Saldo detallado por lote y ubicacion")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<PageResponse<StockResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "true") boolean soloConSaldo,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(stockService.buscar(almacenId, productoId, categoriaId, texto,
                soloConSaldo, pageable));
    }

    @Operation(summary = "Saldo consolidado con semaforo de reposicion y cobertura en dias")
    @GetMapping("/consolidado")
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<List<StockConsolidadoResponse>> consolidado(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "false") boolean soloReposicion) {
        return ResponseEntity.ok(stockService.consolidado(almacenId, categoriaId, texto, soloReposicion));
    }

    @Operation(summary = "Saldo de un producto en un almacen, desglosado por lote")
    @GetMapping("/producto/{productoId}/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<List<StockResponse>> porProductoYAlmacen(
            @org.springframework.web.bind.annotation.PathVariable Long productoId,
            @org.springframework.web.bind.annotation.PathVariable Long almacenId) {
        return ResponseEntity.ok(stockService.porProductoYAlmacen(productoId, almacenId));
    }

    @Operation(summary = "Sugerencia de lotes por politica FEFO para una cantidad solicitada")
    @GetMapping("/sugerencia-fefo")
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<List<SugerenciaLoteResponse>> sugerenciaFefo(
            @RequestParam Long productoId,
            @RequestParam Long almacenId,
            @RequestParam BigDecimal cantidad) {
        return ResponseEntity.ok(asignacionLoteService.sugerir(productoId, almacenId, cantidad));
    }
}
