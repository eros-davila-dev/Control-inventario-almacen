package pe.cid.sgai.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.application.dto.StockAgregadoDto;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.inventario.domain.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /** Busca la celda exacta de saldo (producto - almacen - lote - ubicacion). */
    @Query("""
            select s from Stock s
            where s.producto.id = :productoId
              and s.almacen.id = :almacenId
              and ((:loteId is null and s.lote is null) or s.lote.id = :loteId)
              and ((:ubicacionId is null and s.ubicacion is null) or s.ubicacion.id = :ubicacionId)
            """)
    Optional<Stock> buscarCelda(@Param("productoId") Long productoId,
                                @Param("almacenId") Long almacenId,
                                @Param("loteId") Long loteId,
                                @Param("ubicacionId") Long ubicacionId);

    @Query("""
            select s from Stock s
            where (:almacenId is null or s.almacen.id = :almacenId)
              and (:productoId is null or s.producto.id = :productoId)
              and (:categoriaId is null or s.producto.categoria.id = :categoriaId)
              and (:texto is null or lower(s.producto.nombre) like lower(concat('%', :texto, '%'))
                   or lower(s.producto.codigo) like lower(concat('%', :texto, '%')))
              and (:soloConSaldo = false or s.cantidad > 0)
            """)
    Page<Stock> buscar(@Param("almacenId") Long almacenId,
                       @Param("productoId") Long productoId,
                       @Param("categoriaId") Long categoriaId,
                       @Param("texto") String texto,
                       @Param("soloConSaldo") boolean soloConSaldo,
                       Pageable pageable);

    /**
     * Lotes candidatos para despacho ordenados por FEFO: primero el que vence antes
     * y, a igualdad, el de ingreso mas antiguo (HU-21).
     */
    @Query("""
            select s from Stock s
            where s.producto.id = :productoId
              and s.almacen.id = :almacenId
              and s.cantidad > s.cantidadReservada
              and (s.lote is null
                   or (s.lote.estadoLote = :liberado
                       and (s.lote.fechaVencimiento is null or s.lote.fechaVencimiento >= :hoy)))
            order by s.lote.fechaVencimiento asc nulls last, s.lote.id asc nulls last, s.id asc
            """)
    List<Stock> candidatosFefo(@Param("productoId") Long productoId,
                               @Param("almacenId") Long almacenId,
                               @Param("liberado") EstadoLote liberado,
                               @Param("hoy") LocalDate hoy);

    @Query("select coalesce(sum(s.cantidad), 0) from Stock s where s.producto.id = :productoId")
    BigDecimal saldoTotalProducto(@Param("productoId") Long productoId);

    @Query("""
            select coalesce(sum(s.cantidad), 0) from Stock s
            where s.producto.id = :productoId and s.almacen.id = :almacenId
            """)
    BigDecimal saldoProductoAlmacen(@Param("productoId") Long productoId,
                                    @Param("almacenId") Long almacenId);

    @Query("select s from Stock s where s.lote.id = :loteId and s.cantidad > 0")
    List<Stock> porLote(@Param("loteId") Long loteId);

    /** Saldo consolidado por producto y almacen, base de reposicion y valorizacion. */
    @Query("""
            select new pe.cid.sgai.inventario.application.dto.StockAgregadoDto(
                p.id, p.codigo, p.nombre, p.unidadMedida.codigo, a.id, a.nombre,
                sum(s.cantidad), sum(s.cantidadReservada),
                p.stockMinimo, p.puntoReorden, p.stockMaximo, p.costoPromedio, p.critico)
            from Stock s join s.producto p join s.almacen a
            where (:almacenId is null or a.id = :almacenId)
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:texto is null or lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(p.codigo) like lower(concat('%', :texto, '%')))
              and p.activo = true
            group by p.id, p.codigo, p.nombre, p.unidadMedida.codigo, a.id, a.nombre,
                     p.stockMinimo, p.puntoReorden, p.stockMaximo, p.costoPromedio, p.critico
            """)
    List<StockAgregadoDto> consolidado(@Param("almacenId") Long almacenId,
                                       @Param("categoriaId") Long categoriaId,
                                       @Param("texto") String texto);

    /** Valor total del inventario (opcionalmente de un almacen). */
    @Query("""
            select coalesce(sum(s.cantidad * s.producto.costoPromedio), 0)
            from Stock s where (:almacenId is null or s.almacen.id = :almacenId)
            """)
    BigDecimal valorInventario(@Param("almacenId") Long almacenId);

    @Query("""
            select new pe.cid.sgai.inventario.application.dto.ValorizacionCategoriaDto(
                p.categoria.id, p.categoria.nombre, count(distinct p.id),
                sum(s.cantidad), sum(s.cantidad * p.costoPromedio))
            from Stock s join s.producto p
            where (:almacenId is null or s.almacen.id = :almacenId) and s.cantidad > 0
            group by p.categoria.id, p.categoria.nombre
            order by sum(s.cantidad * p.costoPromedio) desc
            """)
    List<pe.cid.sgai.inventario.application.dto.ValorizacionCategoriaDto> valorizacionPorCategoria(
            @Param("almacenId") Long almacenId);

    /** Lotes con saldo y su tramo de vencimiento (HU-40). */
    @Query("""
            select new pe.cid.sgai.inventario.application.dto.LoteVencimientoDto(
                l.id, l.codigoLote, l.fechaVencimiento, l.estadoLote,
                p.id, p.codigo, p.nombre, a.id, a.nombre,
                sum(s.cantidad), sum(s.cantidad * p.costoPromedio))
            from Stock s join s.lote l join s.producto p join s.almacen a
            where s.cantidad > 0
              and l.fechaVencimiento is not null
              and l.fechaVencimiento <= :limite
              and (:almacenId is null or a.id = :almacenId)
            group by l.id, l.codigoLote, l.fechaVencimiento, l.estadoLote,
                     p.id, p.codigo, p.nombre, a.id, a.nombre
            order by l.fechaVencimiento asc
            """)
    List<pe.cid.sgai.inventario.application.dto.LoteVencimientoDto> vencimientosHasta(
            @Param("limite") LocalDate limite, @Param("almacenId") Long almacenId);

    @Query("select count(s) from Stock s where s.almacen.id = :almacenId and s.cantidad > 0")
    long contarItemsConSaldo(@Param("almacenId") Long almacenId);
}
