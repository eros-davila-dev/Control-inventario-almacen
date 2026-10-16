package pe.cid.sgai.compras.web;

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
import pe.cid.sgai.compras.application.OrdenCompraService;
import pe.cid.sgai.compras.application.RecepcionService;
import pe.cid.sgai.compras.application.dto.OrdenCompraRequest;
import pe.cid.sgai.compras.application.dto.OrdenCompraResponse;
import pe.cid.sgai.compras.application.dto.RecepcionResponse;
import pe.cid.sgai.compras.domain.EstadoOrdenCompra;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

@Tag(name = "Ordenes de compra", description = "Emision, aprobacion y seguimiento de ordenes de compra")
@RestController
@RequestMapping(ApiRutas.ORDENES_COMPRA)
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService service;
    private final RecepcionService recepcionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_LEER + "')")
    public ResponseEntity<PageResponse<OrdenCompraResponse>> buscar(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) EstadoOrdenCompra estado,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20, sort = "fechaEmision") Pageable pageable) {
        return ResponseEntity.ok(service.buscar(almacenId, proveedorId, estado, texto, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_LEER + "')")
    public ResponseEntity<OrdenCompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Recepciones registradas contra la orden")
    @GetMapping("/{id}/recepciones")
    @PreAuthorize("hasAuthority('" + Permisos.RECEPCION_LEER + "')")
    public ResponseEntity<List<RecepcionResponse>> recepciones(@PathVariable Long id) {
        return ResponseEntity.ok(recepcionService.porOrden(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_CREAR + "')")
    public ResponseEntity<OrdenCompraResponse> crear(@Valid @RequestBody OrdenCompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Aprueba la orden: quien la registro no puede aprobarla")
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_APROBAR + "')")
    public ResponseEntity<OrdenCompraResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_APROBAR + "')")
    public ResponseEntity<OrdenCompraResponse> rechazar(@PathVariable Long id,
                                                        @RequestParam String motivo) {
        return ResponseEntity.ok(service.rechazar(id, motivo));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('" + Permisos.ORDEN_APROBAR + "')")
    public ResponseEntity<OrdenCompraResponse> anular(@PathVariable Long id,
                                                      @RequestParam String motivo) {
        return ResponseEntity.ok(service.anular(id, motivo));
    }
}
