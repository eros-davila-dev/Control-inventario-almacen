package pe.cid.sgai.inventario.application;

import pe.cid.sgai.inventario.domain.Stock;

import java.math.BigDecimal;
import java.util.List;

/**
 * Politica de asignacion de lotes al despachar. Hoy se aplica FEFO
 * (First Expired, First Out), obligatoria para insumos y medicamentos; la
 * abstraccion permite incorporar FIFO o LIFO por producto sin tocar el
 * registro de movimientos (patron Strategy).
 */
public interface EstrategiaAsignacionLote {

    /** Nombre de la politica, usado en la trazabilidad del despacho. */
    String nombre();

    /**
     * Reparte la cantidad solicitada entre los lotes disponibles.
     *
     * @throws pe.cid.sgai.shared.exception.NegocioException si el saldo es insuficiente
     */
    List<Asignacion> asignar(Long productoId, Long almacenId, BigDecimal cantidadSolicitada);

    /** Porcion de la cantidad cubierta por una celda de stock concreta. */
    record Asignacion(Stock stock, BigDecimal cantidad) {
    }
}
