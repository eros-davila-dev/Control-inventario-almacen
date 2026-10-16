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
import pe.cid.sgai.catalogo.application.CategoriaService;
import pe.cid.sgai.catalogo.application.dto.CategoriaRequest;
import pe.cid.sgai.catalogo.application.dto.CategoriaResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Categorias", description = "Arbol de categorias del catalogo")
@RestController
@RequestMapping(ApiRutas.CATEGORIAS)
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @Operation(summary = "Lista paginada de categorias")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<PageResponse<CategoriaResponse>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @Operation(summary = "Categorias activas para selectores")
    @GetMapping("/activas")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<List<CategoriaResponse>> activas() {
        return ResponseEntity.ok(service.listarActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<CategoriaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.CATEGORIA_ESCRIBIR + "')")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CATEGORIA_ESCRIBIR + "')")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CATEGORIA_ESCRIBIR + "')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
