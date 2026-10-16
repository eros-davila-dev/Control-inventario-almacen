package pe.cid.sgai.despacho.web;

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
import pe.cid.sgai.despacho.application.DespachoService;
import pe.cid.sgai.despacho.application.RequisicionService;
import pe.cid.sgai.despacho.application.dto.DespachoResponse;
import pe.cid.sgai.despacho.application.dto.RequisicionRequest;
import pe.cid.sgai.despacho.application.dto.RequisicionResponse;
import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Requisiciones", description = "Pedidos internos de las areas asistenciales")
@RestController
@RequestMapping(ApiRutas.REQUISICIONES)
@RequiredArgsConstructor
public class RequisicionController {

    private final RequisicionService service;
    private final DespachoService despachoService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_LEER + "')")
    public ResponseEntity<PageResponse<RequisicionResponse>> buscar(
            @RequestParam(required = false) Long almacenAtencionId,
            @RequestParam(required = false) Long almacenSolicitanteId,
            @RequestParam(required = false) EstadoRequisicion estado,
            @RequestParam(defaultValue = "false") boolean soloMias,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenAtencionId, almacenSolicitanteId, estado,
                soloMias, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_LEER + "')")
    public ResponseEntity<RequisicionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Despachos registrados contra la requisicion")
    @GetMapping("/{id}/despachos")
    @PreAuthorize("hasAuthority('" + Permisos.DESPACHO_LEER + "')")
    public ResponseEntity<List<DespachoResponse>> despachos(@PathVariable Long id) {
        return ResponseEntity.ok(despachoService.porRequisicion(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_CREAR + "')")
    public ResponseEntity<RequisicionResponse> crear(@Valid @RequestBody RequisicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_CREAR + "')")
    public ResponseEntity<RequisicionResponse> enviar(@PathVariable Long id) {
        return ResponseEntity.ok(service.enviar(id));
    }

    @Operation(summary = "Marca la requisicion en preparacion (picking)")
    @PostMapping("/{id}/en-atencion")
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_ATENDER + "')")
    public ResponseEntity<RequisicionResponse> enAtencion(@PathVariable Long id) {
        despachoService.marcarEnAtencion(id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_CREAR + "')")
    public ResponseEntity<RequisicionResponse> anular(@PathVariable Long id,
                                                      @RequestParam String motivo) {
        return ResponseEntity.ok(service.anular(id, motivo));
    }
}
