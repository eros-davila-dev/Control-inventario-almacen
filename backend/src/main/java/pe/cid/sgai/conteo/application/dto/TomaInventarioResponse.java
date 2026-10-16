package pe.cid.sgai.conteo.application.dto;

import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.conteo.domain.AlcanceToma;
import pe.cid.sgai.conteo.domain.EstadoToma;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TomaInventarioResponse(
        Long id,
        String numero,
        Long almacenId,
        String almacenNombre,
        AlcanceToma alcance,
        String alcanceDescripcion,
        Long categoriaId,
        ClasificacionAbc clasificacionAbc,
        LocalDate fechaProgramada,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        EstadoToma estado,
        String estadoDescripcion,
        String responsable,
        String aprobador,
        BigDecimal eri,
        String observacion,
        int itemsTotales,
        int itemsContados,
        int itemsConDiferencia,
        BigDecimal valorDiferencias,
        List<LineaTomaResponse> lineas
) {

    /**
     * En conteo ciego la cantidad teorica se devuelve en {@code null} hasta que la
     * toma pasa a CONTADA (HU-37).
     */
    public record LineaTomaResponse(
            Long id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            String unidadMedida,
            Long loteId,
            String codigoLote,
            LocalDate fechaVencimiento,
            Long ubicacionId,
            String ubicacionCodigo,
            BigDecimal cantidadTeorica,
            BigDecimal cantidadContada,
            BigDecimal segundoConteo,
            BigDecimal diferencia,
            BigDecimal valorDiferencia,
            String contadoPor,
            String observacion
    ) {
    }
}
