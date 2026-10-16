package pe.cid.sgai.seguridad.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Autentica cada peticion a partir del access token. Los permisos del token se
 * publican como authorities, de modo que {@code @PreAuthorize} evalua permisos
 * granulares sin consultar la base de datos.
 */
@Component
@RequiredArgsConstructor
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader(CABECERA);
        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsuarioAutenticado usuario = jwtService.validar(cabecera.substring(PREFIJO.length()).trim());
            if (usuario != null) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                usuario.permisos().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                usuario.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
