package pe.cid.sgai.conteo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.conteo.domain.EstadoToma;
import pe.cid.sgai.conteo.domain.TomaInventario;

public interface TomaInventarioRepository extends JpaRepository<TomaInventario, Long> {

    @Query("""
            select t from TomaInventario t
            where (:almacenId is null or t.almacen.id = :almacenId)
              and (:estado is null or t.estado = :estado)
            """)
    Page<TomaInventario> buscar(@Param("almacenId") Long almacenId,
                                @Param("estado") EstadoToma estado,
                                Pageable pageable);

    long countByEstadoIn(java.util.List<EstadoToma> estados);
}
