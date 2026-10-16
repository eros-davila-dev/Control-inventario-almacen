package pe.cid.sgai.inventario.web;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.LoteService;
import pe.cid.sgai.inventario.application.dto.CambioEstadoLoteRequest;
import pe.cid.sgai.inventario.application.dto.LoteRequest;
import pe.cid.sgai.inventario.application.dto.LoteResponse;
import pe.cid.sgai.inventario.application.dto.TrazabilidadLoteResponse;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Lotes", description = "Lotes, vencimientos, liberacion sanitaria y trazabilidad")
@RestController
@RequestMapping(ApiRutas.LOTES)
@RequiredArgsConstructor
public class LoteController {

    private final LoteService service;

    @Operation(summary = "Busca lotes por producto, estado o vencimiento")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_LEER + "')")
    public ResponseEntity<PageResponse<LoteResponse>> buscar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoLote estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceHasta,
            @PageableDefault(size = 20, sort = "fechaVencimiento") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(productoId, texto, estado, venceHasta, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_LEER + "')")
    public ResponseEntity<LoteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Lotes liberados de un producto, ordenados por vencimiento (FEFO)")
    @GetMapping("/producto/{productoId}/liberados")
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_LEER + "')")
    public ResponseEntity<List<LoteResponse>> liberados(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.liberadosDe(productoId));
    }

    @Operation(summary = "Registra un lote del fabricante")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_ESCRIBIR + "')")
    public ResponseEntity<LoteResponse> crear(@Valid @RequestBody LoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Libera un lote (solo quimico farmaceutico o administrador)")
    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_LIBERAR + "')")
    public ResponseEntity<LoteResponse> liberar(@PathVariable Long id,
                                                @Valid @RequestBody CambioEstadoLoteRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id,
                new CambioEstadoLoteRequest(EstadoLote.LIBERADO, request.motivo())));
    }

    @Operation(summary = "Retiene, rechaza o devuelve a cuarentena un lote")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_RETENER + "')")
    public ResponseEntity<LoteResponse> cambiarEstado(@PathVariable Long id,
                                                      @Valid @RequestBody CambioEstadoLoteRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }

    @Operation(summary = "Trazabilidad completa del lote: donde esta y donde se consumio")
    @GetMapping("/{id}/trazabilidad")
    @PreAuthorize("hasAuthority('" + Permisos.LOTE_LEER + "')")
    public ResponseEntity<TrazabilidadLoteResponse> trazabilidad(@PathVariable Long id) {
        return ResponseEntity.ok(service.trazabilidad(id));
    }
}
