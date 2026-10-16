package pe.cid.sgai.inventario.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** La anulacion debe generar siempre un movimiento de signo contrario (HU-24). */
class TipoMovimientoTest {

    @Test
    @DisplayName("el inverso de cada tipo tiene signo opuesto")
    void inversoCambiaElSigno() {
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            assertThat(tipo.inverso().signo())
                    .as("inverso de %s", tipo)
                    .isEqualTo(-tipo.signo());
        }
    }

    @Test
    @DisplayName("la transferencia se invierte como transferencia")
    void inversoDeTransferencia() {
        assertThat(TipoMovimiento.SALIDA_TRANSFERENCIA.inverso())
                .isEqualTo(TipoMovimiento.INGRESO_TRANSFERENCIA);
        assertThat(TipoMovimiento.INGRESO_TRANSFERENCIA.inverso())
                .isEqualTo(TipoMovimiento.SALIDA_TRANSFERENCIA);
    }
}
