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
@Table(name = "requisicion_linea")
public class RequisicionLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicion_id", nullable = false)
    private Requisicion requisicion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_solicitada", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadSolicitada;

    @Builder.Default
    @Column(name = "cantidad_atendida", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadAtendida = BigDecimal.ZERO;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public BigDecimal pendiente() {
        return CantidadUtil.cantidad(
                CantidadUtil.nvl(cantidadSolicitada).subtract(CantidadUtil.nvl(cantidadAtendida)));
    }

    public boolean estaAtendida() {
        return pendiente().compareTo(BigDecimal.ZERO) <= 0;
    }

    public void registrarAtencion(BigDecimal cantidad) {
        if (cantidad.compareTo(pendiente()) > 0) {
            throw new NegocioException(CodigoError.CANTIDAD_EXCEDE_PENDIENTE,
                    "La cantidad a despachar de %s supera lo pendiente (%s)".formatted(
                            producto.getNombre(), pendiente().toPlainString()))
                    .con("productoId", producto.getId())
                    .con("pendiente", pendiente());
        }
        this.cantidadAtendida = CantidadUtil.cantidad(
                CantidadUtil.nvl(cantidadAtendida).add(cantidad));
    }
}
