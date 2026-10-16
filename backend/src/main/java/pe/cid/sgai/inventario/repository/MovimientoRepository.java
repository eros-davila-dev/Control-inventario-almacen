package pe.cid.sgai.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.domain.EstadoMovimiento;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.TipoMovimiento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    Optional<Movimiento> findByIdempotencyKey(String idempotencyKey);

    Optional<Movimiento> findByNumero(String numero);

    boolean existsByMovimientoAnuladoId(Long movimientoAnuladoId);

    List<Movimiento> findAllByReferenciaTipoAndReferenciaId(String referenciaTipo, Long referenciaId);

    @Query("""
            select distinct m from Movimiento m left join fetch m.lineas
            where (:almacenId is null or m.almacenOrigen.id = :almacenId or m.almacenDestino.id = :almacenId)
              and (:tipo is null or m.tipoMovimiento = :tipo)
              and (:estado is null or m.estadoMovimiento = :estado)
              and m.fecha >= :desde
              and m.fecha <= :hasta
              and (:texto is null or lower(m.numero) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(coalesce(m.documentoNumero, '')) like lower(concat('%', cast(:texto as string), '%')))
            """)
    Page<Movimiento> buscar(@Param("almacenId") Long almacenId,
                            @Param("tipo") TipoMovimiento tipo,
                            @Param("estado") EstadoMovimiento estado,
                            @Param("desde") LocalDate desde,
                            @Param("hasta") LocalDate hasta,
                            @Param("texto") String texto,
                            Pageable pageable);

    @Query("""
            select count(m) from Movimiento m
            where m.fecha = :fecha and m.estadoMovimiento = :estado
              and (:almacenId is null or m.almacenOrigen.id = :almacenId)
            """)
    long contarPorFecha(@Param("fecha") LocalDate fecha,
                        @Param("estado") EstadoMovimiento estado,
                        @Param("almacenId") Long almacenId);
}
