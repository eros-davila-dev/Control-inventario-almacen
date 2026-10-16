package pe.cid.sgai.inventario.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.MovimientoService;
import pe.cid.sgai.inventario.application.dto.AjusteRequest;
import pe.cid.sgai.inventario.application.dto.AnulacionRequest;
import pe.cid.sgai.inventario.application.dto.IngresoRequest;
import pe.cid.sgai.inventario.application.dto.MovimientoResponse;
import pe.cid.sgai.inventario.application.dto.SalidaRequest;
import pe.cid.sgai.inventario.domain.EstadoMovimiento;
import pe.cid.sgai.inventario.domain.TipoMovimiento;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.LocalDate;

@Tag(name = "Movimientos", description = "Ingresos, salidas, ajustes y anulaciones de inventario")
@RestController
@RequestMapping(ApiRutas.MOVIMIENTOS)
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService service;

    @Operation(summary = "Busca movimientos por almacen, tipo, estado y rango de fechas")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<PageResponse<MovimientoResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) EstadoMovimiento estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, tipo, estado, desde, hasta, texto, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<MovimientoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Registra un ingreso de inventario con lote, vencimiento y costo")
    @PostMapping("/ingresos")
    @PreAuthorize("hasAuthority('" + Permisos.MOVIMIENTO_REGISTRAR + "')")
    public ResponseEntity<MovimientoResponse> ingresar(
            @Valid @RequestBody IngresoRequest request,
            @Parameter(description = "Clave de idempotencia para evitar duplicados por reintento")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrarIngreso(request, idempotencyKey));
    }

    @Operation(summary = "Registra una salida de inventario aplicando FEFO")
    @PostMapping("/salidas")
    @PreAuthorize("hasAuthority('" + Permisos.MOVIMIENTO_REGISTRAR + "')")
    public ResponseEntity<MovimientoResponse> salir(
            @Valid @RequestBody SalidaRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrarSalida(request, idempotencyKey));
    }

    @Operation(summary = "Registra un ajuste de inventario con motivo tipificado")
    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('" + Permisos.MOVIMIENTO_REGISTRAR + "')")
    public ResponseEntity<MovimientoResponse> ajustar(@Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarAjuste(request));
    }

    @Operation(summary = "Anula un movimiento generando el movimiento inverso enlazado")
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('" + Permisos.MOVIMIENTO_ANULAR + "')")
    public ResponseEntity<MovimientoResponse> anular(@PathVariable Long id,
                                                     @Valid @RequestBody AnulacionRequest request) {
        return ResponseEntity.ok(service.anular(id, request));
    }
}
