package pe.cid.sgai.almacen.web;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.almacen.application.AlmacenService;
import pe.cid.sgai.almacen.application.dto.AlmacenRequest;
import pe.cid.sgai.almacen.application.dto.AlmacenResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Almacenes", description = "Red de almacenes por sede")
@RestController
@RequestMapping(ApiRutas.ALMACENES)
@RequiredArgsConstructor
public class AlmacenController {

    private final AlmacenService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_LEER + "')")
    public ResponseEntity<PageResponse<AlmacenResponse>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @Operation(summary = "Almacenes activos")
    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_LEER + "')")
    public ResponseEntity<List<AlmacenResponse>> activos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @Operation(summary = "Almacenes que el usuario autenticado puede operar")
    @GetMapping("/autorizados")
    public ResponseEntity<List<AlmacenResponse>> autorizados() {
        return ResponseEntity.ok(service.listarAutorizados());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_LEER + "')")
    public ResponseEntity<AlmacenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_ESCRIBIR + "')")
    public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_ESCRIBIR + "')")
    public ResponseEntity<AlmacenResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }
}
