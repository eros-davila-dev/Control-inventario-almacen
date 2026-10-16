package pe.cid.sgai.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.application.dto.ConsumoMensualDto;
import pe.cid.sgai.inventario.application.dto.RotacionProductoDto;
import pe.cid.sgai.inventario.domain.MovimientoLinea;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoLineaRepository extends JpaRepository<MovimientoLinea, Long> {

    List<MovimientoLinea> findAllByMovimientoId(Long movimientoId);

    /** Trazabilidad hacia adelante de un lote: donde se consumio (HU-17, HU-45). */
    @Query("""
            select ml from MovimientoLinea ml
            join fetch ml.movimiento m
            where ml.lote.id = :loteId and m.estadoMovimiento = pe.cid.sgai.inventario.domain.EstadoMovimiento.REGISTRADO
            order by m.fecha desc, ml.id desc
            """)
    List<MovimientoLinea> historialDeLote(@Param("loteId") Long loteId);

    /** Consumo (salidas) de un producto en una ventana de dias, para cobertura y reposicion. */
    @Query("""
            select coalesce(sum(ml.cantidad), 0) from MovimientoLinea ml
            where ml.producto.id = :productoId
              and ml.movimiento.estadoMovimiento = pe.cid.sgai.inventario.domain.EstadoMovimiento.REGISTRADO
              and ml.movimiento.tipoMovimiento in (
                  pe.cid.sgai.inventario.domain.TipoMovimiento.SALIDA_DESPACHO,
                  pe.cid.sgai.inventario.domain.TipoMovimiento.SALIDA_MERMA)
              and (:almacenId is null or ml.movimiento.almacenOrigen.id = :almacenId)
              and ml.movimiento.fecha >= :desde
            """)
    BigDecimal consumoDesde(@Param("productoId") Long productoId,
                            @Param("almacenId") Long almacenId,
                            @Param("desde") LocalDate desde);

    @Query("""
            select new pe.cid.sgai.inventario.application.dto.ConsumoMensualDto(
                year(m.fecha), month(m.fecha), coalesce(m.areaDestino, 'SIN AREA'),
                coalesce(m.centroCosto, 'SIN CC'), sum(ml.cantidad), sum(ml.costoTotal))
            from MovimientoLinea ml join ml.movimiento m
            where m.estadoMovimiento = pe.cid.sgai.inventario.domain.EstadoMovimiento.REGISTRADO
              and m.tipoMovimiento = pe.cid.sgai.inventario.domain.TipoMovimiento.SALIDA_DESPACHO
              and m.fecha >= :desde
              and (:almacenId is null or m.almacenOrigen.id = :almacenId)
            group by year(m.fecha), month(m.fecha), m.areaDestino, m.centroCosto
            order by year(m.fecha), month(m.fecha)
            """)
    List<ConsumoMensualDto> consumoMensual(@Param("desde") LocalDate desde,
                                           @Param("almacenId") Long almacenId);

    @Query("""
            select new pe.cid.sgai.inventario.application.dto.RotacionProductoDto(
                p.id, p.codigo, p.nombre, sum(ml.cantidad), sum(ml.costoTotal), max(m.fecha))
            from MovimientoLinea ml join ml.movimiento m join ml.producto p
            where m.estadoMovimiento = pe.cid.sgai.inventario.domain.EstadoMovimiento.REGISTRADO
              and m.tipoMovimiento = pe.cid.sgai.inventario.domain.TipoMovimiento.SALIDA_DESPACHO
              and m.fecha >= :desde
              and (:almacenId is null or m.almacenOrigen.id = :almacenId)
            group by p.id, p.codigo, p.nombre
            order by sum(ml.cantidad) desc
            """)
    List<RotacionProductoDto> rotacion(@Param("desde") LocalDate desde,
                                       @Param("almacenId") Long almacenId);

    @Query("""
            select coalesce(max(m.fecha), null) from MovimientoLinea ml join ml.movimiento m
            where ml.producto.id = :productoId
              and (:almacenId is null or m.almacenOrigen.id = :almacenId)
            """)
    LocalDate ultimaFechaMovimiento(@Param("productoId") Long productoId,
                                    @Param("almacenId") Long almacenId);
}
