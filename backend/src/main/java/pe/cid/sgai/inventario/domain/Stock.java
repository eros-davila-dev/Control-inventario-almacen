package pe.cid.sgai.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.Ubicacion;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;

/**
 * Saldo por producto - almacen - lote - ubicacion (HU-26). El bloqueo optimista
 * evita que dos despachos simultaneos sobrevendan el mismo lote (R4).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock")
public class Stock extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ubicacion_id")
    private Ubicacion ubicacion;

    @Builder.Default
    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cantidad_reservada", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadReservada = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private long version = 0L;

    /** Cantidad realmente comprometible: descuenta reservas y lotes no liberados. */
    public BigDecimal disponible() {
        if (lote != null && !lote.esDespachable()) {
            return BigDecimal.ZERO;
        }
        return CantidadUtil.cantidad(cantidad.subtract(cantidadReservada));
    }

    public BigDecimal saldoFisico() {
        return CantidadUtil.cantidad(cantidad);
    }

    public void incrementar(BigDecimal valor) {
        if (!CantidadUtil.esPositiva(valor)) {
            throw new NegocioException(CodigoError.VALIDACION, "La cantidad a ingresar debe ser mayor a cero");
        }
        this.cantidad = CantidadUtil.cantidad(this.cantidad.add(valor));
    }

    /**
     * Descuenta validando el saldo fisico. Es la invariante central del inventario:
     * ningun camino de codigo puede dejar el saldo en negativo.
     */
    public void descontar(BigDecimal valor) {
        if (!CantidadUtil.esPositiva(valor)) {
            throw new NegocioException(CodigoError.VALIDACION, "La cantidad a retirar debe ser mayor a cero");
        }
        BigDecimal resultante = this.cantidad.subtract(valor);
        if (resultante.compareTo(BigDecimal.ZERO) < 0 && !almacen.isPermiteNegativo()) {
            throw new NegocioException(CodigoError.STOCK_INSUFICIENTE,
                    "Stock insuficiente de %s en %s%s: disponible %s, solicitado %s".formatted(
                            producto.getNombre(), almacen.getNombre(),
                            lote == null ? "" : " lote " + lote.getCodigoLote(),
                            CantidadUtil.cantidad(this.cantidad).toPlainString(),
                            CantidadUtil.cantidad(valor).toPlainString()))
                    .con("productoId", producto.getId())
                    .con("almacenId", almacen.getId())
                    .con("disponible", CantidadUtil.cantidad(this.cantidad));
        }
        this.cantidad = CantidadUtil.cantidad(resultante);
    }

    public void reservar(BigDecimal valor) {
        BigDecimal nueva = CantidadUtil.nvl(cantidadReservada).add(CantidadUtil.nvl(valor));
        if (nueva.compareTo(cantidad) > 0) {
            throw new NegocioException(CodigoError.STOCK_INSUFICIENTE,
                    "No se puede reservar mas de lo disponible");
        }
        this.cantidadReservada = CantidadUtil.cantidad(nueva);
    }

    public void liberarReserva(BigDecimal valor) {
        BigDecimal nueva = CantidadUtil.nvl(cantidadReservada).subtract(CantidadUtil.nvl(valor));
        this.cantidadReservada = CantidadUtil.cantidad(
                nueva.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nueva);
    }

    public boolean estaVacio() {
        return CantidadUtil.esCero(cantidad) && CantidadUtil.esCero(cantidadReservada);
    }
}
