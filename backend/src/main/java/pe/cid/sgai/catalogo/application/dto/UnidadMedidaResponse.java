package pe.cid.sgai.catalogo.application.dto;

import java.math.BigDecimal;

public record UnidadMedidaResponse(
        Long id, String codigo, String nombre, String simbolo,
        Long umBaseId, String umBaseCodigo, BigDecimal factorABase, boolean esBase, boolean activo
) {
}
