package pe.cid.sgai.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.inventario.domain.Lote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Query("""
            select l from Lote l
            where l.producto.id = :productoId
              and lower(l.codigoLote) = lower(:codigoLote)
              and ((:proveedorId is null and l.proveedor is null) or l.proveedor.id = :proveedorId)
            """)
    Optional<Lote> buscarExistente(@Param("productoId") Long productoId,
                                   @Param("codigoLote") String codigoLote,
                                   @Param("proveedorId") Long proveedorId);

    @Query("""
            select l from Lote l
            where (:productoId is null or l.producto.id = :productoId)
              and (:texto is null or lower(l.codigoLote) like lower(concat('%', :texto, '%'))
                   or lower(l.producto.nombre) like lower(concat('%', :texto, '%')))
              and (:estado is null or l.estadoLote = :estado)
              and (:venceHasta is null or l.fechaVencimiento <= :venceHasta)
            """)
    Page<Lote> buscar(@Param("productoId") Long productoId,
                      @Param("texto") String texto,
                      @Param("estado") EstadoLote estado,
                      @Param("venceHasta") LocalDate venceHasta,
                      Pageable pageable);

    @Query("""
            select l from Lote l
            where l.fechaVencimiento is not null
              and l.fechaVencimiento < :hoy
              and l.estadoLote not in (:estadosFinales)
            """)
    List<Lote> porMarcarVencidos(@Param("hoy") LocalDate hoy,
                                 @Param("estadosFinales") List<EstadoLote> estadosFinales);

    List<Lote> findAllByProductoIdAndEstadoLoteOrderByFechaVencimientoAsc(Long productoId, EstadoLote estado);
}
