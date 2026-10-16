package pe.cid.sgai.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.cid.sgai.shared.util.FechaUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Insumo, medicamento o material del almacen (HU-07). Incluye los datos que exige
 * el control sanitario (registro sanitario, condicion de almacenamiento, vida util
 * minima en recepcion) y los parametros de reposicion.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "producto")
public class Producto extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private UnidadMedida unidadMedida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 30)
    private TipoProducto tipoProducto;

    @Column(name = "presentacion", length = 120)
    private String presentacion;

    @Column(name = "marca", length = 120)
    private String marca;

    @Column(name = "registro_sanitario", length = 60)
    private String registroSanitario;

    @Column(name = "vence_registro_sanitario")
    private LocalDate venceRegistroSanitario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_almacenamiento", nullable = false, length = 30)
    private CondicionAlmacenamiento condicionAlmacenamiento = CondicionAlmacenamiento.AMBIENTE;

    @Builder.Default
    @Column(name = "controla_lote", nullable = false)
    private boolean controlaLote = true;

    @Builder.Default
    @Column(name = "controla_vencimiento", nullable = false)
    private boolean controlaVencimiento = true;

    /** Vida util remanente minima aceptada al recepcionar (HU-31). */
    @Builder.Default
    @Column(name = "vida_util_minima_dias", nullable = false)
    private int vidaUtilMinimaDias = 180;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_abc", nullable = false, length = 1)
    private ClasificacionAbc clasificacionAbc = ClasificacionAbc.C;

    @Builder.Default
    @Column(name = "stock_minimo", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "punto_reorden", nullable = false, precision = 18, scale = 4)
    private BigDecimal puntoReorden = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_maximo", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockMaximo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "dias_cobertura", nullable = false)
    private int diasCobertura = 30;

    /** Costo promedio ponderado vigente, recalculado en cada ingreso. */
    @Builder.Default
    @Column(name = "costo_promedio", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    /** Marca los insumos cuyo quiebre suspende sesiones de hemodialisis. */
    @Builder.Default
    @Column(name = "critico", nullable = false)
    private boolean critico = false;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public boolean requiereCadenaFrio() {
        return condicionAlmacenamiento.requiereCadenaFrio();
    }

    public boolean registroSanitarioVigente() {
        return venceRegistroSanitario == null || !FechaUtil.estaVencida(venceRegistroSanitario);
    }

    /** Vida util remanente suficiente para aceptar el lote en recepcion. */
    public boolean vidaUtilSuficiente(LocalDate fechaVencimiento) {
        if (!controlaVencimiento || fechaVencimiento == null) {
            return true;
        }
        return FechaUtil.diasHasta(fechaVencimiento) >= vidaUtilMinimaDias;
    }
}
