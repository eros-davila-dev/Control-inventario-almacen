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
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.catalogo.application.UnidadMedidaService;
import pe.cid.sgai.catalogo.application.dto.UnidadMedidaRequest;
import pe.cid.sgai.catalogo.application.dto.UnidadMedidaResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Unidades de medida", description = "Maestro de unidades y factores de conversion")
@RestController
@RequestMapping(ApiRutas.UNIDADES_MEDIDA)
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService service;

    @Operation(summary = "Lista paginada de unidades de medida")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<PageResponse<UnidadMedidaResponse>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @Operation(summary = "Unidades activas para selectores")
    @GetMapping("/activas")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<List<UnidadMedidaResponse>> activas() {
        return ResponseEntity.ok(service.listarActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<UnidadMedidaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.UNIDAD_ESCRIBIR + "')")
    public ResponseEntity<UnidadMedidaResponse> crear(@Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.UNIDAD_ESCRIBIR + "')")
    public ResponseEntity<UnidadMedidaResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.UNIDAD_ESCRIBIR + "')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
