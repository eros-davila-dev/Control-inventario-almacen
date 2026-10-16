package pe.cid.sgai.catalogo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.catalogo.domain.Proveedor;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);

    List<Proveedor> findAllByActivoTrueOrderByRazonSocialAsc();

    @Query("""
            select p from Proveedor p
            where (:texto is null
                   or lower(p.razonSocial) like lower(concat('%', :texto, '%'))
                   or p.ruc like concat('%', :texto, '%')
                   or lower(coalesce(p.nombreComercial, '')) like lower(concat('%', :texto, '%')))
              and (:activo is null or p.activo = :activo)
            """)
    Page<Proveedor> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}
