package pe.cid.sgai.despacho.domain;

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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Requisicion interna de un area asistencial o sede (HU-32). Es el documento con el
 * que enfermeria pide insumos al almacen y permite medir la oportunidad de atencion.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requisicion")
public class Requisicion extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_solicitante_id", nullable = false)
    private Almacen almacenSolicitante;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_atencion_id", nullable = false)
    private Almacen almacenAtencion;

    @Column(name = "centro_costo", nullable = false, length = 40)
    private String centroCosto;

    @Column(name = "area", nullable = false, length = 120)
    private String area;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private Prioridad prioridad = Prioridad.NORMAL;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_requerida")
    private LocalDate fechaRequerida;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoRequisicion estado = EstadoRequisicion.BORRADOR;

    @Column(name = "solicitante_id")
    private Long solicitanteId;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @OneToMany(mappedBy = "requisicion", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<RequisicionLinea> lineas = new ArrayList<>();

    public void agregarLinea(RequisicionLinea linea) {
        linea.setRequisicion(this);
        this.lineas.add(linea);
    }

    public void cambiarEstado(EstadoRequisicion destino) {
        if (!estado.puedeTransitarA(destino)) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La requisicion %s no puede pasar de %s a %s".formatted(numero, estado, destino));
        }
        this.estado = destino;
    }

    /** Ajusta el estado segun lo atendido en cada linea (HU-33). */
    public void actualizarEstadoPorAtencion() {
        boolean completa = lineas.stream().allMatch(RequisicionLinea::estaAtendida);
        this.estado = completa ? EstadoRequisicion.ATENDIDA : EstadoRequisicion.ATENDIDA_PARCIAL;
    }
}
