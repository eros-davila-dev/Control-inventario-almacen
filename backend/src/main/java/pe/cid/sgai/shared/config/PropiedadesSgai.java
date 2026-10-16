package pe.cid.sgai.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/** Configuracion tipada de la aplicacion (prefijo {@code sgai}). */
@ConfigurationProperties(prefix = "sgai")
public record PropiedadesSgai(
        Jwt jwt,
        Seguridad seguridad,
        Cors cors,
        Inventario inventario,
        String zonaHoraria,
        String moneda,
        BigDecimal igv
) {

    public record Jwt(String secret, int accessTokenMinutos, int refreshTokenHoras, String emisor) {
    }

    public record Seguridad(int maxIntentosFallidos, int ventanaIntentosMinutos, int bloqueoMinutos,
                            int historicoPassword, int loginPorMinuto) {
    }

    public record Cors(List<String> origenes) {
    }

    public record Inventario(boolean permitirStockNegativo, List<Integer> tramosVencimientoDias,
                             int diasConsumoPromedio, int diasSinMovimiento) {
    }
}
