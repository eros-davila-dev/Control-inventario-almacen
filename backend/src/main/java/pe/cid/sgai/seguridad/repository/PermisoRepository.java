package pe.cid.sgai.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.seguridad.domain.Permiso;

import java.util.List;
import java.util.Set;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findAllByOrderByModuloAscCodigoAsc();

    Set<Permiso> findAllByCodigoIn(Set<String> codigos);
}
