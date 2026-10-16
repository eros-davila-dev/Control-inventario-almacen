package pe.cid.sgai.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.time.Instant;

/**
 * Refresh token de sesion. Se guarda solo el hash SHA-256 del valor entregado al
 * cliente, de modo que una filtracion de la base no permite suplantar sesiones.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Builder.Default
    @Column(name = "revocado", nullable = false)
    private boolean revocado = false;

    @Column(name = "ip", length = 60)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    public boolean esVigente() {
        return !revocado && expiraEn.isAfter(Instant.now());
    }
}
