package pe.cid.sgai.seguridad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.cid.sgai.seguridad.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Query("""
            select u from Usuario u
            where lower(u.username) = lower(:identificador)
               or lower(u.email) = lower(:identificador)
            """)
    Optional<Usuario> buscarPorUsernameOEmail(@Param("identificador") String identificador);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select distinct u from Usuario u left join u.roles r
            where (:texto is null
                   or lower(u.username) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(u.nombres) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(u.apellidos) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(u.email) like lower(concat('%', cast(:texto as string), '%')))
              and (:rolId is null or r.id = :rolId)
              and (:activo is null or u.activo = :activo)
            """)
    Page<Usuario> buscar(@Param("texto") String texto,
                         @Param("rolId") Long rolId,
                         @Param("activo") Boolean activo,
                         Pageable pageable);

    @Query("select count(u) from Usuario u join u.roles r where r.id = :rolId")
    long contarPorRol(@Param("rolId") Long rolId);
}
