package pe.cid.sgai.seguridad.web;

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
import pe.cid.sgai.seguridad.application.UsuarioService;
import pe.cid.sgai.seguridad.application.dto.PasswordTemporalResponse;
import pe.cid.sgai.seguridad.application.dto.UsuarioRequest;
import pe.cid.sgai.seguridad.application.dto.UsuarioResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

@Tag(name = "Usuarios", description = "Administracion de usuarios, roles asignados y alcance por almacen")
@RestController
@RequestMapping(ApiRutas.USUARIOS)
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Lista usuarios con filtros y paginacion")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_LEER + "')")
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "apellidos") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(texto, rolId, activo, pageable));
    }

    @Operation(summary = "Obtiene un usuario")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_LEER + "')")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @Operation(summary = "Crea un usuario y devuelve su contrasena temporal")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_ESCRIBIR + "')")
    public ResponseEntity<PasswordTemporalResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @Operation(summary = "Actualiza los datos, roles y almacenes de un usuario")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_ESCRIBIR + "')")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @Operation(summary = "Activa o desactiva un usuario")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_ESCRIBIR + "')")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id,
                                                         @RequestParam boolean activo) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, activo));
    }

    @Operation(summary = "Resetea la contrasena y obliga su cambio en el siguiente acceso")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_ESCRIBIR + "')")
    public ResponseEntity<PasswordTemporalResponse> resetear(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.resetearPassword(id));
    }

    @Operation(summary = "Desbloquea una cuenta bloqueada por intentos fallidos")
    @PostMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_ESCRIBIR + "')")
    public ResponseEntity<UsuarioResponse> desbloquear(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desbloquear(id));
    }
}
