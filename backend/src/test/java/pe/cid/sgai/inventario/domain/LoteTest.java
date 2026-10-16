package pe.cid.sgai.inventario.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Maquina de estados sanitarios del lote (HU-16). */
class LoteTest {

    private Lote lote(EstadoLote estado, LocalDate vencimiento) {
        return Lote.builder().codigoLote("LOTE-1").estadoLote(estado)
                .fechaVencimiento(vencimiento).build();
    }

    @Test
    @DisplayName("solo un lote liberado y vigente es despachable")
    void despachable() {
        assertThat(lote(EstadoLote.LIBERADO, LocalDate.now().plusDays(30)).esDespachable()).isTrue();
        assertThat(lote(EstadoLote.CUARENTENA, LocalDate.now().plusDays(30)).esDespachable()).isFalse();
        assertThat(lote(EstadoLote.RETENIDO, LocalDate.now().plusDays(30)).esDespachable()).isFalse();
        assertThat(lote(EstadoLote.LIBERADO, LocalDate.now().minusDays(1)).esDespachable()).isFalse();
    }

    @Test
    @DisplayName("la liberacion desde cuarentena registra responsable y fecha")
    void liberacionRegistraResponsable() {
        Lote lote = lote(EstadoLote.CUARENTENA, LocalDate.now().plusYears(1));
        lote.cambiarEstado(EstadoLote.LIBERADO, "Certificado de analisis conforme", "qf.farmacia");
        assertThat(lote.getEstadoLote()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(lote.getLiberadoPor()).isEqualTo("qf.farmacia");
        assertThat(lote.getFechaLiberacion()).isNotNull();
    }

    @Test
    @DisplayName("un lote rechazado no vuelve a liberarse")
    void transicionInvalida() {
        Lote lote = lote(EstadoLote.RECHAZADO, LocalDate.now().plusYears(1));
        assertThatThrownBy(() -> lote.cambiarEstado(EstadoLote.LIBERADO, "motivo", "qf"))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    @Test
    @DisplayName("el cambio de estado exige motivo")
    void motivoObligatorio() {
        Lote lote = lote(EstadoLote.LIBERADO, LocalDate.now().plusYears(1));
        assertThatThrownBy(() -> lote.cambiarEstado(EstadoLote.RETENIDO, "  ", "jefe.almacen"))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoError.VALIDACION);
    }

    @Test
    @DisplayName("los dias para vencer y el marcado de vencido son coherentes")
    void vencimiento() {
        Lote lote = lote(EstadoLote.LIBERADO, LocalDate.now().plusDays(15));
        assertThat(lote.diasParaVencer()).isEqualTo(15);
        assertThat(lote.estaVencido()).isFalse();
        Lote vencido = lote(EstadoLote.LIBERADO, LocalDate.now().minusDays(2));
        vencido.marcarVencido();
        assertThat(vencido.getEstadoLote()).isEqualTo(EstadoLote.VENCIDO);
        assertThat(vencido.esDespachable()).isFalse();
    }
}
