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
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transferencia_linea")
public class TransferenciaLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private Transferencia transferencia;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(name = "cantidad_enviada", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadEnviada;

    @Builder.Default
    @Column(name = "cantidad_recibida", precision = 18, scale = 4)
    private BigDecimal cantidadRecibida = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public BigDecimal diferencia() {
        return CantidadUtil.cantidad(
                CantidadUtil.nvl(cantidadEnviada).subtract(CantidadUtil.nvl(cantidadRecibida)));
    }

    public boolean tieneDiferencia() {
        return diferencia().compareTo(BigDecimal.ZERO) != 0;
    }
}
