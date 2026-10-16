package pe.cid.sgai.alertas.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.cid.sgai.inventario.domain.StockActualizadoEvent;

/**
 * Reaccion desacoplada al cambio de saldo: el modulo de inventario no conoce al de
 * alertas. Se ejecuta despues del commit, de modo que un fallo al evaluar la alerta
 * nunca revierte el movimiento ya registrado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscuchaStockActualizado {

    private final AlertaService alertaService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alActualizarStock(StockActualizadoEvent evento) {
        try {
            alertaService.evaluarReposicion(evento.productoId(), evento.almacenId());
        } catch (Exception ex) {
            log.error("No se pudo evaluar la reposicion de producto {} en almacen {}",
                    evento.productoId(), evento.almacenId(), ex);
        }
    }
}
