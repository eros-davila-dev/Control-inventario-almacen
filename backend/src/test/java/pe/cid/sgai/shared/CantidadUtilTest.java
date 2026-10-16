package pe.cid.sgai.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Escalas de cantidades, costos e importes (RNF-08). */
class CantidadUtilTest {

    @Test
    @DisplayName("cada magnitud usa su propia escala")
    void escalas() {
        assertThat(CantidadUtil.cantidad(new BigDecimal("1.123456")).scale()).isEqualTo(4);
        assertThat(CantidadUtil.costo(new BigDecimal("1.1")).scale()).isEqualTo(6);
        assertThat(CantidadUtil.importe(new BigDecimal("1.129")).toPlainString()).isEqualTo("1.13");
    }

    @Test
    @DisplayName("los nulos se tratan como cero")
    void nulos() {
        assertThat(CantidadUtil.cantidad(null)).isEqualByComparingTo("0");
        assertThat(CantidadUtil.nvl(null)).isEqualByComparingTo("0");
        assertThat(CantidadUtil.esCero(null)).isTrue();
        assertThat(CantidadUtil.esPositiva(null)).isFalse();
    }

    @Test
    @DisplayName("la division por cero no rompe el calculo de cobertura")
    void divisionSegura() {
        assertThat(CantidadUtil.dividirSeguro(new BigDecimal("100"), BigDecimal.ZERO, 2))
                .isEqualByComparingTo("0");
        assertThat(CantidadUtil.dividirSeguro(new BigDecimal("100"), new BigDecimal("3"), 2))
                .isEqualByComparingTo("33.33");
    }
}
