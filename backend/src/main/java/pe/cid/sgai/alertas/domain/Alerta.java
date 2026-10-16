package pe.cid.sgai.alertas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alerta operativa. Se mantiene una sola alerta abierta por combinacion
 * tipo + producto + almacen + lote, y se resuelve automaticamente cuando la
 * condicion que la origino desaparece (HU-39).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alerta")
public class Alerta extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alerta", nullable = false, length = 25)
    private TipoAlerta tipoAlerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, length = 10)
    private SeveridadAlerta severidad;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "producto_nombre", length = 200)
    private String productoNombre;

    @Column(name = "almacen_id")
    private Long almacenId;

    @Column(name = "almacen_nombre", length = 150)
    private String almacenNombre;

    @Column(name = "lote_id")
    private Long loteId;

    @Column(name = "codigo_lote", length = 60)
    private String codigoLote;

    @Column(name = "mensaje", nullable = false, length = 400)
    private String mensaje;

    @Column(name = "valor_referencia", precision = 18, scale = 4)
    private BigDecimal valorReferencia;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoAlerta estado = EstadoAlerta.ACTIVA;

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    public void resolver() {
        this.estado = EstadoAlerta.RESUELTA;
        this.fechaResolucion = Instant.now();
    }

    public void marcarLeida() {
        if (this.estado == EstadoAlerta.ACTIVA) {
            this.estado = EstadoAlerta.LEIDA;
        }
    }
}
