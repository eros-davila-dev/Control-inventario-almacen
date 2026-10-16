package pe.cid.sgai.seguridad.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.CodigoError;

import java.util.List;

/**
 * Cadena de seguridad: API sin estado, autorizacion por permiso y cabeceras de
 * endurecimiento (RNF-01). Todo endpoint esta denegado por defecto.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracionSeguridad {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/login", "/api/v1/auth/refresh",
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    private final FiltroAutenticacionJwt filtroJwt;
    private final FiltroCambioPassword filtroCambioPassword;
    private final FiltroLimiteIntentos filtroLimiteIntentos;
    private final PropiedadesSgai propiedades;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> escribirError(res, 401,
                                CodigoError.TOKEN_INVALIDO, "Sesion no valida o expirada"))
                        .accessDeniedHandler((req, res, e) -> escribirError(res, 403,
                                CodigoError.PERMISO_DENEGADO, "No cuenta con permisos para esta operacion")))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(31536000))
                        .addHeaderWriter((req, res) -> {
                            res.setHeader("Content-Security-Policy",
                                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
                            res.setHeader("Permissions-Policy",
                                    "geolocation=(), microphone=(), camera=()");
                            res.setHeader("X-Permitted-Cross-Domain-Policies", "none");
                        }))
                .addFilterBefore(filtroLimiteIntentos, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(filtroCambioPassword, FiltroAutenticacionJwt.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(propiedades.cors().origenes());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "Idempotency-Key", "X-Requested-With"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse res, int estado,
                               CodigoError codigo, String detalle) throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("""
                {"status":%d,"codigo":"%s","title":"%s","detail":"%s"}"""
                .formatted(estado, codigo.name(), codigo.mensajePorDefecto(), detalle));
    }
}
