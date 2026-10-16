package pe.cid.sgai.conteo.domain;

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
import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.shared.domain.BaseEntity;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Toma de inventario fisico (HU-36 a HU-38). Al abrirla se congela el saldo teorico
 * de cada item, de modo que la diferencia es comparable y auditable; el ERI resultante
 * es el indicador OE1 del proyecto.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "toma_inventario")
public class TomaInventario extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false, length = 20)
    private AlcanceToma alcance;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_abc", length = 1)
    private ClasificacionAbc clasificacionAbc;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoToma estado = EstadoToma.PROGRAMADA;

    @Column(name = "responsable", length = 100)
    private String responsable;

    @Column(name = "aprobador", length = 100)
    private String aprobador;

    /** Exactitud del registro de inventario (%) resultante de la toma. */
    @Column(name = "eri", precision = 5, scale = 2)
    private BigDecimal eri;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @OneToMany(mappedBy = "toma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TomaInventarioLinea> lineas = new ArrayList<>();

    public void agregarLinea(TomaInventarioLinea linea) {
        linea.setToma(this);
        this.lineas.add(linea);
    }

    public void cambiarEstado(EstadoToma destino) {
        if (!estado.puedeTransitarA(destino)) {
            throw new NegocioException(CodigoError.ESTADO_INVALIDO,
                    "La toma %s no puede pasar de %s a %s".formatted(numero, estado, destino));
        }
        this.estado = destino;
    }

    /** ERI = items sin diferencia / items contados x 100. */
    public BigDecimal calcularEri() {
        long contados = lineas.stream().filter(TomaInventarioLinea::fueContada).count();
        if (contados == 0) {
            return BigDecimal.ZERO;
        }
        long exactos = lineas.stream()
                .filter(TomaInventarioLinea::fueContada)
                .filter(l -> !l.tieneDiferencia())
                .count();
        return BigDecimal.valueOf(exactos * 100.0 / contados).setScale(2, RoundingMode.HALF_UP);
    }
}
