package pe.cid.sgai.seguridad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.seguridad.domain.IntentoAcceso;

import java.time.Instant;

public interface IntentoAccesoRepository extends JpaRepository<IntentoAcceso, Long> {

    @Query("""
            select count(i) from IntentoAcceso i
            where lower(i.username) = lower(:username) and i.exitoso = false and i.fecha >= :desde
            """)
    long contarFallidosDesde(@Param("username") String username, @Param("desde") Instant desde);

    @Query("select count(i) from IntentoAcceso i where i.ip = :ip and i.fecha >= :desde")
    long contarPorIpDesde(@Param("ip") String ip, @Param("desde") Instant desde);

    Page<IntentoAcceso> findAllByOrderByFechaDesc(Pageable pageable);
}
