package pe.cid.sgai.inventario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.inventario.domain.MotivoAjuste;
import pe.cid.sgai.inventario.domain.TipoMovimiento;

import java.time.LocalDate;
import java.util.List;

/** Salida de inventario con validacion de saldo y politica FEFO (HU-20, HU-21). */
public record SalidaRequest(
        @NotNull(message = "Seleccione el almacen") Long almacenId,
        @NotNull(message = "Indique el tipo de salida") TipoMovimiento tipoMovimiento,
        LocalDate fecha,
        @Size(max = 40) String centroCosto,
        @Size(max = 120) String areaDestino,
        MotivoAjuste motivoAjuste,
        @Size(max = 40) String documentoTipo,
        @Size(max = 60) String documentoNumero,
        @Size(max = 500) String observacion,
        @Size(max = 30) String referenciaTipo,
        Long referenciaId,
        @NotEmpty(message = "Agregue al menos una linea") @Valid List<LineaSalidaRequest> lineas
) {
}
