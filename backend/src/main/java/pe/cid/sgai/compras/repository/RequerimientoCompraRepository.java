package pe.cid.sgai.compras.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.compras.domain.EstadoRequerimiento;
import pe.cid.sgai.compras.domain.RequerimientoCompra;

public interface RequerimientoCompraRepository extends JpaRepository<RequerimientoCompra, Long> {

    @Query("""
            select r from RequerimientoCompra r
            where (:almacenId is null or r.almacen.id = :almacenId)
              and (:estado is null or r.estado = :estado)
            """)
    Page<RequerimientoCompra> buscar(@Param("almacenId") Long almacenId,
                                     @Param("estado") EstadoRequerimiento estado,
                                     Pageable pageable);

    long countByEstado(EstadoRequerimiento estado);
}
