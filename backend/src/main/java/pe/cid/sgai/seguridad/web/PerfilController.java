package pe.cid.sgai.seguridad.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.seguridad.application.AuthService;
import pe.cid.sgai.seguridad.application.dto.CambiarPasswordRequest;
import pe.cid.sgai.seguridad.application.dto.UsuarioResponse;
import pe.cid.sgai.shared.web.ApiRutas;

@Tag(name = "Perfil", description = "Datos y contrasena del usuario autenticado")
@RestController
@RequestMapping(ApiRutas.PERFIL)
@RequiredArgsConstructor
public class PerfilController {

    private final AuthService authService;

    @Operation(summary = "Perfil del usuario autenticado con sus roles y permisos")
    @GetMapping
    public ResponseEntity<UsuarioResponse> perfil() {
        return ResponseEntity.ok(authService.perfilActual());
    }

    @Operation(summary = "Cambia la contrasena del usuario autenticado")
    @PostMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(request);
        return ResponseEntity.noContent().build();
    }
}
