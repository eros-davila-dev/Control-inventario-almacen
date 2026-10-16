package pe.cid.sgai.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache en memoria para catalogos de baja volatilidad (permisos, unidades de medida,
 * categorias). Sustituible por Redis sin tocar los servicios.
 */
@Configuration
public class ConfiguracionCache {

    public static final String PERMISOS = "permisos";
    public static final String UNIDADES_MEDIDA = "unidadesMedida";
    public static final String CATEGORIAS = "categorias";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PERMISOS, UNIDADES_MEDIDA, CATEGORIAS);
    }
}
