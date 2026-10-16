package pe.cid.sgai.shared.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.RecursoNoEncontradoException;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

/**
 * Plantilla reutilizable para los maestros del sistema: evita repetir el mismo
 * CRUD en unidades de medida, categorias, proveedores, almacenes y ubicaciones.
 * Cada servicio concreto solo aporta sus validaciones y su mapeo.
 *
 * @param <E>   entidad
 * @param <REQ> DTO de entrada
 * @param <RES> DTO de salida
 */
public abstract class BaseCrudService<E extends BaseEntity, REQ, RES> {

    protected abstract JpaRepository<E, Long> repositorio();

    protected abstract String nombreEntidad();

    protected abstract E nuevaEntidad(REQ request);

    protected abstract void aplicarCambios(E entidad, REQ request);

    protected abstract RES aResponse(E entidad);

    /** Validaciones previas a la creacion (unicidad, referencias, reglas). */
    protected void validarCreacion(REQ request) {
        // por defecto sin validaciones adicionales
    }

    /** Validaciones previas a la actualizacion. */
    protected void validarActualizacion(E entidad, REQ request) {
        // por defecto sin validaciones adicionales
    }

    /** Validaciones previas a la baja (referencias en uso). */
    protected void validarEliminacion(E entidad) {
        // por defecto sin validaciones adicionales
    }

    @Transactional(readOnly = true)
    public E buscarEntidad(Long id) {
        return repositorio().findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(nombreEntidad(), id));
    }

    @Transactional(readOnly = true)
    public RES obtener(Long id) {
        return aResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<RES> listar(Pageable pageable) {
        Page<E> pagina = repositorio().findAll(pageable);
        return PageResponse.de(pagina, this::aResponse);
    }

    @Transactional(readOnly = true)
    public List<RES> listarTodo() {
        return repositorio().findAll().stream().map(this::aResponse).toList();
    }

    @Transactional
    public RES crear(REQ request) {
        validarCreacion(request);
        E entidad = nuevaEntidad(request);
        return aResponse(repositorio().save(entidad));
    }

    @Transactional
    public RES actualizar(Long id, REQ request) {
        E entidad = buscarEntidad(id);
        validarActualizacion(entidad, request);
        aplicarCambios(entidad, request);
        return aResponse(repositorio().save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        E entidad = buscarEntidad(id);
        validarEliminacion(entidad);
        repositorio().delete(entidad);
    }
}
