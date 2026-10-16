package pe.cid.sgai.inventario.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.inventario.domain.EstadoLote;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.repository.StockRepository;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** FEFO: primero el lote que vence antes (HU-21, objetivo OE2). */
@ExtendWith(MockitoExtension.class)
class EstrategiaFefoTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private EstrategiaFefo estrategia;

    private Producto producto;
    private Almacen almacen;

    @BeforeEach
    void preparar() {
        producto = Producto.builder().codigo("DIA-HF18").nombre("Dializador 1.8")
                .costoPromedio(new BigDecimal("50")).build();
        almacen = Almacen.builder().codigo("ALM-CEN").nombre("Almacen Central").build();
    }

    private Stock celda(String codigoLote, int diasParaVencer, String cantidad) {
        Lote lote = Lote.builder().codigoLote(codigoLote).estadoLote(EstadoLote.LIBERADO)
                .fechaVencimiento(LocalDate.now().plusDays(diasParaVencer))
                .costoUnitario(new BigDecimal("50")).build();
        return Stock.builder().producto(producto).almacen(almacen).lote(lote)
                .cantidad(new BigDecimal(cantidad)).cantidadReservada(BigDecimal.ZERO).build();
    }

    @Test
    @DisplayName("reparte la cantidad empezando por el lote de vencimiento mas proximo")
    void repartePorVencimiento() {
        when(stockRepository.candidatosFefo(anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of(celda("VENCE-30", 30, "40"), celda("VENCE-200", 200, "100")));

        List<EstrategiaAsignacionLote.Asignacion> asignaciones =
                estrategia.asignar(1L, 1L, new BigDecimal("60"));

        assertThat(asignaciones).hasSize(2);
        assertThat(asignaciones.get(0).stock().getLote().getCodigoLote()).isEqualTo("VENCE-30");
        assertThat(asignaciones.get(0).cantidad()).isEqualByComparingTo("40");
        assertThat(asignaciones.get(1).stock().getLote().getCodigoLote()).isEqualTo("VENCE-200");
        assertThat(asignaciones.get(1).cantidad()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("no toma mas lotes de los necesarios")
    void seDetieneAlCubrirLaCantidad() {
        when(stockRepository.candidatosFefo(anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of(celda("VENCE-30", 30, "100"), celda("VENCE-200", 200, "100")));

        List<EstrategiaAsignacionLote.Asignacion> asignaciones =
                estrategia.asignar(1L, 1L, new BigDecimal("50"));

        assertThat(asignaciones).hasSize(1);
        assertThat(asignaciones.get(0).cantidad()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("informa el disponible real cuando el saldo no alcanza")
    void stockInsuficiente() {
        when(stockRepository.candidatosFefo(anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of(celda("VENCE-30", 30, "10")));

        assertThatThrownBy(() -> estrategia.asignar(1L, 1L, new BigDecimal("25")))
                .isInstanceOf(NegocioException.class)
                .satisfies(ex -> {
                    NegocioException negocio = (NegocioException) ex;
                    assertThat(negocio.getCodigo()).isEqualTo(CodigoError.STOCK_INSUFICIENTE);
                    assertThat(negocio.getDetalles()).containsEntry("disponible", new BigDecimal("10.0000"));
                });
    }

    @Test
    @DisplayName("la politica declarada es FEFO")
    void nombreDeLaPolitica() {
        assertThat(estrategia.nombre()).isEqualTo("FEFO");
    }
}
