package pe.cid.sgai.despacho.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Documento de despacho generado al atender una requisicion (HU-33). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "despacho")
public class Despacho extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requisicion_id", nullable = false)
    private Requisicion requisicion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "movimiento_id")
    private Long movimientoId;

    @Column(name = "entregado_a", length = 150)
    private String entregadoA;

    @Column(name = "despachado_por", length = 100)
    private String despachadoPor;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @OneToMany(mappedBy = "despacho", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<DespachoLinea> lineas = new ArrayList<>();

    public void agregarLinea(DespachoLinea linea) {
        linea.setDespacho(this);
        this.lineas.add(linea);
    }

    public BigDecimal valorTotal() {
        return CantidadUtil.importe(lineas.stream()
                .map(l -> CantidadUtil.nvl(l.getCantidad()).multiply(CantidadUtil.nvl(l.getCostoUnitario())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
