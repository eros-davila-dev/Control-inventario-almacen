package pe.cid.sgai.inventario.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariantes del saldo: ningun camino de codigo puede dejar el stock negativo. */
class StockTest {

    private Stock stockDe(BigDecimal cantidad, Lote lote, boolean permiteNegativo) {
        Producto producto = Producto.builder().codigo("DIA-HF18")
                .nombre("Dializador de alto flujo 1.8 m2").costoPromedio(new BigDecimal("50"))
                .build();
        Almacen almacen = Almacen.builder().codigo("ALM-CEN").nombre("Almacen Central")
                .permiteNegativo(permiteNegativo).build();
        return Stock.builder().producto(producto).almacen(almacen).lote(lote)
                .cantidad(cantidad).cantidadReservada(BigDecimal.ZERO).build();
    }

    @Test
    @DisplayName("descontar rechaza la operacion cuando el saldo es insuficiente")
    void descontarSinSaldoSuficiente() {
        Stock stock = stockDe(new BigDecimal("10"), null, false);
        assertThatThrownBy(() -> stock.descontar(new BigDecimal("11")))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoError.STOCK_INSUFICIENTE);
        assertThat(stock.getCantidad()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("descontar deja el saldo exacto cuando se consume todo")
    void descontarTodo() {
        Stock stock = stockDe(new BigDecimal("10"), null, false);
        stock.descontar(new BigDecimal("10"));
        assertThat(stock.getCantidad()).isEqualByComparingTo("0");
        assertThat(stock.estaVacio()).isTrue();
    }

    @Test
    @DisplayName("un lote no liberado no aporta disponibilidad")
    void disponibleConLoteEnCuarentena() {
        Lote lote = Lote.builder().codigoLote("L-1").estadoLote(EstadoLote.CUARENTENA)
                .fechaVencimiento(LocalDate.now().plusYears(1)).build();
        Stock stock = stockDe(new BigDecimal("100"), lote, false);
        assertThat(stock.saldoFisico()).isEqualByComparingTo("100");
        assertThat(stock.disponible()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("lo reservado no se puede comprometer dos veces")
    void disponibleDescuentaReservas() {
        Stock stock = stockDe(new BigDecimal("100"), null, false);
        stock.reservar(new BigDecimal("30"));
        assertThat(stock.disponible()).isEqualByComparingTo("70");
        assertThatThrownBy(() -> stock.reservar(new BigDecimal("80")))
                .isInstanceOf(NegocioException.class);
        stock.liberarReserva(new BigDecimal("30"));
        assertThat(stock.disponible()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("un almacen configurado para negativos permite el descubierto")
    void permiteNegativoCuandoEstaConfigurado() {
        Stock stock = stockDe(new BigDecimal("5"), null, true);
        stock.descontar(new BigDecimal("8"));
        assertThat(stock.getCantidad()).isEqualByComparingTo("-3");
    }

    @Test
    @DisplayName("ingresar y retirar exigen cantidades positivas")
    void cantidadesPositivas() {
        Stock stock = stockDe(BigDecimal.ZERO, null, false);
        assertThatThrownBy(() -> stock.incrementar(BigDecimal.ZERO))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> stock.descontar(new BigDecimal("-1")))
                .isInstanceOf(NegocioException.class);
    }
}
