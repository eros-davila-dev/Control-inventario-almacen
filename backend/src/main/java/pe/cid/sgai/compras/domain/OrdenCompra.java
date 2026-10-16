package pe.cid.sgai.compras.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Orden de compra al proveedor (HU-28, HU-29). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orden_compra")
public class OrdenCompra extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requerimiento_id")
    private RequerimientoCompra requerimiento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false)
    private Almacen almacenDestino;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_esperada")
    private LocalDate fechaEsperada;

    @Builder.Default
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "condicion_pago", length = 60)
    private String condicionPago;

    @Builder.Default
    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "igv", nullable = false, precision = 18, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoOrdenCompra estado = EstadoOrdenCompra.BORRADOR;

    @Column(name = "aprobador", length = 100)
    private String aprobador;

    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<OrdenCompraLinea> lineas = new ArrayList<>();

    public void agregarLinea(OrdenCompraLinea linea) {
        linea.setOrden(this);
        this.lineas.add(linea);
    }

    /** Totales calculados siempre en el servidor, nunca enviados por el cliente. */
    public void calcularTotales(BigDecimal tasaIgv) {
        this.subtotal = CantidadUtil.importe(lineas.stream()
                .map(OrdenCompraLinea::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        this.igv = CantidadUtil.importe(subtotal.multiply(tasaIgv));
        this.total = CantidadUtil.importe(subtotal.add(igv));
    }

    public void cambiarEstado(EstadoOrdenCompra destino) {
        if (!estado.puedeTransitarA(destino)) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La orden %s no puede pasar de %s a %s".formatted(numero, estado, destino));
        }
        this.estado = destino;
    }

    /** Recalcula el estado segun lo recibido en cada linea. */
    public void actualizarEstadoPorRecepcion() {
        boolean completa = lineas.stream().allMatch(OrdenCompraLinea::estaCompleta);
        boolean alguna = lineas.stream().anyMatch(l -> CantidadUtil.esPositiva(l.getCantidadRecibida()));
        if (completa) {
            this.estado = EstadoOrdenCompra.RECIBIDA;
        } else if (alguna) {
            this.estado = EstadoOrdenCompra.RECIBIDA_PARCIAL;
        }
    }
}
