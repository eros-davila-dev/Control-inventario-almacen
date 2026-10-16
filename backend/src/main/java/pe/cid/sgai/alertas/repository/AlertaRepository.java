package pe.cid.sgai.alertas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.alertas.domain.Alerta;
import pe.cid.sgai.alertas.domain.EstadoAlerta;
import pe.cid.sgai.alertas.domain.SeveridadAlerta;
import pe.cid.sgai.alertas.domain.TipoAlerta;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    @Query("""
            select a from Alerta a
            where a.tipoAlerta = :tipo
              and a.estado <> pe.cid.sgai.alertas.domain.EstadoAlerta.RESUELTA
              and ((:productoId is null and a.productoId is null) or a.productoId = :productoId)
              and ((:almacenId is null and a.almacenId is null) or a.almacenId = :almacenId)
              and ((:loteId is null and a.loteId is null) or a.loteId = :loteId)
            """)
    Optional<Alerta> buscarAbierta(@Param("tipo") TipoAlerta tipo,
                                   @Param("productoId") Long productoId,
                                   @Param("almacenId") Long almacenId,
                                   @Param("loteId") Long loteId);

    @Query("""
            select a from Alerta a
            where (:tipo is null or a.tipoAlerta = :tipo)
              and (:severidad is null or a.severidad = :severidad)
              and (:estado is null or a.estado = :estado)
              and (:almacenId is null or a.almacenId = :almacenId)
            """)
    Page<Alerta> buscar(@Param("tipo") TipoAlerta tipo,
                        @Param("severidad") SeveridadAlerta severidad,
                        @Param("estado") EstadoAlerta estado,
                        @Param("almacenId") Long almacenId,
                        Pageable pageable);

    @Query("""
            select a.severidad, count(a) from Alerta a
            where a.estado <> pe.cid.sgai.alertas.domain.EstadoAlerta.RESUELTA
              and (:almacenId is null or a.almacenId = :almacenId)
            group by a.severidad
            """)
    List<Object[]> resumenPorSeveridad(@Param("almacenId") Long almacenId);

    long countByEstadoAndAlmacenId(EstadoAlerta estado, Long almacenId);

    long countByEstadoNot(EstadoAlerta estado);

    @Query("""
            select count(a) from Alerta a
            where a.tipoAlerta = :tipo and a.estado <> pe.cid.sgai.alertas.domain.EstadoAlerta.RESUELTA
              and (:almacenId is null or a.almacenId = :almacenId)
            """)
    long contarAbiertasPorTipo(@Param("tipo") TipoAlerta tipo, @Param("almacenId") Long almacenId);

    List<Alerta> findAllByTipoAlertaAndEstadoNot(TipoAlerta tipo, EstadoAlerta estado);
}
