package pe.cid.sgai.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.TipoAlmacen;

import java.util.List;
import java.util.Optional;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {

    Optional<Almacen> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Almacen> findAllByActivoTrueOrderByNombreAsc();

    List<Almacen> findAllByTipoAlmacenAndActivoTrue(TipoAlmacen tipoAlmacen);

    Optional<Almacen> findFirstByTipoAlmacenAndSedeIgnoreCaseAndActivoTrue(TipoAlmacen tipo, String sede);
}
