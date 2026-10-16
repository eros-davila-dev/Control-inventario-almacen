package pe.cid.sgai.seguridad.domain;

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

/** Bitacora de intentos de autenticacion (soporte de bloqueo y analisis forense). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "intento_acceso")
public class IntentoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 60)
    private String username;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(name = "motivo", length = 80)
    private String motivo;

    @Column(name = "ip", length = 60)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;
}
