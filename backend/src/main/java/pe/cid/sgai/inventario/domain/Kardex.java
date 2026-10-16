package pe.cid.sgai.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Linea de kardex valorizado (HU-25). Tabla de SOLO INSERCION: la correccion de un
 * error se hace con un movimiento inverso, nunca actualizando o borrando historia.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kardex")
public class Kardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "almacen_id", nullable = false)
    private Long almacenId;

    @Column(name = "movimiento_id", nullable = false)
    private Long movimientoId;

    @Column(name = "movimiento_linea_id", nullable = false)
    private Long movimientoLineaId;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 35)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "lote_id")
    private Long loteId;

    @Column(name = "codigo_lote", length = 60)
    private String codigoLote;

    @Builder.Default
    @Column(name = "entrada", nullable = false, precision = 18, scale = 4)
    private BigDecimal entrada = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "salida", nullable = false, precision = 18, scale = 4)
    private BigDecimal salida = BigDecimal.ZERO;

    @Column(name = "saldo", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldo;

    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "costo_promedio", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "valor_saldo", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorSaldo = BigDecimal.ZERO;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;
}
