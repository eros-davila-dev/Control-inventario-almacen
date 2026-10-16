package pe.cid.sgai.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.util.FechaUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lote del fabricante con su fecha de vencimiento (HU-15). Es la unidad de
 * trazabilidad sanitaria: todo movimiento de un producto con control de lote
 * referencia un lote.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lote", uniqueConstraints = @UniqueConstraint(
        name = "uq_lote_producto_codigo_proveedor",
        columnNames = {"producto_id", "codigo_lote", "proveedor_id"}))
public class Lote extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "codigo_lote", nullable = false, length = 60)
    private String codigoLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_lote", nullable = false, length = 20)
    private EstadoLote estadoLote = EstadoLote.CUARENTENA;

    @Column(name = "motivo_estado", length = 300)
    private String motivoEstado;

    @Column(name = "fecha_liberacion")
    private Instant fechaLiberacion;

    @Column(name = "liberado_por", length = 100)
    private String liberadoPor;

    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private long version = 0L;

    public boolean esDespachable() {
        return estadoLote.esDespachable() && !estaVencido();
    }

    public boolean estaVencido() {
        return FechaUtil.estaVencida(fechaVencimiento);
    }

    public long diasParaVencer() {
        return FechaUtil.diasHasta(fechaVencimiento);
    }

    /** Cambia el estado validando la maquina de estados y exigiendo motivo. */
    public void cambiarEstado(EstadoLote destino, String motivo, String usuario) {
        if (estadoLote == destino) {
            return;
        }
        if (!estadoLote.puedeTransitarA(destino)) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "No se puede pasar el lote de %s a %s".formatted(estadoLote, destino));
        }
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException(CodigoError.VALIDACION,
                    "Indique el motivo del cambio de estado del lote");
        }
        this.estadoLote = destino;
        this.motivoEstado = motivo;
        if (destino == EstadoLote.LIBERADO) {
            this.fechaLiberacion = Instant.now();
            this.liberadoPor = usuario;
        }
    }

    public void marcarVencido() {
        this.estadoLote = EstadoLote.VENCIDO;
        this.motivoEstado = "Vencimiento automatico al " + FechaUtil.hoy();
    }

    public String etiqueta() {
        return codigoLote + (fechaVencimiento == null ? "" : " (vence " + fechaVencimiento + ")");
    }
}
