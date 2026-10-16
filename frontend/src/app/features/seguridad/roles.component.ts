import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Permiso, Rol, RolRequest } from '../../core/models/seguridad.model';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import {
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiInputComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiSkeletonComponent,
  UiTextareaComponent,
} from '../../shared/ui';
import { RolAdminService } from './seguridad.service';

/**
 * Roles y permisos granulares (HU-05). Los roles del sistema no se eliminan y
 * los permisos se agrupan por modulo para hacer legible la matriz de accesos.
 */
@Component({
  selector: 'sgai-roles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiButtonComponent,
    UiBadgeComponent, UiModalComponent, UiInputComponent, UiTextareaComponent, UiSkeletonComponent,
    HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Roles y permisos"
                    descripcion="Cada rol agrupa permisos con formato modulo:recurso:accion. El sistema autoriza por permiso, nunca por rol.">
      <div acciones>
        <span *hasPermiso="'seguridad:rol:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nuevo rol</ui-button>
        </span>
      </div>
    </ui-page-header>

    @if (cargando()) {
      <ui-skeleton [filas]="6" />
    } @else {
      <div class="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
        @for (rol of roles(); track rol.id) {
          <ui-card [titulo]="rol.nombre.replace('_', ' ')" [subtitulo]="rol.descripcion || ''">
            <div acciones class="flex items-center gap-1">
              @if (rol.esSistema) {
                <ui-badge tono="info" [punto]="false">Sistema</ui-badge>
              }
              <ui-badge [tono]="rol.activo ? 'exito' : 'neutral'" [punto]="false">
                {{ rol.activo ? 'Activo' : 'Inactivo' }}
              </ui-badge>
            </div>

            <p class="mb-3 text-xs text-fg-muted">
              {{ rol.usuariosAsignados }} usuario(s) · {{ rol.permisos.length }} permiso(s)
            </p>

            <div class="sgai-scroll max-h-44 space-y-2 overflow-y-auto pr-1">
              @for (grupo of agrupar(rol.permisos); track grupo.modulo) {
                <div>
                  <p class="text-[11px] font-semibold uppercase tracking-wide text-fg-subtle">
                    {{ grupo.modulo }}
                  </p>
                  <div class="mt-1 flex flex-wrap gap-1">
                    @for (permiso of grupo.permisos; track permiso.id) {
                      <code class="rounded bg-surface-muted px-1.5 py-0.5 text-[11px] text-fg-muted"
                            [attr.title]="permiso.descripcion">
                        {{ permiso.codigo.split(':').slice(1).join(':') }}
                      </code>
                    }
                  </div>
                </div>
              }
            </div>

            <div class="mt-3 flex justify-end gap-2">
              <span *hasPermiso="'seguridad:rol:escribir'">
                <ui-button tamano="sm" variante="secundario" (presionado)="abrirEdicion(rol)">
                  Editar permisos
                </ui-button>
              </span>
              @if (!rol.esSistema) {
                <span *hasPermiso="'seguridad:rol:escribir'">
                  <ui-button tamano="sm" variante="peligro" (presionado)="eliminar(rol)">
                    Eliminar
                  </ui-button>
                </span>
              }
            </div>
          </ui-card>
        }
      </div>
    }

    <ui-modal [abierto]="modalAbierto()" tamano="lg"
              [titulo]="editando() ? 'Editar rol' : 'Nuevo rol'"
              subtitulo="Marque los permisos que el rol debe otorgar."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <ui-input etiqueta="Nombre del rol" formControlName="nombre" [requerido]="true"
                    ayuda="Mayusculas y guion bajo: JEFE_ALMACEN"
                    [error]="errorNombre()" />
          <ui-textarea etiqueta="Descripcion" formControlName="descripcion" [filas]="2" />
        </div>

        <div class="space-y-3">
          @for (modulo of modulos(); track modulo.nombre) {
            <fieldset class="rounded-xl border border-border p-3">
              <legend class="flex items-center gap-2 px-1">
                <span class="text-sm font-semibold capitalize text-fg">{{ modulo.nombre }}</span>
                <button type="button" class="text-xs font-medium text-brand-700 dark:text-brand-300"
                        (click)="alternarModulo(modulo.permisos)">
                  seleccionar todo
                </button>
              </legend>
              <div class="grid gap-1 sm:grid-cols-2">
                @for (permiso of modulo.permisos; track permiso.id) {
                  <label class="flex items-start gap-2 py-0.5">
                    <input type="checkbox" class="mt-1 size-4 accent-brand-600"
                           [checked]="seleccionados().has(permiso.id)"
                           (change)="alternarPermiso(permiso.id)" />
                    <span>
                      <span class="block text-sm text-fg">{{ permiso.descripcion }}</span>
                      <code class="block text-[11px] text-fg-subtle">{{ permiso.codigo }}</code>
                    </span>
                  </label>
                }
              </div>
            </fieldset>
          }
        </div>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar rol</ui-button>
      </div>
    </ui-modal>
  `,
})
export class RolesComponent {
  private readonly servicio = inject(RolAdminService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly roles = signal<Rol[]>([]);
  protected readonly modulos = signal<{ nombre: string; permisos: Permiso[] }[]>([]);
  protected readonly cargando = signal(true);
  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Rol | null>(null);
  protected readonly guardando = signal(false);
  protected readonly seleccionados = signal<Set<number>>(new Set());

  protected readonly formulario = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.pattern(/^[A-Z_]+$/)]],
    descripcion: [''],
  });

  constructor() {
    this.cargar();
    this.servicio.permisosPorModulo().subscribe((mapa) => {
      this.modulos.set(Object.entries(mapa).map(([nombre, permisos]) => ({ nombre, permisos })));
    });
  }

  protected agrupar(permisos: Permiso[]): { modulo: string; permisos: Permiso[] }[] {
    const mapa = new Map<string, Permiso[]>();
    for (const permiso of permisos) {
      const lista = mapa.get(permiso.modulo) ?? [];
      lista.push(permiso);
      mapa.set(permiso.modulo, lista);
    }
    return [...mapa.entries()].map(([modulo, lista]) => ({ modulo, permisos: lista }));
  }

  protected errorNombre(): string {
    const control = this.formulario.controls.nombre;
    if (control.valid || !(control.dirty || control.touched)) {
      return '';
    }
    return control.hasError('pattern')
      ? 'Use solo mayusculas y guion bajo' : 'El nombre es obligatorio';
  }

  protected alternarPermiso(id: number): void {
    this.seleccionados.update((actual) => {
      const copia = new Set(actual);
      copia.has(id) ? copia.delete(id) : copia.add(id);
      return copia;
    });
  }

  protected alternarModulo(permisos: Permiso[]): void {
    const todos = permisos.every((p) => this.seleccionados().has(p.id));
    this.seleccionados.update((actual) => {
      const copia = new Set(actual);
      permisos.forEach((p) => (todos ? copia.delete(p.id) : copia.add(p.id)));
      return copia;
    });
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset();
    this.seleccionados.set(new Set());
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(rol: Rol): void {
    this.editando.set(rol);
    this.formulario.patchValue({ nombre: rol.nombre, descripcion: rol.descripcion ?? '' });
    this.seleccionados.set(new Set(rol.permisos.map((p) => p.id)));
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid || !this.seleccionados().size) {
      this.formulario.markAllAsTouched();
      this.notificacion.advertencia('Seleccione al menos un permiso');
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: RolRequest = {
      nombre: valores.nombre,
      descripcion: valores.descripcion || null,
      permisoIds: [...this.seleccionados()],
    };
    this.guardando.set(true);
    const editando = this.editando();
    const peticion = editando
      ? this.servicio.actualizar(editando.id, cuerpo)
      : this.servicio.crear(cuerpo);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.notificacion.exito('Rol guardado', cuerpo.nombre);
        this.cargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async eliminar(rol: Rol): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Eliminar rol',
      mensaje: `Se eliminara el rol ${rol.nombre}. Solo es posible si no tiene usuarios asignados.`,
      textoConfirmar: 'Eliminar',
      peligroso: true,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.eliminar(rol.id).subscribe(() => {
      this.notificacion.exito('Rol eliminado');
      this.cargar();
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.servicio.listar().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
