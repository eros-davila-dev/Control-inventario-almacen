package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.inventario.application.dto.SugerenciaLoteResponse;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fachada de la politica de asignacion de lotes. El modulo de despacho consulta
 * aqui la sugerencia FEFO y valida las desviaciones (HU-21, HU-33).
 */
@Service
@RequiredArgsConstructor
public class AsignacionLoteService {

    private final EstrategiaAsignacionLote estrategia;

    public String politica() {
        return estrategia.nombre();
    }

    @Transactional(readOnly = true)
    public List<EstrategiaAsignacionLote.Asignacion> asignar(Long productoId, Long almacenId,
                                                             BigDecimal cantidad) {
        return estrategia.asignar(productoId, almacenId, cantidad);
    }

    /** Sugerencia para la pantalla de picking: lotes, ubicaciones y cantidades. */
    @Transactional(readOnly = true)
    public List<SugerenciaLoteResponse> sugerir(Long productoId, Long almacenId, BigDecimal cantidad) {
        return asignar(productoId, almacenId, cantidad).stream()
                .map(asignacion -> {
                    Stock stock = asignacion.stock();
                    Lote lote = stock.getLote();
                    return new SugerenciaLoteResponse(
                            lote == null ? null : lote.getId(),
                            lote == null ? "SIN LOTE" : lote.getCodigoLote(),
                            lote == null ? null : lote.getFechaVencimiento(),
                            lote == null || lote.getFechaVencimiento() == null
                                    ? null : lote.diasParaVencer(),
                            stock.getUbicacion() == null ? null : stock.getUbicacion().getId(),
                            stock.getUbicacion() == null ? null : stock.getUbicacion().getCodigo(),
                            stock.disponible(),
                            CantidadUtil.cantidad(asignacion.cantidad()),
                            lote == null ? stock.getProducto().getCostoPromedio()
                                    : lote.getCostoUnitario());
                })
                .toList();
    }

    /**
     * Indica si el lote elegido manualmente es el que FEFO habria propuesto primero.
     * Si no lo es, el servicio de movimientos exige un motivo y lo deja auditado.
     */
    @Transactional(readOnly = true)
    public boolean esLoteFefo(Long productoId, Long almacenId, Long loteId) {
        if (loteId == null) {
            return true;
        }
        List<EstrategiaAsignacionLote.Asignacion> sugerencia =
                estrategia.asignar(productoId, almacenId, new BigDecimal("0.0001"));
        return sugerencia.isEmpty() || sugerencia.get(0).stock().getLote() == null
                || loteId.equals(sugerencia.get(0).stock().getLote().getId());
    }
}
