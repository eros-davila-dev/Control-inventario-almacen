package pe.cid.sgai.inventario.domain;

import java.math.BigDecimal;

/**
 * Evento de dominio publicado tras cada variacion de saldo. Desacopla los efectos
 * secundarios (evaluacion de alertas de reposicion) del registro del movimiento.
 */
public record StockActualizadoEvent(Long productoId, Long almacenId, BigDecimal saldoResultante) {
}
