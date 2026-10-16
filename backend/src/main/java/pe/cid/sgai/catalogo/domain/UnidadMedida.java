package pe.cid.sgai.catalogo.domain;

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

import java.math.BigDecimal;

/**
 * Unidad de medida con conversion a la unidad base (HU-09).
 * Ejemplo: CAJA x 20 UNIDAD -> umBase = UNIDAD, factorABase = 20.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "unidad_medida")
public class UnidadMedida extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "simbolo", length = 10)
    private String simbolo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "um_base_id")
    private UnidadMedida umBase;

    @Builder.Default
    @Column(name = "factor_a_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal factorABase = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public boolean esBase() {
        return umBase == null;
    }

    /** Convierte una cantidad expresada en esta unidad a la unidad base. */
    public BigDecimal aCantidadBase(BigDecimal cantidad) {
        return cantidad == null ? BigDecimal.ZERO : cantidad.multiply(factorABase);
    }
}
