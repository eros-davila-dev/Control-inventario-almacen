package pe.cid.sgai.compras.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.compras.domain.EstadoOrdenCompra;
import pe.cid.sgai.compras.domain.OrdenCompra;

import java.util.List;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("""
            select o from OrdenCompra o
            where (:almacenId is null or o.almacenDestino.id = :almacenId)
              and (:proveedorId is null or o.proveedor.id = :proveedorId)
              and (:estado is null or o.estado = :estado)
              and (:texto is null or lower(o.numero) like lower(concat('%', cast(:texto as string), '%')))
            """)
    Page<OrdenCompra> buscar(@Param("almacenId") Long almacenId,
                             @Param("proveedorId") Long proveedorId,
                             @Param("estado") EstadoOrdenCompra estado,
                             @Param("texto") String texto,
                             Pageable pageable);

    List<OrdenCompra> findAllByEstadoIn(List<EstadoOrdenCompra> estados);

    long countByEstadoIn(List<EstadoOrdenCompra> estados);

    long countByProveedorId(Long proveedorId);
}
