package pe.cid.sgai.conteo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.catalogo.domain.Producto;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Diferencias y ERI de la toma de inventario (HU-38, indicador OE1). */
class TomaInventarioTest {

    private TomaInventarioLinea linea(String teorico, String contado) {
        return TomaInventarioLinea.builder()
                .producto(Producto.builder().codigo("P").nombre("Producto")
                        .costoPromedio(new BigDecimal("10")).build())
                .cantidadTeorica(new BigDecimal(teorico))
                .cantidadContada(contado == null ? null : new BigDecimal(contado))
                .build();
    }

    @Test
    @DisplayName("la diferencia y su valorizacion se calculan sobre el conteo final")
    void diferenciaYValor() {
        TomaInventarioLinea l = linea("100", "95");
        assertThat(l.diferencia()).isEqualByComparingTo("-5");
        assertThat(l.valorDiferencia()).isEqualByComparingTo("-50.00");
        assertThat(l.tieneDiferencia()).isTrue();
    }

    @Test
    @DisplayName("el segundo conteo prevalece sobre el primero")
    void segundoConteoPrevalece() {
        TomaInventarioLinea l = linea("100", "95");
        l.setSegundoConteo(new BigDecimal("100"));
        assertThat(l.cantidadFinal()).isEqualByComparingTo("100");
        assertThat(l.tieneDiferencia()).isFalse();
    }

    @Test
    @DisplayName("el ERI es el porcentaje de items sin diferencia")
    void calculoDelEri() {
        TomaInventario toma = TomaInventario.builder().numero("TOM-1").build();
        toma.agregarLinea(linea("100", "100"));
        toma.agregarLinea(linea("50", "50"));
        toma.agregarLinea(linea("20", "18"));
        toma.agregarLinea(linea("10", null));

        // 3 items contados, 2 exactos -> 66.67 %
        assertThat(toma.calcularEri()).isEqualByComparingTo("66.67");
    }

    @Test
    @DisplayName("sin conteos el ERI es cero")
    void eriSinConteos() {
        TomaInventario toma = TomaInventario.builder().numero("TOM-2").build();
        toma.agregarLinea(linea("100", null));
        assertThat(toma.calcularEri()).isEqualByComparingTo("0");
    }
}
