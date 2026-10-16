package pe.cid.sgai.conteo.web;

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
import pe.cid.sgai.conteo.application.TomaInventarioService;
import pe.cid.sgai.conteo.application.dto.ConteoRequest;
import pe.cid.sgai.conteo.application.dto.TomaInventarioRequest;
import pe.cid.sgai.conteo.application.dto.TomaInventarioResponse;
import pe.cid.sgai.conteo.domain.EstadoToma;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

@Tag(name = "Toma de inventario", description = "Conteo fisico ciego, diferencias, ERI y ajustes")
@RestController
@RequestMapping(ApiRutas.TOMAS_INVENTARIO)
@RequiredArgsConstructor
public class TomaInventarioController {

    private final TomaInventarioService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_LEER + "')")
    public ResponseEntity<PageResponse<TomaInventarioResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) EstadoToma estado,
            @PageableDefault(size = 20, sort = "fechaProgramada") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_LEER + "')")
    public ResponseEntity<TomaInventarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Programa una toma total, por categoria o ciclica ABC")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_PROGRAMAR + "')")
    public ResponseEntity<TomaInventarioResponse> programar(
            @Valid @RequestBody TomaInventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.programar(request));
    }

    @Operation(summary = "Abre la toma y congela el saldo teorico")
    @PostMapping("/{id}/abrir")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_EJECUTAR + "')")
    public ResponseEntity<TomaInventarioResponse> abrir(@PathVariable Long id) {
        return ResponseEntity.ok(service.abrir(id));
    }

    @Operation(summary = "Registra el conteo fisico (ciego) de una o varias lineas")
    @PostMapping("/{id}/conteos")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_EJECUTAR + "')")
    public ResponseEntity<TomaInventarioResponse> contar(@PathVariable Long id,
                                                         @Valid @RequestBody ConteoRequest request) {
        return ResponseEntity.ok(service.registrarConteo(id, request));
    }

    @Operation(summary = "Cierra el conteo y muestra las diferencias")
    @PostMapping("/{id}/cerrar-conteo")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_EJECUTAR + "')")
    public ResponseEntity<TomaInventarioResponse> cerrarConteo(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarContada(id));
    }

    @Operation(summary = "Aprueba las diferencias, genera los ajustes y calcula el ERI")
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_APROBAR + "')")
    public ResponseEntity<TomaInventarioResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aprobar(id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('" + Permisos.TOMA_PROGRAMAR + "')")
    public ResponseEntity<TomaInventarioResponse> anular(@PathVariable Long id,
                                                         @RequestParam String motivo) {
        return ResponseEntity.ok(service.anular(id, motivo));
    }
}
