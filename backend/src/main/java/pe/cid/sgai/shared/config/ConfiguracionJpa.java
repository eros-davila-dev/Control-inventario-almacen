package pe.cid.sgai.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Habilita la auditoria de JPA tomando el usuario autenticado del contexto de seguridad. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class ConfiguracionJpa {

    public static final String SISTEMA = "sistema";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(SISTEMA);
            }
            return Optional.of(auth.getName());
        };
    }
}
