package pe.cid.sgai.alertas.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.cid.sgai.inventario.application.LoteService;
import pe.cid.sgai.seguridad.repository.RefreshTokenRepository;
import pe.cid.sgai.shared.audit.AuditoriaService;

import java.time.Instant;

/**
 * Tareas programadas del sistema. Las horas estan fuera del horario de atencion de
 * las sedes para no competir con la operacion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final LoteService loteService;
    private final AlertaService alertaService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditoriaService auditoriaService;

    /** 00:30 - Marca como vencidos los lotes cuya fecha de vencimiento paso (HU-18). */
    @Scheduled(cron = "0 30 0 * * *", zone = "America/Lima")
    public void marcarLotesVencidos() {
        int cantidad = loteService.marcarVencidos();
        if (cantidad > 0) {
            auditoriaService.registrar("JOB_MARCAR_VENCIDOS", "lote", null,
                    "lotes marcados como vencidos: " + cantidad);
        }
    }

    /** 01:00 - Genera las alertas de vencimiento por tramos (HU-40). */
    @Scheduled(cron = "0 0 1 * * *", zone = "America/Lima")
    public void generarAlertasVencimiento() {
        int lotes = alertaService.evaluarVencimientos();
        log.info("Tarea de vencimientos completada sobre {} lote(s)", lotes);
    }

    /** 01:30 - Revisa la reposicion de todo el inventario (HU-39). */
    @Scheduled(cron = "0 30 1 * * *", zone = "America/Lima")
    public void generarAlertasReposicion() {
        int items = alertaService.evaluarReposicionGlobal();
        log.info("Tarea de reposicion completada sobre {} item(s)", items);
    }

    /** 02:00 - Limpia los refresh token expirados. */
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Lima")
    public void limpiarTokens() {
        int eliminados = refreshTokenRepository.eliminarExpirados(Instant.now());
        if (eliminados > 0) {
            log.info("Se eliminaron {} refresh token(s) expirados", eliminados);
        }
    }
}
