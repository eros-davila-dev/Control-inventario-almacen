package pe.cid.sgai.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

/** Proveedor de insumos y medicamentos (HU-10). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "proveedor")
public class Proveedor extends BaseEntity {

    @Column(name = "ruc", nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "contacto", length = 120)
    private String contacto;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "condicion_pago", length = 60)
    private String condicionPago;

    @Builder.Default
    @Column(name = "lead_time_dias", nullable = false)
    private int leadTimeDias = 7;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
