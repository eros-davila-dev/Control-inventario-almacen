package pe.cid.sgai.inventario.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.inventario.application.dto.KardexResponse;
import pe.cid.sgai.inventario.application.mapper.InventarioMapper;
import pe.cid.sgai.inventario.domain.Kardex;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.MovimientoLinea;
import pe.cid.sgai.inventario.repository.KardexRepository;
import pe.cid.sgai.shared.util.CantidadUtil;
import pe.cid.sgai.shared.web.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Kardex valorizado de solo insercion (HU-25). Cada linea guarda el saldo y el
 * costo promedio vigentes en el momento del movimiento, por lo que el historico
 * es reconstruible y a prueba de manipulacion (RNF-07).
 */
@Service
@RequiredArgsConstructor
public class KardexService {

    private final KardexRepository repository;
    private final InventarioMapper mapper;

    /**
     * Registra el asiento de kardex de una linea de movimiento.
     *
     * @param almacenId     almacen afectado (origen en salidas, destino en ingresos)
     * @param costoPromedio costo promedio ponderado vigente tras el movimiento
     */
    @Transactional
    public Kardex registrar(Movimiento movimiento, MovimientoLinea linea, Long almacenId,
                            boolean esEntrada, BigDecimal costoPromedio) {
        BigDecimal cantidad = CantidadUtil.cantidad(linea.getCantidad());
        BigDecimal saldoAnterior = repository.ultimoSaldo(linea.getProducto().getId(), almacenId)
                .map(Kardex::getSaldo)
                .orElse(BigDecimal.ZERO);
        BigDecimal saldo = CantidadUtil.cantidad(esEntrada
                ? saldoAnterior.add(cantidad) : saldoAnterior.subtract(cantidad));
        BigDecimal promedio = CantidadUtil.costo(costoPromedio);

        return repository.save(Kardex.builder()
                .productoId(linea.getProducto().getId())
                .almacenId(almacenId)
                .movimientoId(movimiento.getId())
                .movimientoLineaId(linea.getId())
                .numeroDocumento(movimiento.getNumero())
                .fecha(movimiento.getFecha())
                .tipoMovimiento(movimiento.getTipoMovimiento())
                .loteId(linea.getLote() == null ? null : linea.getLote().getId())
                .codigoLote(linea.getLote() == null ? null : linea.getLote().getCodigoLote())
                .entrada(esEntrada ? cantidad : BigDecimal.ZERO)
                .salida(esEntrada ? BigDecimal.ZERO : cantidad)
                .saldo(saldo)
                .costoUnitario(CantidadUtil.costo(linea.getCostoUnitario()))
                .costoPromedio(promedio)
                .valorSaldo(CantidadUtil.importe(saldo.multiply(promedio)))
                .usuario(movimiento.getCreatedBy())
                .registradoEn(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<KardexResponse> consultar(Long productoId, Long almacenId, LocalDate desde,
                                                  LocalDate hasta, Pageable pageable) {
        return PageResponse.de(repository.kardexDe(productoId, almacenId, desde, hasta, pageable),
                mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public List<Kardex> completo(Long productoId, Long almacenId, LocalDate desde, LocalDate hasta) {
        return repository.kardexCompleto(productoId, almacenId, desde, hasta);
    }
}
