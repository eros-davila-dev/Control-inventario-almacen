package pe.cid.sgai.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.seguridad.domain.PasswordHistorico;

import java.util.List;

public interface PasswordHistoricoRepository extends JpaRepository<PasswordHistorico, Long> {

    List<PasswordHistorico> findTop5ByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
