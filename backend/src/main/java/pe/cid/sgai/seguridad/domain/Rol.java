package pe.cid.sgai.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

/** Rol funcional (ADMIN, JEFE_ALMACEN, ALMACENERO, QUIMICO_FARMACEUTICO, ...). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rol")
public class Rol extends BaseEntity {

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    /** Los roles del sistema no pueden eliminarse (HU-05). */
    @Builder.Default
    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new LinkedHashSet<>();

    public void reemplazarPermisos(Set<Permiso> nuevos) {
        this.permisos.clear();
        this.permisos.addAll(nuevos);
    }
}
