package pe.cid.sgai.compras.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.compras.application.RequerimientoCompraService;
import pe.cid.sgai.compras.application.dto.RequerimientoRequest;
import pe.cid.sgai.compras.application.dto.RequerimientoResponse;
import pe.cid.sgai.compras.application.dto.SugerenciaReposicionResponse;
import pe.cid.sgai.compras.domain.EstadoRequerimiento;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Requerimientos de compra", description = "Reposicion sugerida y requerimientos de almacen")
@RestController
@RequestMapping(ApiRutas.REQUERIMIENTOS)
@RequiredArgsConstructor
public class RequerimientoCompraController {

    private final RequerimientoCompraService service;

    @Operation(summary = "Propuesta de reposicion del almacen segun punto de reorden y consumo")
    @GetMapping("/sugerencia/{almacenId}")
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_LEER + "')")
    public ResponseEntity<List<SugerenciaReposicionResponse>> sugerencia(@PathVariable Long almacenId) {
        return ResponseEntity.ok(service.sugerirReposicion(almacenId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_LEER + "')")
    public ResponseEntity<PageResponse<RequerimientoResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) EstadoRequerimiento estado,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_LEER + "')")
    public ResponseEntity<RequerimientoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_CREAR + "')")
    public ResponseEntity<RequerimientoResponse> crear(@Valid @RequestBody RequerimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Envia el requerimiento para aprobacion")
    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_CREAR + "')")
    public ResponseEntity<RequerimientoResponse> enviar(@PathVariable Long id) {
        return ResponseEntity.ok(service.enviar(id));
    }

    @Operation(summary = "Aprueba el requerimiento (segregacion de funciones)")
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_APROBAR + "')")
    public ResponseEntity<RequerimientoResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('" + Permisos.REQUERIMIENTO_APROBAR + "')")
    public ResponseEntity<RequerimientoResponse> rechazar(@PathVariable Long id,
                                                          @RequestParam String motivo) {
        return ResponseEntity.ok(service.rechazar(id, motivo));
    }
}
