package pe.cid.sgai.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.seguridad.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revocado = true where t.usuario.id = :usuarioId and t.revocado = false")
    int revocarTodosDe(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiraEn < :limite")
    int eliminarExpirados(@Param("limite") Instant limite);
}
