package pe.cid.sgai.conteo.domain;

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
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "toma_inventario_linea")
public class TomaInventarioLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "toma_id", nullable = false)
    private TomaInventario toma;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ubicacion_id")
    private Ubicacion ubicacion;

    /** Saldo del sistema congelado al abrir la toma (no se muestra en conteo ciego). */
    @Builder.Default
    @Column(name = "cantidad_teorica", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadTeorica = BigDecimal.ZERO;

    @Column(name = "cantidad_contada", precision = 18, scale = 4)
    private BigDecimal cantidadContada;

    @Column(name = "segundo_conteo", precision = 18, scale = 4)
    private BigDecimal segundoConteo;

    @Column(name = "contado_por", length = 100)
    private String contadoPor;

    @Column(name = "fecha_conteo")
    private Instant fechaConteo;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public boolean fueContada() {
        return cantidadContada != null;
    }

    /** El segundo conteo, si existe, prevalece sobre el primero. */
    public BigDecimal cantidadFinal() {
        return segundoConteo != null ? segundoConteo : cantidadContada;
    }

    public BigDecimal diferencia() {
        if (!fueContada()) {
            return BigDecimal.ZERO;
        }
        return CantidadUtil.cantidad(cantidadFinal().subtract(CantidadUtil.nvl(cantidadTeorica)));
    }

    public boolean tieneDiferencia() {
        return diferencia().compareTo(BigDecimal.ZERO) != 0;
    }

    public BigDecimal valorDiferencia() {
        return CantidadUtil.importe(diferencia().multiply(
                CantidadUtil.nvl(producto.getCostoPromedio())));
    }
}
