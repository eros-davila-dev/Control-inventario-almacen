package pe.cid.sgai.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.seguridad.infrastructure.UsuarioAutenticado;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Alcance operativo por almacen (HU-14). */
class UsuarioAutenticadoTest {

    private UsuarioAutenticado principal(Set<String> roles, Set<Long> almacenes) {
        return new UsuarioAutenticado(1L, "usuario.prueba", "Usuario Prueba", roles,
                Set.of("inventario:stock:leer"), almacenes, false);
    }

    @Test
    @DisplayName("el nombre del principal es el username, no el objeto")
    void nombreDelPrincipal() {
        assertThat(principal(Set.of("ALMACENERO"), Set.of(1L)).getName()).isEqualTo("usuario.prueba");
    }

    @Test
    @DisplayName("el almacenero solo opera los almacenes asignados")
    void alcanceDelAlmacenero() {
        UsuarioAutenticado almacenero = principal(Set.of("ALMACENERO"), Set.of(5L, 6L));
        assertThat(almacenero.puedeOperarAlmacen(5L)).isTrue();
        assertThat(almacenero.puedeOperarAlmacen(1L)).isFalse();
        assertThat(almacenero.tieneVisionGlobal()).isFalse();
    }

    @Test
    @DisplayName("los roles con vision global operan cualquier almacen")
    void visionGlobal() {
        assertThat(principal(Set.of("JEFE_ALMACEN"), Set.of()).puedeOperarAlmacen(99L)).isTrue();
        assertThat(principal(Set.of("ADMIN"), Set.of()).esAdministrador()).isTrue();
        assertThat(principal(Set.of("AUDITOR"), Set.of()).tieneVisionGlobal()).isTrue();
    }
}
