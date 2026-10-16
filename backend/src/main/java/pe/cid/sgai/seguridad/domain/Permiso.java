package pe.cid.sgai.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

/**
 * Permiso granular con formato {@code modulo:recurso:accion}.
 * Es la autoridad que evalua Spring Security; el rol nunca se consulta directamente.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permiso")
public class Permiso extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;
}
