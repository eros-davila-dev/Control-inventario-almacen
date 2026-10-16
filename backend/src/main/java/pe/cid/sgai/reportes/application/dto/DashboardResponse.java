package pe.cid.sgai.reportes.application.dto;

import pe.cid.sgai.inventario.application.dto.ConsumoMensualDto;
import pe.cid.sgai.inventario.application.dto.RotacionProductoDto;
import pe.cid.sgai.inventario.application.dto.ValorizacionCategoriaDto;

import java.math.BigDecimal;
import java.util.List;

/** Indicadores del tablero de control del almacen (HU-43). */
public record DashboardResponse(
        BigDecimal valorInventario,
        long itemsConSaldo,
        long itemsBajoMinimo,
        long itemsAgotados,
        long lotesPorVencer30,
        long lotesPorVencer60,
        long lotesPorVencer90,
        long lotesVencidos,
        BigDecimal valorExpuestoVencimiento,
        long requisicionesPendientes,
        long ordenesPorRecibir,
        long transferenciasEnTransito,
        long tomasEnProceso,
        long movimientosHoy,
        long alertasCriticas,
        long desviacionesFefo30Dias,
        List<ValorizacionCategoriaDto> valorizacionPorCategoria,
        List<ConsumoMensualDto> consumoMensual,
        List<RotacionProductoDto> topRotacion
) {
}
