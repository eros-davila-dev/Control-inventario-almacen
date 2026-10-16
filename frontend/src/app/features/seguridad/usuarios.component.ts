import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';
import { Rol, UsuarioRequest, UsuarioSesion } from '../../core/models/seguridad.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiCheckboxComponent,
  UiInputComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
} from '../../shared/ui';
import { RolAdminService, UsuarioAdminService } from './seguridad.service';

/**
 * Administracion de usuarios (HU-04). La contrasena inicial la genera el sistema
 * y se entrega al usuario, que debe cambiarla en su primer acceso.
 */
@Component({
  selector: 'sgai-usuarios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent, UiInputComponent,
    UiCheckboxComponent, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Usuarios"
                    descripcion="Cuentas del sistema, roles asignados y almacenes que cada usuario puede operar.">
      <div acciones>
        <span *hasPermiso="'seguridad:usuario:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nuevo usuario</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-3">
        <input class="sgai-input sm:col-span-2" type="search"
               placeholder="Nombre, usuario o correo..."
               (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
        <select class="sgai-input" (change)="lista.filtrar({ activo: $any($event.target).value || null })">
          <option value="">Activos e inactivos</option>
          <option value="true">Solo activos</option>
          <option value="false">Solo inactivos</option>
        </select>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)">
          <ng-template #fila let-usuario>
            <td class="px-3 py-2.5">
              <p class="font-medium text-fg">{{ usuario.nombreCompleto }}</p>
              <p class="text-xs text-fg-muted">{{ usuario.username }} · {{ usuario.email }}</p>
            </td>
            <td class="hidden px-3 py-2.5 sm:table-cell">
              <div class="flex flex-wrap gap-1">
                @for (rol of usuario.roles; track rol) {
                  <ui-badge tono="marca" [punto]="false">{{ rol.replace('_', ' ') }}</ui-badge>
                }
              </div>
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted lg:table-cell">
              {{ usuario.cargo || '—' }}
            </td>
            <td class="hidden px-3 py-2.5 text-center text-xs text-fg-muted lg:table-cell">
              {{ usuario.almacenIds.length || 'Todos' }}
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ usuario.ultimoAcceso | fechaSgai: 'hora' }}
            </td>
            <td class="px-3 py-2.5 text-center">
              <div class="flex flex-col items-center gap-1">
                <ui-badge [tono]="usuario.activo ? 'exito' : 'neutral'">
                  {{ usuario.activo ? 'Activo' : 'Inactivo' }}
                </ui-badge>
                @if (usuario.debeCambiarPassword) {
                  <ui-badge tono="advertencia" [punto]="false">Clave temporal</ui-badge>
                }
              </div>
            </td>
            <td class="px-3 py-2.5 text-right">
              <span *hasPermiso="'seguridad:usuario:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(usuario)">Editar</button>
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                               hover:bg-surface-muted dark:text-brand-300"
                        (click)="resetear(usuario)">Resetear clave</button>
                <button type="button"
                        class="rounded-lg px-2 py-1 text-xs font-medium hover:bg-surface-muted"
                        [class.text-danger]="usuario.activo" [class.text-success]="!usuario.activo"
                        (click)="alternarEstado(usuario)">
                  {{ usuario.activo ? 'Desactivar' : 'Activar' }}
                </button>
              </span>
            </td>
          </ng-template>
        </ui-table>
        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <!-- Formulario -->
    <ui-modal [abierto]="modalAbierto()" tamano="lg"
              [titulo]="editando() ? 'Editar usuario' : 'Nuevo usuario'"
              subtitulo="El alcance por almacen limita donde puede registrar movimientos."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <ui-input etiqueta="Usuario" formControlName="username" [requerido]="true"
                    ayuda="Sin espacios: nombre.apellido" />
          <ui-input etiqueta="Correo institucional" tipo="email" formControlName="email"
                    [requerido]="true" />
          <ui-input etiqueta="Nombres" formControlName="nombres" [requerido]="true" />
          <ui-input etiqueta="Apellidos" formControlName="apellidos" [requerido]="true" />
          <ui-input etiqueta="Documento (DNI)" formControlName="dni" modoEntrada="numeric" />
          <ui-input etiqueta="Cargo" formControlName="cargo" />
          <ui-input etiqueta="Telefono" formControlName="telefono" tipo="tel" />
          <ui-checkbox etiqueta="Usuario activo" formControlName="activo" />
        </div>

        <fieldset class="rounded-xl border border-border p-3">
          <legend class="px-1 text-sm font-semibold text-fg">Roles</legend>
          <p class="mb-2 text-xs text-fg-muted">
            Los permisos del usuario son la suma de los permisos de sus roles.
          </p>
          <div class="grid gap-1 sm:grid-cols-2">
            @for (rol of roles(); track rol.id) {
              <label class="flex items-start gap-2 py-1">
                <input type="checkbox" class="mt-1 size-4 accent-brand-600"
                       [checked]="rolesSeleccionados().has(rol.id)"
                       (change)="alternarRol(rol.id)" />
                <span>
                  <span class="block text-sm font-medium text-fg">{{ rol.nombre.replace('_', ' ') }}</span>
                  <span class="block text-xs text-fg-muted">{{ rol.descripcion }}</span>
                </span>
              </label>
            }
          </div>
          @if (!rolesSeleccionados().size) {
            <p class="mt-2 text-xs font-medium text-danger">Asigne al menos un rol.</p>
          }
        </fieldset>

        <fieldset class="rounded-xl border border-border p-3">
          <legend class="px-1 text-sm font-semibold text-fg">Almacenes autorizados</legend>
          <p class="mb-2 text-xs text-fg-muted">
            Sin seleccion, el usuario queda limitado por su rol: los roles con vision global
            (ADMIN, JEFE_ALMACEN, AUDITOR) operan toda la red.
          </p>
          <div class="grid gap-1 sm:grid-cols-2">
            @for (almacen of contexto.almacenes(); track almacen.id) {
              <label class="flex items-center gap-2 py-1">
                <input type="checkbox" class="size-4 accent-brand-600"
                       [checked]="almacenesSeleccionados().has(almacen.id)"
                       (change)="alternarAlmacen(almacen.id)" />
                <span class="text-sm text-fg">{{ almacen.codigo }} · {{ almacen.nombre }}</span>
              </label>
            }
          </div>
        </fieldset>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>

    <!-- Contrasena temporal generada -->
    <ui-modal [abierto]="!!passwordTemporal()" tamano="sm" titulo="Contrasena temporal generada"
              subtitulo="Entreguela al usuario por un canal seguro: debera cambiarla al ingresar."
              (cerrado)="passwordTemporal.set(null)">
      <div class="space-y-3">
        <p class="text-sm text-fg-muted">
          Usuario <strong class="text-fg">{{ passwordTemporal()?.username }}</strong>
        </p>
        <code class="block select-all rounded-lg border border-border bg-surface-muted px-3 py-2.5
                     text-center font-mono text-lg tracking-wider text-fg">
          {{ passwordTemporal()?.passwordTemporal }}
        </code>
        <p class="text-xs text-fg-subtle">
          Esta contrasena no se vuelve a mostrar. Si se pierde, use "Resetear clave".
        </p>
      </div>
      <div pie>
        <ui-button (presionado)="passwordTemporal.set(null)">Entendido</ui-button>
      </div>
    </ui-modal>
  `,
})
export class UsuariosComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(UsuarioAdminService);
  private readonly rolServicio = inject(RolAdminService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'usuario', titulo: 'Usuario', ordenPor: 'apellidos' },
    { clave: 'roles', titulo: 'Roles', ocultarEn: 'movil' },
    { clave: 'cargo', titulo: 'Cargo', ocultarEn: 'tablet' },
    { clave: 'almacenes', titulo: 'Almacenes', alineacion: 'centro', ocultarEn: 'tablet' },
    { clave: 'acceso', titulo: 'Ultimo acceso', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<UsuarioSesion>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'apellidos,asc' });

  protected readonly roles = signal<Rol[]>([]);
  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<UsuarioSesion | null>(null);
  protected readonly guardando = signal(false);
  protected readonly passwordTemporal = signal<{ username: string; passwordTemporal: string } | null>(null);
  protected readonly rolesSeleccionados = signal<Set<number>>(new Set());
  protected readonly almacenesSeleccionados = signal<Set<number>>(new Set());

  protected readonly formulario = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(4),
      Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    email: ['', [Validators.required, Validators.email]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    dni: [''],
    cargo: [''],
    telefono: [''],
    activo: [true],
  });

  constructor() {
    this.rolServicio.listar().subscribe((roles) => this.roles.set(roles));
    this.lista.recargar();
  }

  protected alternarRol(id: number): void {
    this.rolesSeleccionados.update((actual) => {
      const copia = new Set(actual);
      copia.has(id) ? copia.delete(id) : copia.add(id);
      return copia;
    });
  }

  protected alternarAlmacen(id: number): void {
    this.almacenesSeleccionados.update((actual) => {
      const copia = new Set(actual);
      copia.has(id) ? copia.delete(id) : copia.add(id);
      return copia;
    });
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({ activo: true });
    this.rolesSeleccionados.set(new Set());
    this.almacenesSeleccionados.set(new Set());
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(usuario: UsuarioSesion): void {
    this.editando.set(usuario);
    this.formulario.patchValue({
      username: usuario.username,
      email: usuario.email,
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      dni: usuario.dni ?? '',
      cargo: usuario.cargo ?? '',
      telefono: usuario.telefono ?? '',
      activo: usuario.activo,
    });
    const idsRoles = this.roles()
      .filter((rol) => usuario.roles.includes(rol.nombre))
      .map((rol) => rol.id);
    this.rolesSeleccionados.set(new Set(idsRoles));
    this.almacenesSeleccionados.set(new Set(usuario.almacenIds));
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid || !this.rolesSeleccionados().size) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: UsuarioRequest = {
      ...valores,
      dni: valores.dni || null,
      cargo: valores.cargo || null,
      telefono: valores.telefono || null,
      rolIds: [...this.rolesSeleccionados()],
      almacenIds: [...this.almacenesSeleccionados()],
    };
    this.guardando.set(true);
    const editando = this.editando();
    if (editando) {
      this.servicio.actualizar(editando.id, cuerpo).subscribe({
        next: () => {
          this.guardando.set(false);
          this.modalAbierto.set(false);
          this.notificacion.exito('Usuario actualizado', cuerpo.username);
          this.lista.recargar();
        },
        error: () => this.guardando.set(false),
      });
      return;
    }
    this.servicio.crear(cuerpo).subscribe({
      next: (respuesta) => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.passwordTemporal.set(respuesta);
        this.notificacion.exito('Usuario creado', respuesta.username);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async resetear(usuario: UsuarioSesion): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Resetear contrasena',
      mensaje: `Se generara una contrasena temporal para ${usuario.nombreCompleto} y se cerraran sus sesiones activas.`,
      textoConfirmar: 'Resetear',
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.resetearPassword(usuario.id).subscribe((temporal) => {
      this.passwordTemporal.set(temporal);
      this.lista.recargar();
    });
  }

  protected async alternarEstado(usuario: UsuarioSesion): Promise<void> {
    const activar = !usuario.activo;
    const respuesta = await this.confirmacion.preguntar({
      titulo: activar ? 'Activar usuario' : 'Desactivar usuario',
      mensaje: activar
        ? `${usuario.nombreCompleto} podra volver a ingresar al sistema.`
        : `${usuario.nombreCompleto} no podra ingresar y se cerraran sus sesiones.`,
      textoConfirmar: activar ? 'Activar' : 'Desactivar',
      peligroso: !activar,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.cambiarEstado(usuario.id, activar).subscribe(() => {
      this.notificacion.exito(activar ? 'Usuario activado' : 'Usuario desactivado');
      this.lista.recargar();
    });
  }
}
