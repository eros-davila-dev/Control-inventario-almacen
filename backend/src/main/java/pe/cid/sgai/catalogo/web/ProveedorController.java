package pe.cid.sgai.catalogo.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.catalogo.application.ProveedorService;
import pe.cid.sgai.catalogo.application.dto.ProveedorRequest;
import pe.cid.sgai.catalogo.application.dto.ProveedorResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Proveedores", description = "Maestro de proveedores de insumos y medicamentos")
@RestController
@RequestMapping(ApiRutas.PROVEEDORES)
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    @Operation(summary = "Busca proveedores por RUC o razon social")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_LEER + "')")
    public ResponseEntity<PageResponse<ProveedorResponse>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "razonSocial") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(texto, activo, pageable));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_LEER + "')")
    public ResponseEntity<List<ProveedorResponse>> activos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_LEER + "')")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_ESCRIBIR + "')")
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_ESCRIBIR + "')")
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PROVEEDOR_ESCRIBIR + "')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
