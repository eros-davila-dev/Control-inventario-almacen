package pe.cid.sgai.almacen.domain;

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

/** Almacen de una sede de hemodialisis o de la sede central (HU-12). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "almacen")
public class Almacen extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "sede", nullable = false, length = 120)
    private String sede;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_almacen", nullable = false, length = 20)
    private TipoAlmacen tipoAlmacen;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "telefono", length = 40)
    private String telefono;

    /** El almacen nunca permite saldo negativo salvo configuracion expresa. */
    @Builder.Default
    @Column(name = "permite_negativo", nullable = false)
    private boolean permiteNegativo = false;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public boolean esDespachable() {
        return activo && tipoAlmacen.esDespachable();
    }
}
