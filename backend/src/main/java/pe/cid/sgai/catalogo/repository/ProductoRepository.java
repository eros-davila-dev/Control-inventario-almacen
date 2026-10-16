package pe.cid.sgai.catalogo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.catalogo.domain.TipoProducto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    long countByCategoriaId(Long categoriaId);

    long countByUnidadMedidaId(Long unidadMedidaId);

    /** Busqueda del catalogo por codigo, nombre, marca o categoria (HU-11). */
    @Query("""
            select p from Producto p
            where (:texto is null
                   or lower(p.codigo) like lower(concat('%', :texto, '%'))
                   or lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(p.marca, '')) like lower(concat('%', :texto, '%')))
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:tipo is null or p.tipoProducto = :tipo)
              and (:clasificacion is null or p.clasificacionAbc = :clasificacion)
              and (:critico is null or p.critico = :critico)
              and (:activo is null or p.activo = :activo)
            """)
    Page<Producto> buscar(@Param("texto") String texto,
                          @Param("categoriaId") Long categoriaId,
                          @Param("tipo") TipoProducto tipo,
                          @Param("clasificacion") ClasificacionAbc clasificacion,
                          @Param("critico") Boolean critico,
                          @Param("activo") Boolean activo,
                          Pageable pageable);

    List<Producto> findAllByActivoTrueOrderByNombreAsc();

    @Query("""
            select p from Producto p
            where p.activo = true and p.controlaVencimiento = true
              and p.venceRegistroSanitario is not null and p.venceRegistroSanitario < current_date
            """)
    List<Producto> conRegistroSanitarioVencido();
}
