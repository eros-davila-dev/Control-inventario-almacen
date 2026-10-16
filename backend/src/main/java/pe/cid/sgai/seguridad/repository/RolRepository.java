package pe.cid.sgai.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.seguridad.domain.Rol;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Rol> findAllByActivoTrueOrderByNombreAsc();
}
