package pe.cid.sgai.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Utilidades de fecha en la zona de operacion de la empresa (America/Lima). */
public final class FechaUtil {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private FechaUtil() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static long diasHasta(LocalDate fecha) {
        return fecha == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(hoy(), fecha);
    }

    public static boolean estaVencida(LocalDate fecha) {
        return fecha != null && fecha.isBefore(hoy());
    }
}
