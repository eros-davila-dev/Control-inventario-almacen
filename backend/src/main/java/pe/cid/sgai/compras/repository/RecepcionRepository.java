package pe.cid.sgai.compras.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.compras.domain.Recepcion;

import java.util.List;

public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {

    List<Recepcion> findAllByOrdenIdOrderByFechaAsc(Long ordenId);

    @Query("""
            select r from Recepcion r
            where (:almacenId is null or r.almacen.id = :almacenId)
              and (:ordenId is null or r.orden.id = :ordenId)
            """)
    Page<Recepcion> buscar(@Param("almacenId") Long almacenId, @Param("ordenId") Long ordenId,
                           Pageable pageable);
}
