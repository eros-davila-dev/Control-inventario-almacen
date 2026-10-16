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
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Requerimiento de compra originado por el punto de reorden (HU-27). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requerimiento_compra")
public class RequerimientoCompra extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoRequerimiento estado = EstadoRequerimiento.BORRADOR;

    @Column(name = "justificacion", length = 500)
    private String justificacion;

    @Column(name = "solicitante_id")
    private Long solicitanteId;

    @Column(name = "aprobador", length = 100)
    private String aprobador;

    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Builder.Default
    @OneToMany(mappedBy = "requerimiento", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<RequerimientoCompraLinea> lineas = new ArrayList<>();

    public void agregarLinea(RequerimientoCompraLinea linea) {
        linea.setRequerimiento(this);
        this.lineas.add(linea);
    }

    public void cambiarEstado(EstadoRequerimiento destino) {
        if (!estado.puedeTransitarA(destino)) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "El requerimiento %s no puede pasar de %s a %s".formatted(numero, estado, destino));
        }
        this.estado = destino;
    }
}
