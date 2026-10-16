package pe.cid.sgai.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Escalas unicas para cantidades, costos e importes (RNF-08). */
public final class CantidadUtil {

    public static final int ESCALA_CANTIDAD = 4;
    public static final int ESCALA_COSTO = 6;
    public static final int ESCALA_IMPORTE = 2;

    private CantidadUtil() {
    }

    public static BigDecimal cantidad(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP)
                : valor.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
    }

    public static BigDecimal costo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(ESCALA_COSTO, RoundingMode.HALF_UP)
                : valor.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    public static BigDecimal importe(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP)
                : valor.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }

    public static boolean esPositiva(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean esCero(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) == 0;
    }

    public static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public static BigDecimal dividirSeguro(BigDecimal dividendo, BigDecimal divisor, int escala) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(escala, RoundingMode.HALF_UP);
        }
        return dividendo.divide(divisor, escala, RoundingMode.HALF_UP);
    }
}
