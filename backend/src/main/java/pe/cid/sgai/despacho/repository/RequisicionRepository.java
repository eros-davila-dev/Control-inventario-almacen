package pe.cid.sgai.despacho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.despacho.domain.EstadoRequisicion;
import pe.cid.sgai.despacho.domain.Requisicion;

import java.util.List;

public interface RequisicionRepository extends JpaRepository<Requisicion, Long> {

    @Query("""
            select r from Requisicion r
            where (:almacenAtencionId is null or r.almacenAtencion.id = :almacenAtencionId)
              and (:almacenSolicitanteId is null or r.almacenSolicitante.id = :almacenSolicitanteId)
              and (:estado is null or r.estado = :estado)
              and (:solicitanteId is null or r.solicitanteId = :solicitanteId)
            """)
    Page<Requisicion> buscar(@Param("almacenAtencionId") Long almacenAtencionId,
                             @Param("almacenSolicitanteId") Long almacenSolicitanteId,
                             @Param("estado") EstadoRequisicion estado,
                             @Param("solicitanteId") Long solicitanteId,
                             Pageable pageable);

    long countByEstadoIn(List<EstadoRequisicion> estados);
}
