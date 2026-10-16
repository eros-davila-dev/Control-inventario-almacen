package pe.cid.sgai.catalogo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas sanitarias del maestro de productos (HU-07, HU-31). */
class ProductoTest {

    private Producto producto(CondicionAlmacenamiento condicion, int vidaUtilMinima,
                              LocalDate venceRegistro) {
        return Producto.builder()
                .codigo("MED-EPO4").nombre("Eritropoyetina 4000 UI")
                .tipoProducto(TipoProducto.MEDICAMENTO)
                .condicionAlmacenamiento(condicion)
                .vidaUtilMinimaDias(vidaUtilMinima)
                .venceRegistroSanitario(venceRegistro)
                .controlaLote(true).controlaVencimiento(true)
                .costoPromedio(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("los refrigerados requieren cadena de frio")
    void cadenaDeFrio() {
        assertThat(producto(CondicionAlmacenamiento.REFRIGERADO_2_8, 180, null)
                .requiereCadenaFrio()).isTrue();
        assertThat(producto(CondicionAlmacenamiento.AMBIENTE, 180, null)
                .requiereCadenaFrio()).isFalse();
    }

    @Test
    @DisplayName("el registro sanitario vencido invalida la recepcion")
    void registroSanitario() {
        assertThat(producto(CondicionAlmacenamiento.AMBIENTE, 180, LocalDate.now().plusYears(1))
                .registroSanitarioVigente()).isTrue();
        assertThat(producto(CondicionAlmacenamiento.AMBIENTE, 180, LocalDate.now().minusDays(1))
                .registroSanitarioVigente()).isFalse();
        assertThat(producto(CondicionAlmacenamiento.AMBIENTE, 180, null)
                .registroSanitarioVigente()).isTrue();
    }

    @Test
    @DisplayName("la vida util remanente se compara contra el minimo exigido")
    void vidaUtilRemanente() {
        Producto p = producto(CondicionAlmacenamiento.AMBIENTE, 180, null);
        assertThat(p.vidaUtilSuficiente(LocalDate.now().plusDays(200))).isTrue();
        assertThat(p.vidaUtilSuficiente(LocalDate.now().plusDays(179))).isFalse();
    }

    @Test
    @DisplayName("los tipos sanitarios exigen trazabilidad por lote")
    void trazabilidadPorTipo() {
        assertThat(TipoProducto.MEDICAMENTO.exigeTrazabilidad()).isTrue();
        assertThat(TipoProducto.INSUMO_DIALISIS.exigeTrazabilidad()).isTrue();
        assertThat(TipoProducto.OFICINA.exigeTrazabilidad()).isFalse();
    }
}
