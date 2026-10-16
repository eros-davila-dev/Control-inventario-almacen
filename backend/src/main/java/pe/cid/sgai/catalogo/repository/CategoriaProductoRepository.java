package pe.cid.sgai.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.cid.sgai.catalogo.domain.CategoriaProducto;

import java.util.List;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<CategoriaProducto> findAllByActivoTrueOrderByNombreAsc();

    List<CategoriaProducto> findAllByCategoriaPadreId(Long categoriaPadreId);

    long countByCategoriaPadreId(Long categoriaPadreId);
}
