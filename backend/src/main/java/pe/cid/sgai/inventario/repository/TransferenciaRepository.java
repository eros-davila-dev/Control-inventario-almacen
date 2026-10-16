package pe.cid.sgai.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.domain.EstadoTransferencia;
import pe.cid.sgai.inventario.domain.Transferencia;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Query("""
            select t from Transferencia t
            where (:almacenId is null or t.almacenOrigen.id = :almacenId
                   or t.almacenDestino.id = :almacenId)
              and (:estado is null or t.estado = :estado)
            """)
    Page<Transferencia> buscar(@Param("almacenId") Long almacenId,
                               @Param("estado") EstadoTransferencia estado,
                               Pageable pageable);

    long countByEstado(EstadoTransferencia estado);
}
