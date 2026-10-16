package pe.cid.sgai.alertas.application.dto;

/** Contadores para la campana de notificaciones del encabezado (HU-42). */
public record ResumenAlertasResponse(
        long total, long criticas, long altas, long medias, long bajas,
        long stockBajo, long porVencer, long vencidos
) {
}
