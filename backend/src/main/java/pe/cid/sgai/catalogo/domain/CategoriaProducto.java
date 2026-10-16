package pe.cid.sgai.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

/** Categoria jerarquica del catalogo (HU-08). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categoria_producto")
public class CategoriaProducto extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    private CategoriaProducto categoriaPadre;

    @Builder.Default
    @Column(name = "requiere_cadena_frio", nullable = false)
    private boolean requiereCadenaFrio = false;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
