package pe.cid.sgai.compras.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.catalogo.domain.Producto;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Totales e estados de la orden de compra (HU-28, HU-30). */
class OrdenCompraTest {

    private OrdenCompraLinea linea(String cantidad, String precio) {
        OrdenCompraLinea linea = OrdenCompraLinea.builder()
                .producto(Producto.builder().codigo("DIA-HF18").nombre("Dializador").build())
                .cantidad(new BigDecimal(cantidad))
                .cantidadRecibida(BigDecimal.ZERO)
                .precioUnitario(new BigDecimal(precio))
                .build();
        linea.calcularImporte();
        return linea;
    }

    @Test
    @DisplayName("el servidor calcula subtotal, IGV y total")
    void totalesConIgv() {
        OrdenCompra orden = OrdenCompra.builder().numero("OC-2026-00001").build();
        orden.agregarLinea(linea("500", "50"));
        orden.agregarLinea(linea("100", "18.90"));
        orden.calcularTotales(new BigDecimal("0.18"));

        assertThat(orden.getSubtotal()).isEqualByComparingTo("26890.00");
        assertThat(orden.getIgv()).isEqualByComparingTo("4840.20");
        assertThat(orden.getTotal()).isEqualByComparingTo("31730.20");
    }

    @Test
    @DisplayName("no se recibe mas de lo pedido: tolerancia cero")
    void recepcionNoExcedeLoPedido() {
        OrdenCompraLinea linea = linea("100", "50");
        linea.registrarRecepcion(new BigDecimal("60"));
        assertThat(linea.pendiente()).isEqualByComparingTo("40");
        assertThat(linea.estaCompleta()).isFalse();

        assertThatThrownBy(() -> linea.registrarRecepcion(new BigDecimal("41")))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoError.CANTIDAD_EXCEDE_PENDIENTE);

        linea.registrarRecepcion(new BigDecimal("40"));
        assertThat(linea.estaCompleta()).isTrue();
    }

    @Test
    @DisplayName("el estado refleja la recepcion parcial y total")
    void estadoPorRecepcion() {
        OrdenCompra orden = OrdenCompra.builder().numero("OC-1")
                .estado(EstadoOrdenCompra.APROBADA).build();
        OrdenCompraLinea unica = linea("100", "50");
        orden.agregarLinea(unica);

        unica.registrarRecepcion(new BigDecimal("30"));
        orden.actualizarEstadoPorRecepcion();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA_PARCIAL);

        unica.registrarRecepcion(new BigDecimal("70"));
        orden.actualizarEstadoPorRecepcion();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
    }

    @Test
    @DisplayName("las transiciones de estado invalidas se rechazan")
    void transicionInvalida() {
        OrdenCompra orden = OrdenCompra.builder().numero("OC-1")
                .estado(EstadoOrdenCompra.RECIBIDA).build();
        assertThatThrownBy(() -> orden.cambiarEstado(EstadoOrdenCompra.APROBADA))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoError.ESTADO_INVALIDO);
    }
}
