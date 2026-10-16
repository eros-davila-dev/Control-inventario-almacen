package pe.cid.sgai.despacho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.despacho.domain.Despacho;

import java.time.LocalDate;
import java.util.List;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    List<Despacho> findAllByRequisicionIdOrderByFechaAsc(Long requisicionId);

    @Query("""
            select d from Despacho d
            where (:almacenId is null or d.almacen.id = :almacenId)
              and d.fecha >= :desde
              and d.fecha <= :hasta
            """)
    Page<Despacho> buscar(@Param("almacenId") Long almacenId,
                          @Param("desde") LocalDate desde,
                          @Param("hasta") LocalDate hasta,
                          Pageable pageable);

    @Query("""
            select count(dl) from DespachoLinea dl where dl.fefoRespetado = false
              and dl.despacho.fecha >= :desde
            """)
    long contarDesviacionesFefo(@Param("desde") LocalDate desde);
}
