package pe.cid.sgai.inventario.domain;

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
import pe.cid.sgai.almacen.domain.Ubicacion;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;

/** Detalle de un movimiento: producto, lote, ubicacion, cantidad y costo. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movimiento_linea")
public class MovimientoLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_id", nullable = false)
    private Movimiento movimiento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ubicacion_id")
    private Ubicacion ubicacion;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    /** Cantidad convertida a la unidad base del producto (HU-09). */
    @Column(name = "cantidad_base", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadBase;

    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "costo_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal costoTotal = BigDecimal.ZERO;

    /** Trazabilidad de la politica FEFO: si no se respeto, se exige motivo (HU-21). */
    @Builder.Default
    @Column(name = "fefo_respetado", nullable = false)
    private boolean fefoRespetado = true;

    @Column(name = "motivo_no_fefo", length = 300)
    private String motivoNoFefo;

    public void calcularCostoTotal() {
        this.costoTotal = CantidadUtil.importe(
                CantidadUtil.nvl(cantidad).multiply(CantidadUtil.nvl(costoUnitario)));
    }
}
