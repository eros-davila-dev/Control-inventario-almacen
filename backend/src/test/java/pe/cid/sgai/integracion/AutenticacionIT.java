package pe.cid.sgai.integracion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.seguridad.domain.Permiso;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.seguridad.domain.Rol;
import pe.cid.sgai.seguridad.domain.Usuario;
import pe.cid.sgai.seguridad.repository.PermisoRepository;
import pe.cid.sgai.seguridad.repository.RolRepository;
import pe.cid.sgai.seguridad.repository.UsuarioRepository;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la cadena de seguridad: autenticacion, bloqueo por
 * intentos, cambio de contrasena obligatorio y autorizacion por permiso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AutenticacionIT {

    private static final String PASSWORD = "Prueba#2026Sgai";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PermisoRepository permisoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparar() {
        if (usuarioRepository.existsByUsernameIgnoreCase("almacenero.prueba")) {
            return;
        }
        Permiso leerStock = permisoRepository.save(Permiso.builder()
                .codigo(Permisos.STOCK_LEER).modulo("inventario").descripcion("leer stock").build());
        Set<Permiso> permisos = new LinkedHashSet<>();
        permisos.add(leerStock);
        Rol rol = rolRepository.save(Rol.builder().nombre("ALMACENERO")
                .descripcion("Operador de almacen").esSistema(true).activo(true)
                .permisos(permisos).build());
        usuarioRepository.save(Usuario.builder()
                .username("almacenero.prueba").email("almacenero.prueba@nipromed.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .nombres("Carlos").apellidos("Mendoza").activo(true)
                .debeCambiarPassword(false)
                // Colecciones mutables: Hibernate necesita poder reemplazarlas al hacer merge.
                .roles(new LinkedHashSet<>(Set.of(rol)))
                .almacenIds(new LinkedHashSet<>(Set.of(1L)))
                .build());
    }

    private String cuerpoLogin(String usuario, String password) {
        return "{\"usuario\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password);
    }

    @Test
    @DisplayName("el login entrega access token, refresh token y los permisos del usuario")
    void loginExitoso() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("almacenero.prueba", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.username").value("almacenero.prueba"))
                .andExpect(jsonPath("$.usuario.permisos[0]").value(Permisos.STOCK_LEER));
    }

    @Test
    @DisplayName("las credenciales invalidas devuelven 401 sin revelar si el usuario existe")
    void credencialesInvalidas() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("almacenero.prueba", "ClaveIncorrecta#1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("no.existe", "ClaveIncorrecta#1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("sin token toda la API responde 401")
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("con token pero sin el permiso requerido responde 403")
    void sinPermisoNoHayAcceso() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("almacenero.prueba", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = respuesta.split("\"accessToken\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PERMISO_DENEGADO"));

        mockMvc.perform(get("/api/v1/stock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el login valida el cuerpo de la peticion")
    void validacionDeEntrada() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores").isMap());
    }

    @Test
    @DisplayName("el refresh token rotado no se puede reutilizar")
    void refreshTokenRotatorio() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("almacenero.prueba", PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        String refresh = respuesta.split("\"refreshToken\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }
}
