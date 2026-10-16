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
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orden_compra_linea")
public class OrdenCompraLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Builder.Default
    @Column(name = "cantidad_recibida", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadRecibida = BigDecimal.ZERO;

    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal precioUnitario;

    @Builder.Default
    @Column(name = "importe", nullable = false, precision = 18, scale = 2)
    private BigDecimal importe = BigDecimal.ZERO;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public void calcularImporte() {
        this.importe = CantidadUtil.importe(
                CantidadUtil.nvl(cantidad).multiply(CantidadUtil.nvl(precioUnitario)));
    }

    public BigDecimal pendiente() {
        return CantidadUtil.cantidad(
                CantidadUtil.nvl(cantidad).subtract(CantidadUtil.nvl(cantidadRecibida)));
    }

    public boolean estaCompleta() {
        return pendiente().compareTo(BigDecimal.ZERO) <= 0;
    }

    /** No se admite recibir mas de lo pedido: tolerancia cero (HU-30). */
    public void registrarRecepcion(BigDecimal cantidadRecibidaAhora) {
        if (cantidadRecibidaAhora.compareTo(pendiente()) > 0) {
            throw new NegocioException(CodigoError.CANTIDAD_EXCEDE_PENDIENTE,
                    "La cantidad recibida de %s supera el pendiente (%s)".formatted(
                            producto.getNombre(), pendiente().toPlainString()))
                    .con("productoId", producto.getId())
                    .con("pendiente", pendiente());
        }
        this.cantidadRecibida = CantidadUtil.cantidad(
                CantidadUtil.nvl(cantidadRecibida).add(cantidadRecibidaAhora));
    }
}
