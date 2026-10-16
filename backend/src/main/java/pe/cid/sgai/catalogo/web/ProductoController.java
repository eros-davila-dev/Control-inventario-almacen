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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.catalogo.application.ProductoService;
import pe.cid.sgai.catalogo.application.dto.ProductoRequest;
import pe.cid.sgai.catalogo.application.dto.ProductoResponse;
import pe.cid.sgai.catalogo.application.dto.ProductoResumenResponse;
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.catalogo.domain.TipoProducto;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Productos", description = "Catalogo de insumos, medicamentos y materiales")
@RestController
@RequestMapping(ApiRutas.PRODUCTOS)
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @Operation(summary = "Busca productos por codigo, nombre, marca, categoria o tipo")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<PageResponse<ProductoResponse>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) TipoProducto tipo,
            @RequestParam(required = false) ClasificacionAbc clasificacion,
            @RequestParam(required = false) Boolean critico,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(texto, categoriaId, tipo, clasificacion,
                critico, activo, pageable));
    }

    @Operation(summary = "Productos activos en formato resumido para lineas de documento")
    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<List<ProductoResumenResponse>> resumen() {
        return ResponseEntity.ok(service.listarResumen());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_LEER + "')")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_ESCRIBIR + "')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_ESCRIBIR + "')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @Operation(summary = "Activa o desactiva un producto sin borrar su historia")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTO_ESCRIBIR + "')")
    public ResponseEntity<ProductoResponse> cambiarEstado(@PathVariable Long id,
                                                          @RequestParam boolean activo) {
        return ResponseEntity.ok(service.cambiarEstado(id, activo));
    }
}
