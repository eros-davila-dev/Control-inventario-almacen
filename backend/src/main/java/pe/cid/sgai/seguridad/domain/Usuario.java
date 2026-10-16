package pe.cid.sgai.seguridad.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.cid.sgai.shared.domain.BaseEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuario del sistema. El alcance operativo se limita por los almacenes asignados
 * (HU-14): un almacenero de sede no puede mover stock de otra sede.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuario")
public class Usuario extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 120)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 120)
    private String apellidos;

    @Column(name = "dni", length = 15)
    private String dni;

    @Column(name = "cargo", length = 120)
    private String cargo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Builder.Default
    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Builder.Default
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new LinkedHashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_almacen", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "almacen_id", nullable = false)
    private Set<Long> almacenIds = new HashSet<>();

    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(Instant.now());
    }

    public Set<String> codigosPermisos() {
        return roles.stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::getCodigo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> nombresRoles() {
        return roles.stream().map(Rol::getNombre).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean tienePermiso(String codigo) {
        return codigosPermisos().contains(codigo);
    }

    public boolean tieneRol(String nombre) {
        return nombresRoles().contains(nombre);
    }

    /** Suma un intento fallido y bloquea la cuenta al superar el maximo permitido. */
    public void registrarIntentoFallido(int maxIntentos, int minutosBloqueo) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= maxIntentos) {
            this.bloqueadoHasta = Instant.now().plusSeconds(60L * minutosBloqueo);
            this.intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
        this.ultimoAcceso = Instant.now();
    }
}
