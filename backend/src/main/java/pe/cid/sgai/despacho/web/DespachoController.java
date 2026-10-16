package pe.cid.sgai.despacho.web;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.despacho.application.DespachoService;
import pe.cid.sgai.despacho.application.dto.DespachoRequest;
import pe.cid.sgai.despacho.application.dto.DespachoResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.LocalDate;

@Tag(name = "Despachos", description = "Atencion de requisiciones con politica FEFO")
@RestController
@RequestMapping(ApiRutas.DESPACHOS)
@RequiredArgsConstructor
public class DespachoController {

    private final DespachoService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.DESPACHO_LEER + "')")
    public ResponseEntity<PageResponse<DespachoResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, desde, hasta, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.DESPACHO_LEER + "')")
    public ResponseEntity<DespachoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Atiende una requisicion aplicando FEFO y genera la salida de inventario")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.REQUISICION_ATENDER + "')")
    public ResponseEntity<DespachoResponse> atender(@Valid @RequestBody DespachoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.atender(request));
    }
}
