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
import pe.cid.sgai.compras.application.RecepcionService;
import pe.cid.sgai.compras.application.dto.RecepcionRequest;
import pe.cid.sgai.compras.application.dto.RecepcionResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

@Tag(name = "Recepciones", description = "Recepcion de mercaderia con control de calidad sanitario")
@RestController
@RequestMapping(ApiRutas.RECEPCIONES)
@RequiredArgsConstructor
public class RecepcionController {

    private final RecepcionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.RECEPCION_LEER + "')")
    public ResponseEntity<PageResponse<RecepcionResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ordenId,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, ordenId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.RECEPCION_LEER + "')")
    public ResponseEntity<RecepcionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Registra la recepcion total o parcial de una orden de compra")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.RECEPCION_REGISTRAR + "')")
    public ResponseEntity<RecepcionResponse> registrar(@Valid @RequestBody RecepcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }
}
