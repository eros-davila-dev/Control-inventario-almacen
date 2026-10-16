package pe.cid.sgai.inventario.domain;

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
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de un movimiento de inventario. Es el documento que respalda cada
 * variacion de saldo; nunca se elimina (la correccion se hace con un movimiento
 * inverso enlazado, HU-24).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movimiento")
public class Movimiento extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 35)
    private TipoMovimiento tipoMovimiento;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_movimiento", nullable = false, length = 20)
    private EstadoMovimiento estadoMovimiento = EstadoMovimiento.REGISTRADO;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_origen_id", nullable = false)
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "almacen_destino_id")
    private Almacen almacenDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_ajuste", length = 30)
    private MotivoAjuste motivoAjuste;

    @Column(name = "documento_tipo", length = 40)
    private String documentoTipo;

    @Column(name = "documento_numero", length = 60)
    private String documentoNumero;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "centro_costo", length = 40)
    private String centroCosto;

    @Column(name = "area_destino", length = 120)
    private String areaDestino;

    @Column(name = "observacion", length = 500)
    private String observacion;

    /** Movimiento que este documento anula (si es un movimiento inverso). */
    @Column(name = "movimiento_anulado_id")
    private Long movimientoAnuladoId;

    /** Documento de origen: RECEPCION, DESPACHO, TRANSFERENCIA, TOMA_INVENTARIO. */
    @Column(name = "referencia_tipo", length = 30)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "idempotency_key", unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Builder.Default
    @OneToMany(mappedBy = "movimiento", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<MovimientoLinea> lineas = new ArrayList<>();

    public void agregarLinea(MovimientoLinea linea) {
        linea.setMovimiento(this);
        this.lineas.add(linea);
    }

    public BigDecimal valorTotal() {
        return CantidadUtil.importe(lineas.stream()
                .map(MovimientoLinea::getCostoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public boolean estaAnulado() {
        return estadoMovimiento == EstadoMovimiento.ANULADO;
    }
}
