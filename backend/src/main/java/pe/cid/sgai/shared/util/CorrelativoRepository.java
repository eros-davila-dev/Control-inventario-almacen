package pe.cid.sgai.shared.util;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CorrelativoRepository extends JpaRepository<Correlativo, Long> {

    /** Bloqueo pesimista: dos usuarios no pueden obtener el mismo numero de documento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Correlativo c where c.tipo = :tipo and c.anio = :anio")
    Optional<Correlativo> buscarParaActualizar(@Param("tipo") String tipo, @Param("anio") int anio);
}
