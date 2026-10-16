package pe.cid.sgai.almacen.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.almacen.domain.Ubicacion;

import java.util.List;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    List<Ubicacion> findAllByAlmacenIdAndActivoTrueOrderByCodigoAsc(Long almacenId);

    boolean existsByAlmacenIdAndCodigoIgnoreCase(Long almacenId, String codigo);

    boolean existsByAlmacenIdAndCodigoIgnoreCaseAndIdNot(Long almacenId, String codigo, Long id);

    @Query("""
            select u from Ubicacion u
            where (:almacenId is null or u.almacen.id = :almacenId)
              and (:texto is null or lower(u.codigo) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(coalesce(u.descripcion, '')) like lower(concat('%', cast(:texto as string), '%')))
            """)
    Page<Ubicacion> buscar(@Param("almacenId") Long almacenId, @Param("texto") String texto,
                           Pageable pageable);
}
