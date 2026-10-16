package pe.cid.sgai.shared.util;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contador de numeracion de documentos por tipo y anio. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "correlativo", uniqueConstraints =
@UniqueConstraint(name = "uq_correlativo_tipo_anio", columnNames = {"tipo", "anio"}))
public class Correlativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "anio", nullable = false)
    private int anio;

    @Column(name = "ultimo", nullable = false)
    private long ultimo;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Correlativo(String tipo, int anio) {
        this.tipo = tipo;
        this.anio = anio;
        this.ultimo = 0L;
    }

    public long incrementar() {
        return ++ultimo;
    }
}
