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
import pe.cid.sgai.shared.domain.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transferencia entre almacenes con confirmacion en destino (HU-22).
 * Mientras esta ENVIADA el stock no pertenece a ningun almacen operativo: esta en transito.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transferencia")
public class Transferencia extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_origen_id", nullable = false)
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false)
    private Almacen almacenDestino;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDate fechaEnvio;

    @Column(name = "fecha_recepcion")
    private LocalDate fechaRecepcion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTransferencia estado = EstadoTransferencia.ENVIADA;

    @Column(name = "movimiento_salida_id")
    private Long movimientoSalidaId;

    @Column(name = "movimiento_ingreso_id")
    private Long movimientoIngresoId;

    @Column(name = "transportista", length = 150)
    private String transportista;

    @Column(name = "guia_remision", length = 60)
    private String guiaRemision;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "enviado_por", length = 100)
    private String enviadoPor;

    @Column(name = "recibido_por", length = 100)
    private String recibidoPor;

    @Builder.Default
    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<TransferenciaLinea> lineas = new ArrayList<>();

    public void agregarLinea(TransferenciaLinea linea) {
        linea.setTransferencia(this);
        this.lineas.add(linea);
    }

    public boolean tieneDiferencias() {
        return lineas.stream().anyMatch(TransferenciaLinea::tieneDiferencia);
    }
}
