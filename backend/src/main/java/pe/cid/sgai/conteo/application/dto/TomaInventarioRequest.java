package pe.cid.sgai.conteo.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.conteo.domain.AlcanceToma;

import java.time.LocalDate;

public record TomaInventarioRequest(
        @NotNull(message = "Seleccione el almacen") Long almacenId,
        @NotNull(message = "Indique el alcance de la toma") AlcanceToma alcance,
        Long categoriaId,
        ClasificacionAbc clasificacionAbc,
        LocalDate fechaProgramada,
        @Size(max = 500) String observacion
) {
}
