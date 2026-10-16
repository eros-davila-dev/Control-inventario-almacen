package pe.cid.sgai.inventario.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cid.sgai.inventario.application.KardexService;
import pe.cid.sgai.inventario.application.dto.KardexResponse;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.shared.web.ApiRutas;
import pe.cid.sgai.shared.web.PageResponse;

import java.time.LocalDate;

@Tag(name = "Kardex", description = "Kardex valorizado por costo promedio ponderado")
@RestController
@RequestMapping(ApiRutas.KARDEX)
@RequiredArgsConstructor
public class KardexController {

    private final KardexService service;

    @Operation(summary = "Kardex de un producto por almacen y rango de fechas")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.KARDEX_LEER + "')")
    public ResponseEntity<PageResponse<KardexResponse>> consultar(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(service.consultar(productoId, almacenId, desde, hasta, pageable));
    }
}
