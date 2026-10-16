package pe.cid.sgai.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Configuracion de MVC, formatos y ejecutor de tareas asincronas. */
@Configuration
@EnableSpringDataWebSupport
public class ConfiguracionWeb implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Las fechas se intercambian siempre en ISO-8601 (yyyy-MM-dd / yyyy-MM-ddTHH:mm:ss).
    }

    @Bean(name = "tareasSgai")
    public Executor tareasSgai() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
