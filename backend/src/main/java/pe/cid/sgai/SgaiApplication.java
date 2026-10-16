package pe.cid.sgai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SGAI-CID: Sistema de Gestion de Almacen e Inventarios.
 * Corporacion Integral de Dialisis S.A.C. - Lima, 2026.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableAsync
@EnableScheduling
public class SgaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgaiApplication.class, args);
    }
}
