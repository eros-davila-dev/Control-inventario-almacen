package pe.cid.sgai.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.domain.Kardex;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KardexRepository extends JpaRepository<Kardex, Long> {

    /** Ultimo saldo registrado para la combinacion producto - almacen. */
    @Query("""
            select k from Kardex k
            where k.productoId = :productoId and k.almacenId = :almacenId
            order by k.id desc
            limit 1
            """)
    Optional<Kardex> ultimoSaldo(@Param("productoId") Long productoId,
                                 @Param("almacenId") Long almacenId);

    @Query("""
            select k from Kardex k
            where k.productoId = :productoId
              and (:almacenId is null or k.almacenId = :almacenId)
              and k.fecha >= :desde
              and k.fecha <= :hasta
            order by k.fecha asc, k.id asc
            """)
    Page<Kardex> kardexDe(@Param("productoId") Long productoId,
                          @Param("almacenId") Long almacenId,
                          @Param("desde") LocalDate desde,
                          @Param("hasta") LocalDate hasta,
                          Pageable pageable);

    @Query("""
            select k from Kardex k
            where k.productoId = :productoId
              and (:almacenId is null or k.almacenId = :almacenId)
              and k.fecha >= :desde
              and k.fecha <= :hasta
            order by k.fecha asc, k.id asc
            """)
    List<Kardex> kardexCompleto(@Param("productoId") Long productoId,
                                @Param("almacenId") Long almacenId,
                                @Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta);

    List<Kardex> findAllByMovimientoId(Long movimientoId);
}
