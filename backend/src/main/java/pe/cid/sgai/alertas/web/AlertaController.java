package pe.cid.sgai.alertas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.alertas.application.AlertaService;
import pe.cid.sgai.alertas.application.dto.AlertaResponse;
import pe.cid.sgai.alertas.application.dto.ResumenAlertasResponse;
import pe.cid.sgai.alertas.domain.EstadoAlerta;
import pe.cid.sgai.alertas.domain.SeveridadAlerta;
import pe.cid.sgai.alertas.domain.TipoAlerta;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

@Tag(name = "Alertas", description = "Alertas de reposicion, vencimiento y retencion de lotes")
@RestController
@RequestMapping(ApiRutas.ALERTAS)
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.ALERTA_LEER + "')")
    public ResponseEntity<PageResponse<AlertaResponse>> buscar(
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(required = false) SeveridadAlerta severidad,
            @RequestParam(required = false) EstadoAlerta estado,
            @RequestParam(required = false) Long almacenId,
            @PageableDefault(size = 25, sort = "fechaGeneracion") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(tipo, severidad, estado, almacenId, pageable));
    }

    @Operation(summary = "Contadores de alertas para la campana de notificaciones")
    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('" + Permisos.ALERTA_LEER + "')")
    public ResponseEntity<ResumenAlertasResponse> resumen(
            @RequestParam(required = false) Long almacenId) {
        return ResponseEntity.ok(service.resumen(almacenId));
    }

    @PatchMapping("/{id}/leida")
    @PreAuthorize("hasAuthority('" + Permisos.ALERTA_LEER + "')")
    public ResponseEntity<AlertaResponse> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarLeida(id));
    }

    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasAuthority('" + Permisos.ALERTA_LEER + "')")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long id) {
        return ResponseEntity.ok(service.resolver(id));
    }

    @Operation(summary = "Recalcula las alertas de vencimiento y reposicion bajo demanda")
    @PostMapping("/recalcular")
    @PreAuthorize("hasAuthority('" + Permisos.ALERTA_LEER + "')")
    public ResponseEntity<ResumenAlertasResponse> recalcular(
            @RequestParam(required = false) Long almacenId) {
        service.evaluarVencimientos();
        service.evaluarReposicionGlobal();
        return ResponseEntity.ok(service.resumen(almacenId));
    }
}
