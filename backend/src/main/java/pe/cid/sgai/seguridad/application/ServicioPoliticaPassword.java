package pe.cid.sgai.seguridad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.cid.sgai.seguridad.domain.PasswordHistorico;
import pe.cid.sgai.seguridad.repository.PasswordHistoricoRepository;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Politica de contrasenas: longitud, complejidad y no reutilizacion de las
 * ultimas claves (seccion 1 de docs/05-SEGURIDAD.md).
 */
@Service
@RequiredArgsConstructor
public class ServicioPoliticaPassword {

    private static final int LONGITUD_MINIMA = 10;
    private static final Pattern MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern MINUSCULA = Pattern.compile("[a-z]");
    private static final Pattern DIGITO = Pattern.compile("\\d");
    private static final Pattern SIMBOLO = Pattern.compile("[^A-Za-z0-9]");
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String SIMBOLOS = "!@#$%&*?";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final PasswordHistoricoRepository historicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropiedadesSgai propiedades;

    public void validar(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA) {
            throw new NegocioException(CodigoError.PASSWORD_DEBIL,
                    "La contrasena debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (!MAYUSCULA.matcher(password).find() || !MINUSCULA.matcher(password).find()
                || !DIGITO.matcher(password).find() || !SIMBOLO.matcher(password).find()) {
            throw new NegocioException(CodigoError.PASSWORD_DEBIL,
                    "La contrasena debe combinar mayusculas, minusculas, numeros y simbolos");
        }
    }

    public void validarNoReutilizada(Long usuarioId, String passwordPlano) {
        int limite = propiedades.seguridad().historicoPassword();
        List<PasswordHistorico> historico = historicoRepository
                .findTop5ByUsuarioIdOrderByFechaDesc(usuarioId);
        historico.stream().limit(limite).forEach(h -> {
            if (passwordEncoder.matches(passwordPlano, h.getPasswordHash())) {
                throw new NegocioException(CodigoError.PASSWORD_REUTILIZADA);
            }
        });
    }

    public void registrarEnHistorico(Long usuarioId, String passwordHash) {
        historicoRepository.save(PasswordHistorico.builder()
                .usuarioId(usuarioId)
                .passwordHash(passwordHash)
                .fecha(Instant.now())
                .build());
    }

    /** Contrasena temporal que cumple la politica, para alta de usuario o reseteo. */
    public String generarTemporal() {
        StringBuilder sb = new StringBuilder();
        sb.append((char) ('A' + ALEATORIO.nextInt(26)));
        sb.append((char) ('a' + ALEATORIO.nextInt(26)));
        sb.append((char) ('2' + ALEATORIO.nextInt(8)));
        sb.append(SIMBOLOS.charAt(ALEATORIO.nextInt(SIMBOLOS.length())));
        for (int i = 0; i < 8; i++) {
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
