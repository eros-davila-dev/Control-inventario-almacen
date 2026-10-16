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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Recepcion de mercaderia contra orden de compra con control de calidad (HU-30, HU-31). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recepcion")
public class Recepcion extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "guia_remision", length = 60)
    private String guiaRemision;

    @Column(name = "factura", length = 60)
    private String factura;

    @Column(name = "transportista", length = 150)
    private String transportista;

    /** Temperatura de llegada: obligatoria en productos de cadena de frio. */
    @Column(name = "temperatura_llegada", precision = 6, scale = 2)
    private BigDecimal temperaturaLlegada;

    @Builder.Default
    @Column(name = "empaque_integro", nullable = false)
    private boolean empaqueIntegro = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_calidad", nullable = false, length = 20)
    private ResultadoCalidad resultadoCalidad;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "movimiento_id")
    private Long movimientoId;

    @Column(name = "recibido_por", length = 100)
    private String recibidoPor;

    @Builder.Default
    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<RecepcionLinea> lineas = new ArrayList<>();

    public void agregarLinea(RecepcionLinea linea) {
        linea.setRecepcion(this);
        this.lineas.add(linea);
    }
}
