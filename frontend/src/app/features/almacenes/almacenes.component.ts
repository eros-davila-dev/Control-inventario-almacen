import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Almacen, AlmacenRequest } from '../../core/models/almacen.model';
import { Opcion } from '../../core/models/api.model';
import { NotificacionService } from '../../core/services/notificacion.service';
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
  UiSelectComponent,
  UiTableComponent,
} from '../../shared/ui';
import { AlmacenService } from './almacenes.service';

/** Red de almacenes por sede (HU-12). */
@Component({
  selector: 'sgai-almacenes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent, UiInputComponent,
    UiSelectComponent, UiCheckboxComponent, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Red de almacenes"
                    descripcion="Almacen central, sedes de hemodialisis, camara de frio, cuarentena y rechazados.">
      <div acciones>
        <a routerLink="/almacenes/ubicaciones">
          <ui-button variante="secundario">Ver ubicaciones</ui-button>
        </a>
        <span *hasPermiso="'almacen:almacen:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nuevo almacen</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)">
          <ng-template #fila let-almacen>
            <td class="px-3 py-2.5">
              <p class="font-medium text-fg">{{ almacen.nombre }}</p>
              <p class="font-mono text-xs text-fg-muted">{{ almacen.codigo }}</p>
            </td>
            <td class="px-3 py-2.5 text-fg-muted">{{ almacen.sede }}</td>
            <td class="px-3 py-2.5">
              <ui-badge [tono]="almacen.despachable ? 'marca' : 'advertencia'">
                {{ almacen.tipoAlmacenDescripcion }}
              </ui-badge>
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted lg:table-cell">
              {{ almacen.direccion || '—' }}
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="almacen.activo ? 'exito' : 'neutral'">
                {{ almacen.activo ? 'Activo' : 'Inactivo' }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <a [routerLink]="['/inventario/stock']" [queryParams]="{ almacenId: almacen.id }"
                 class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                        hover:bg-surface-muted dark:text-brand-300">Ver saldos</a>
              <span *hasPermiso="'almacen:almacen:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(almacen)">Editar</button>
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

    <ui-modal [abierto]="modalAbierto()" tamano="md"
              [titulo]="editando() ? 'Editar almacen' : 'Nuevo almacen'"
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="grid gap-3 sm:grid-cols-2">
        <ui-input etiqueta="Codigo" formControlName="codigo" [requerido]="true"
                  ayuda="Ejemplo: ALM-CEN, ALM-SJL" />
        <ui-input etiqueta="Nombre" formControlName="nombre" [requerido]="true" />
        <ui-input etiqueta="Sede" formControlName="sede" [requerido]="true"
                  ayuda="Debe coincidir con la sede de su almacen de cuarentena" />
        <ui-select etiqueta="Tipo de almacen" formControlName="tipoAlmacen" [requerido]="true"
                   [opciones]="tipos" />
        <ui-input etiqueta="Direccion" formControlName="direccion" class="sm:col-span-2" />
        <ui-input etiqueta="Telefono" formControlName="telefono" tipo="tel" />
        <ui-checkbox etiqueta="Almacen activo" formControlName="activo" />
        <ui-checkbox etiqueta="Permite saldo negativo" formControlName="permiteNegativo"
                     descripcion="Solo para casos excepcionales autorizados por el Jefe de Almacen"
                     class="sm:col-span-2" />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class AlmacenesComponent {
  private readonly servicio = inject(AlmacenService);
  private readonly notificacion = inject(NotificacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tipos: Opcion<string>[] = [
    { valor: 'CENTRAL', etiqueta: 'Central' },
    { valor: 'SEDE', etiqueta: 'Sede / clinica' },
    { valor: 'FARMACIA', etiqueta: 'Farmacia interna' },
    { valor: 'FRIO', etiqueta: 'Camara de frio' },
    { valor: 'CUARENTENA', etiqueta: 'Cuarentena sanitaria' },
    { valor: 'RECHAZADOS', etiqueta: 'Rechazados y bajas' },
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'nombre', titulo: 'Almacen', ordenPor: 'nombre' },
    { clave: 'sede', titulo: 'Sede', ordenPor: 'sede' },
    { clave: 'tipo', titulo: 'Tipo' },
    { clave: 'direccion', titulo: 'Direccion', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Almacen>(
    (parametros) => this.servicio.listar(parametros), { sort: 'nombre,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Almacen | null>(null);
  protected readonly guardando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    sede: ['', [Validators.required, Validators.maxLength(120)]],
    direccion: [''],
    tipoAlmacen: ['SEDE', [Validators.required]],
    telefono: [''],
    permiteNegativo: [false],
    activo: [true],
  });

  constructor() {
    this.lista.recargar();
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({ tipoAlmacen: 'SEDE', permiteNegativo: false, activo: true });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(almacen: Almacen): void {
    this.editando.set(almacen);
    this.formulario.patchValue({
      codigo: almacen.codigo,
      nombre: almacen.nombre,
      sede: almacen.sede,
      direccion: almacen.direccion ?? '',
      tipoAlmacen: almacen.tipoAlmacen,
      telefono: almacen.telefono ?? '',
      permiteNegativo: almacen.permiteNegativo,
      activo: almacen.activo,
    });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: AlmacenRequest = {
      ...valores,
      tipoAlmacen: valores.tipoAlmacen as AlmacenRequest['tipoAlmacen'],
      direccion: valores.direccion || null,
      telefono: valores.telefono || null,
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
        this.notificacion.exito('Almacen guardado', cuerpo.nombre);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }
}
