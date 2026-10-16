package pe.cid.sgai.seguridad.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import pe.cid.sgai.seguridad.application.PermisoService;
import pe.cid.sgai.seguridad.application.RolService;
import pe.cid.sgai.seguridad.application.dto.PermisoResponse;
import pe.cid.sgai.seguridad.application.dto.RolRequest;
import pe.cid.sgai.seguridad.application.dto.RolResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;

import java.util.List;
import java.util.Map;

@Tag(name = "Roles y permisos", description = "Roles del sistema y sus permisos granulares")
@RestController
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;
    private final PermisoService permisoService;

    @Operation(summary = "Lista los roles con su cantidad de usuarios y permisos")
    @GetMapping(ApiRutas.ROLES)
    @PreAuthorize("hasAuthority('" + Permisos.ROL_LEER + "')")
    public ResponseEntity<List<RolResponse>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @Operation(summary = "Obtiene un rol")
    @GetMapping(ApiRutas.ROLES + "/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ROL_LEER + "')")
    public ResponseEntity<RolResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtener(id));
    }

    @Operation(summary = "Crea un rol")
    @PostMapping(ApiRutas.ROLES)
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ESCRIBIR + "')")
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request));
    }

    @Operation(summary = "Actualiza un rol y sus permisos")
    @PutMapping(ApiRutas.ROLES + "/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ESCRIBIR + "')")
    public ResponseEntity<RolResponse> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody RolRequest request) {
        return ResponseEntity.ok(rolService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un rol que no sea del sistema y no tenga usuarios")
    @DeleteMapping(ApiRutas.ROLES + "/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ESCRIBIR + "')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Catalogo de permisos agrupado por modulo")
    @GetMapping(ApiRutas.PERMISOS)
    @PreAuthorize("hasAuthority('" + Permisos.ROL_LEER + "')")
    public ResponseEntity<Map<String, List<PermisoResponse>>> permisos() {
        return ResponseEntity.ok(permisoService.listarPorModulo());
    }
}
