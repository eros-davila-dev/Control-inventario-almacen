package pe.cid.sgai.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.catalogo.domain.UnidadMedida;

import java.util.List;
import java.util.Optional;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    Optional<UnidadMedida> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<UnidadMedida> findAllByActivoTrueOrderByNombreAsc();

    long countByUmBaseId(Long umBaseId);
}
