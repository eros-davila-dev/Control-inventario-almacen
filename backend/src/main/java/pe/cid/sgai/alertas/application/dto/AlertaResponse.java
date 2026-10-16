package pe.cid.sgai.alertas.application.dto;

import pe.cid.sgai.alertas.domain.EstadoAlerta;
import pe.cid.sgai.alertas.domain.SeveridadAlerta;
import pe.cid.sgai.alertas.domain.TipoAlerta;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertaResponse(
        Long id,
        TipoAlerta tipoAlerta,
        String tipoDescripcion,
        SeveridadAlerta severidad,
        EstadoAlerta estado,
        Long productoId,
        String productoNombre,
        Long almacenId,
        String almacenNombre,
        Long loteId,
        String codigoLote,
        String mensaje,
        BigDecimal valorReferencia,
        Instant fechaGeneracion,
        Instant fechaResolucion
) {
}
