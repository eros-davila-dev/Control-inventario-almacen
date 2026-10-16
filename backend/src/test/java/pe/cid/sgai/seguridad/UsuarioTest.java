package pe.cid.sgai.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.cid.sgai.seguridad.domain.Permiso;
import pe.cid.sgai.seguridad.domain.Permisos;
import pe.cid.sgai.seguridad.domain.Rol;
import pe.cid.sgai.seguridad.domain.Usuario;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloqueo por intentos fallidos y resolucion de permisos (HU-01, HU-05). */
class UsuarioTest {

    private Usuario usuario() {
        Rol rol = Rol.builder().nombre("ALMACENERO")
                .permisos(Set.of(
                        Permiso.builder().codigo(Permisos.STOCK_LEER).modulo("inventario")
                                .descripcion("leer stock").build(),
                        Permiso.builder().codigo(Permisos.MOVIMIENTO_REGISTRAR).modulo("inventario")
                                .descripcion("registrar movimientos").build()))
                .build();
        return Usuario.builder().username("almacenero.central").email("a@b.pe")
                .passwordHash("x").nombres("Carlos").apellidos("Mendoza")
                .roles(Set.of(rol)).almacenIds(Set.of(1L, 2L)).build();
    }

    @Test
    @DisplayName("la cuenta se bloquea al alcanzar el maximo de intentos")
    void bloqueoPorIntentos() {
        Usuario usuario = usuario();
        for (int i = 0; i < 4; i++) {
            usuario.registrarIntentoFallido(5, 15);
            assertThat(usuario.estaBloqueado()).isFalse();
        }
        usuario.registrarIntentoFallido(5, 15);
        assertThat(usuario.estaBloqueado()).isTrue();
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    @DisplayName("un acceso exitoso limpia el bloqueo y registra la fecha")
    void accesoExitosoLimpiaBloqueo() {
        Usuario usuario = usuario();
        usuario.registrarIntentoFallido(5, 15);
        usuario.registrarAccesoExitoso();
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.getUltimoAcceso()).isNotNull();
    }

    @Test
    @DisplayName("los permisos se resuelven desde los roles")
    void permisosDesdeRoles() {
        Usuario usuario = usuario();
        assertThat(usuario.codigosPermisos())
                .containsExactlyInAnyOrder(Permisos.STOCK_LEER, Permisos.MOVIMIENTO_REGISTRAR);
        assertThat(usuario.tienePermiso(Permisos.MOVIMIENTO_ANULAR)).isFalse();
        assertThat(usuario.tieneRol("ALMACENERO")).isTrue();
        assertThat(usuario.nombreCompleto()).isEqualTo("Carlos Mendoza");
    }
}
