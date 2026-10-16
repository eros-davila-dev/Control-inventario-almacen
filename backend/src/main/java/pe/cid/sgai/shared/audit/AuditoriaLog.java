package pe.cid.sgai.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pista de auditoria inmutable: la aplicacion solo inserta (RNF-07).
 * No extiende BaseEntity porque no debe tener campos de actualizacion.
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auditoria_log")
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "accion", nullable = false, length = 80)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 80)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(name = "detalle", length = 4000)
    private String detalle;

    @Column(name = "ip", length = 60)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;
}
