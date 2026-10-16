package pe.cid.sgai.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    @Query("""
            select a from AuditoriaLog a
            where (:usuario is null or lower(a.usuario) like lower(concat('%', :usuario, '%')))
              and (:entidad is null or a.entidad = :entidad)
              and (:entidadId is null or a.entidadId = :entidadId)
              and (:accion is null or a.accion = :accion)
              and (:desde is null or a.fecha >= :desde)
              and (:hasta is null or a.fecha <= :hasta)
            """)
    Page<AuditoriaLog> buscar(@Param("usuario") String usuario,
                              @Param("entidad") String entidad,
                              @Param("entidadId") String entidadId,
                              @Param("accion") String accion,
                              @Param("desde") Instant desde,
                              @Param("hasta") Instant hasta,
                              Pageable pageable);

    List<AuditoriaLog> findTop200ByEntidadAndEntidadIdOrderByFechaDesc(String entidad, String entidadId);
}
