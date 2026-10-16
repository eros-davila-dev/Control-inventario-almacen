package pe.cid.sgai.reportes.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.reportes.application.DashboardService;
import pe.cid.sgai.reportes.application.dto.DashboardResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;

@Tag(name = "Dashboard", description = "Indicadores de gestion del almacen")
@RestController
@RequestMapping(ApiRutas.DASHBOARD)
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @Operation(summary = "Indicadores del almacen: valor, reposicion, vencimientos y pendientes")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.DASHBOARD_LEER + "')")
    public ResponseEntity<DashboardResponse> indicadores(
            @RequestParam(required = false) Long almacenId) {
        return ResponseEntity.ok(service.indicadores(almacenId));
    }
}
