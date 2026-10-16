package pe.cid.sgai.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionOpenApi {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGAI-CID API")
                        .version("1.0.0")
                        .description("""
                                Sistema de Gestion de Almacen e Inventarios de
                                Corporacion Integral de Dialisis S.A.C.
                                Control de insumos y medicamentos de hemodialisis con trazabilidad
                                por lote, FEFO, kardex valorizado y alertas de reposicion.""")
                        .contact(new Contact().name("Departamento de Sistemas - CID")
                                .email("sistemas@nipromed.com"))
                        .license(new License().name("Uso interno CID")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token emitido por POST /api/v1/auth/login")));
    }
}
