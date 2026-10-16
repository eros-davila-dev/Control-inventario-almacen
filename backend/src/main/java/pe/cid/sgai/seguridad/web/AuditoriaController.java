package pe.cid.sgai.seguridad.web;

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
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.audit.AuditoriaLog;
import pe.cid.sgai.shared.audit.AuditoriaService;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.Instant;
import java.util.List;

@Tag(name = "Auditoria", description = "Pista de auditoria inmutable de las operaciones del sistema")
@RestController
@RequestMapping(ApiRutas.AUDITORIA)
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Consulta la pista de auditoria con filtros")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.AUDITORIA_LEER + "')")
    public ResponseEntity<PageResponse<AuditoriaLog>> buscar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.de(
                auditoriaService.buscar(usuario, entidad, entidadId, accion, desde, hasta, pageable)));
    }

    @Operation(summary = "Historial de auditoria de una entidad concreta")
    @GetMapping("/{entidad}/{entidadId}")
    @PreAuthorize("hasAuthority('" + Permisos.AUDITORIA_LEER + "')")
    public ResponseEntity<List<AuditoriaLog>> historial(@org.springframework.web.bind.annotation.PathVariable String entidad,
                                                        @org.springframework.web.bind.annotation.PathVariable String entidadId) {
        return ResponseEntity.ok(auditoriaService.historialDe(entidad, entidadId));
    }
}
