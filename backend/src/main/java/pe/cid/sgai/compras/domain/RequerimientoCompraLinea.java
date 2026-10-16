package pe.cid.sgai.compras.domain;

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
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requerimiento_compra_linea")
public class RequerimientoCompraLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requerimiento_id", nullable = false)
    private RequerimientoCompra requerimiento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_solicitada", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadSolicitada;

    /** Cantidad propuesta por el sistema segun stock maximo y consumo promedio. */
    @Builder.Default
    @Column(name = "cantidad_sugerida", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadSugerida = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "disponible_actual", nullable = false, precision = 18, scale = 4)
    private BigDecimal disponibleActual = BigDecimal.ZERO;

    @Column(name = "observacion", length = 300)
    private String observacion;
}
