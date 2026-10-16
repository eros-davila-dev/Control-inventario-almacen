package pe.cid.sgai.seguridad.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.seguridad.application.AuthService;
import pe.cid.sgai.seguridad.application.dto.LoginRequest;
import pe.cid.sgai.seguridad.application.dto.RefreshRequest;
import pe.cid.sgai.seguridad.application.dto.TokenResponse;
import pe.cid.sgai.shared.web.ApiRutas;

@Tag(name = "Autenticacion", description = "Inicio de sesion, renovacion y cierre de sesion")
@RestController
@RequestMapping(ApiRutas.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(summary = "Inicia sesion y entrega access y refresh token")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @SecurityRequirements
    @Operation(summary = "Renueva el access token (sesion transitiva) rotando el refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refrescar(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refrescar(request.refreshToken()));
    }

    @Operation(summary = "Cierra la sesion y revoca el refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
