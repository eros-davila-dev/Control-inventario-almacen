package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.util.FechaUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * FEFO: consume primero el lote que vence antes y, a igualdad de vencimiento,
 * el de ingreso mas antiguo. Es la politica que evita mermas por vencimiento en
 * insumos de hemodialisis (HU-21, objetivo OE2).
 */
@Component("fefo")
@RequiredArgsConstructor
public class EstrategiaFefo implements EstrategiaAsignacionLote {

    private final StockRepository stockRepository;

    @Override
    public String nombre() {
        return "FEFO";
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asignacion> asignar(Long productoId, Long almacenId, BigDecimal cantidadSolicitada) {
        BigDecimal pendiente = CantidadUtil.cantidad(cantidadSolicitada);
        if (!CantidadUtil.esPositiva(pendiente)) {
            throw new NegocioException(CodigoError.VALIDACION, "La cantidad debe ser mayor a cero");
        }
        List<Stock> candidatos = stockRepository.candidatosFefo(productoId, almacenId,
                EstadoLote.LIBERADO, FechaUtil.hoy());
        List<Asignacion> asignaciones = new ArrayList<>();
        BigDecimal disponibleTotal = BigDecimal.ZERO;

        for (Stock stock : candidatos) {
            BigDecimal disponible = stock.disponible();
            if (!CantidadUtil.esPositiva(disponible)) {
                continue;
            }
            disponibleTotal = disponibleTotal.add(disponible);
            BigDecimal aTomar = disponible.min(pendiente);
            asignaciones.add(new Asignacion(stock, CantidadUtil.cantidad(aTomar)));
            pendiente = pendiente.subtract(aTomar);
            if (!CantidadUtil.esPositiva(pendiente)) {
                break;
            }
        }

        if (CantidadUtil.esPositiva(pendiente)) {
            throw new NegocioException(CodigoError.STOCK_INSUFICIENTE,
                    "Stock disponible insuficiente: faltan %s unidades"
                            .formatted(CantidadUtil.cantidad(pendiente).toPlainString()))
                    .con("productoId", productoId)
                    .con("almacenId", almacenId)
                    .con("disponible", CantidadUtil.cantidad(disponibleTotal))
                    .con("solicitado", CantidadUtil.cantidad(cantidadSolicitada));
        }
        return asignaciones;
    }
}
