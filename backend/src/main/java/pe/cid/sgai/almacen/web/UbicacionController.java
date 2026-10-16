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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.almacen.application.UbicacionService;
import pe.cid.sgai.almacen.application.dto.UbicacionRequest;
import pe.cid.sgai.almacen.application.dto.UbicacionResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Ubicaciones", description = "Zonas, racks y niveles por almacen")
@RestController
@RequestMapping(ApiRutas.UBICACIONES)
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_LEER + "')")
    public ResponseEntity<PageResponse<UbicacionResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, texto, pageable));
    }

    @Operation(summary = "Ubicaciones activas de un almacen")
    @GetMapping("/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('" + Permisos.ALMACEN_LEER + "')")
    public ResponseEntity<List<UbicacionResponse>> porAlmacen(@PathVariable Long almacenId) {
        return ResponseEntity.ok(service.porAlmacen(almacenId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.UBICACION_ESCRIBIR + "')")
    public ResponseEntity<UbicacionResponse> crear(@Valid @RequestBody UbicacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.UBICACION_ESCRIBIR + "')")
    public ResponseEntity<UbicacionResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody UbicacionRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.UBICACION_ESCRIBIR + "')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
