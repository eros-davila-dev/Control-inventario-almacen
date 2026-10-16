package pe.cid.sgai.seguridad.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pe.cid.sgai.seguridad.domain.Usuario;
import pe.cid.sgai.shared.config.PropiedadesSgai;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Emision y verificacion de tokens JWT (HS512). El secreto proviene del entorno:
 * en produccion la aplicacion no arranca sin el.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_NOMBRE = "nombre";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISOS = "permisos";
    private static final String CLAIM_ALMACENES = "almacenes";
    private static final String CLAIM_CAMBIO_PASSWORD = "cambioPassword";

    private final PropiedadesSgai propiedades;
    private final Environment environment;
    private SecretKey clave;

    public JwtService(PropiedadesSgai propiedades, Environment environment) {
        this.propiedades = propiedades;
        this.environment = environment;
    }

    @PostConstruct
    void inicializar() {
        String secreto = propiedades.jwt().secret();
        boolean produccion = List.of(environment.getActiveProfiles()).contains("prod");
        if (secreto == null || secreto.isBlank()) {
            if (produccion) {
                throw new IllegalStateException(
                        "SGAI_JWT_SECRET es obligatorio en produccion: configure el secreto en el entorno");
            }
            log.warn("SGAI_JWT_SECRET no configurado: se genera una clave efimera solo para desarrollo");
            this.clave = Jwts.SIG.HS512.key().build();
            return;
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            if (produccion) {
                throw new IllegalStateException("SGAI_JWT_SECRET debe tener al menos 64 bytes para HS512");
            }
            bytes = sha512(bytes);
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(Duration.ofMinutes(propiedades.jwt().accessTokenMinutos()));
        return Jwts.builder()
                .issuer(propiedades.jwt().emisor())
                .subject(usuario.getUsername())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_UID, usuario.getId())
                .claim(CLAIM_NOMBRE, usuario.nombreCompleto())
                .claim(CLAIM_ROLES, List.copyOf(usuario.nombresRoles()))
                .claim(CLAIM_PERMISOS, List.copyOf(usuario.codigosPermisos()))
                .claim(CLAIM_ALMACENES, List.copyOf(usuario.getAlmacenIds()))
                .claim(CLAIM_CAMBIO_PASSWORD, usuario.isDebeCambiarPassword())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS512)
                .compact();
    }

    public long segundosDeVigencia() {
        return Duration.ofMinutes(propiedades.jwt().accessTokenMinutos()).toSeconds();
    }

    /** Devuelve el principal si el token es valido; {@code null} si no lo es. */
    public UsuarioAutenticado validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(propiedades.jwt().emisor())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new UsuarioAutenticado(
                    numero(claims.get(CLAIM_UID)),
                    claims.getSubject(),
                    claims.get(CLAIM_NOMBRE, String.class),
                    textos(claims.get(CLAIM_ROLES)),
                    textos(claims.get(CLAIM_PERMISOS)),
                    numeros(claims.get(CLAIM_ALMACENES)),
                    Boolean.TRUE.equals(claims.get(CLAIM_CAMBIO_PASSWORD, Boolean.class)));
        } catch (ExpiredJwtException ex) {
            log.debug("Token expirado para {}", ex.getClaims().getSubject());
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token invalido: {}", ex.getMessage());
            return null;
        }
    }

    /** Hash del refresh token: en base de datos nunca se guarda el valor en claro. */
    public String hashear(String valor) {
        return Base64.getEncoder().encodeToString(sha256(valor.getBytes(StandardCharsets.UTF_8)));
    }

    public String generarRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    @SuppressWarnings("unchecked")
    private Set<String> textos(Object valor) {
        if (valor instanceof List<?> lista) {
            Set<String> resultado = new HashSet<>();
            ((List<Object>) lista).forEach(v -> resultado.add(String.valueOf(v)));
            return resultado;
        }
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    private Set<Long> numeros(Object valor) {
        if (valor instanceof List<?> lista) {
            Set<Long> resultado = new HashSet<>();
            ((List<Object>) lista).forEach(v -> resultado.add(numero(v)));
            return resultado;
        }
        return Set.of();
    }

    private Long numero(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        return valor == null ? null : Long.valueOf(String.valueOf(valor));
    }

    private byte[] sha256(byte[] datos) {
        return digerir("SHA-256", datos);
    }

    private byte[] sha512(byte[] datos) {
        return digerir("SHA-512", datos);
    }

    private byte[] digerir(String algoritmo, byte[] datos) {
        try {
            return MessageDigest.getInstance(algoritmo).digest(datos);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo no disponible: " + algoritmo, ex);
        }
    }
}
