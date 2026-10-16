package pe.cid.sgai.almacen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

/** Ubicacion fisica (zona / rack / nivel) dentro de un almacen (HU-13). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ubicacion", uniqueConstraints =
@UniqueConstraint(name = "uq_ubicacion_almacen_codigo", columnNames = {"almacen_id", "codigo"}))
public class Ubicacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Column(name = "zona", length = 30)
    private String zona;

    @Column(name = "rack", length = 30)
    private String rack;

    @Column(name = "nivel", length = 30)
    private String nivel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ubicacion", nullable = false, length = 20)
    private TipoUbicacion tipoUbicacion = TipoUbicacion.PICKING;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
