package pe.cid.sgai.despacho.domain;

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
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "despacho_linea")
public class DespachoLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "despacho_id", nullable = false)
    private Despacho despacho;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requisicion_linea_id", nullable = false)
    private RequisicionLinea requisicionLinea;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    /** Deja constancia de si se respeto FEFO y, si no, por que (HU-21). */
    @Builder.Default
    @Column(name = "fefo_respetado", nullable = false)
    private boolean fefoRespetado = true;

    @Column(name = "motivo_no_fefo", length = 300)
    private String motivoNoFefo;
}
