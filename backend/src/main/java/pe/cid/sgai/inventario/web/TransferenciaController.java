package pe.cid.sgai.inventario.web;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.TransferenciaService;
import pe.cid.sgai.inventario.application.dto.RecepcionTransferenciaRequest;
import pe.cid.sgai.inventario.application.dto.TransferenciaRequest;
import pe.cid.sgai.inventario.application.dto.TransferenciaResponse;
import pe.cid.sgai.inventario.domain.EstadoTransferencia;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

@Tag(name = "Transferencias", description = "Movimiento de stock entre almacenes con confirmacion en destino")
@RestController
@RequestMapping(ApiRutas.TRANSFERENCIAS)
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<PageResponse<TransferenciaResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) EstadoTransferencia estado,
            @PageableDefault(size = 20, sort = "fechaEnvio") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.STOCK_LEER + "')")
    public ResponseEntity<TransferenciaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Envia una transferencia: descuenta origen y deja el stock en transito")
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.TRANSFERENCIA_REGISTRAR + "')")
    public ResponseEntity<TransferenciaResponse> enviar(@Valid @RequestBody TransferenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(request));
    }

    @Operation(summary = "Confirma la recepcion en destino y registra las diferencias")
    @PostMapping("/{id}/recibir")
    @PreAuthorize("hasAuthority('" + Permisos.TRANSFERENCIA_RECIBIR + "')")
    public ResponseEntity<TransferenciaResponse> recibir(
            @PathVariable Long id, @Valid @RequestBody RecepcionTransferenciaRequest request) {
        return ResponseEntity.ok(service.recibir(id, request));
    }
}
