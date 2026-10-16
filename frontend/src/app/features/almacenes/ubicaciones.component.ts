import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Ubicacion, UbicacionRequest } from '../../core/models/almacen.model';
import { Opcion } from '../../core/models/api.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
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
import { UbicacionService } from './almacenes.service';

/** Ubicaciones fisicas por almacen: zona, rack y nivel (HU-13). */
@Component({
  selector: 'sgai-ubicaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent, UiInputComponent,
    UiSelectComponent, UiCheckboxComponent, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Ubicaciones de almacen"
                    descripcion="El saldo se registra por ubicacion: eso hace posible el picking dirigido.">
      <div acciones>
        <span *hasPermiso="'almacen:ubicacion:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nueva ubicacion</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2">
        <select class="sgai-input" (change)="filtrarAlmacen($any($event.target).value)">
          <option value="">Todos los almacenes</option>
          @for (almacen of contexto.almacenes(); track almacen.id) {
            <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
          }
        </select>
        <input class="sgai-input" type="search" placeholder="Codigo o descripcion..."
               (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
      </div>
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()">
          <ng-template #fila let-ubicacion>
            <td class="px-3 py-2.5 font-mono text-sm font-medium text-fg">{{ ubicacion.codigo }}</td>
            <td class="px-3 py-2.5 text-fg-muted">{{ ubicacion.almacenNombre }}</td>
            <td class="hidden px-3 py-2.5 text-fg-muted sm:table-cell">
              {{ ubicacion.descripcion || '—' }}
            </td>
            <td class="hidden px-3 py-2.5 text-center lg:table-cell">
              {{ ubicacion.zona || '—' }} / {{ ubicacion.rack || '—' }} / {{ ubicacion.nivel || '—' }}
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="ubicacion.tipoUbicacion === 'CUARENTENA' ? 'advertencia'
                                 : (ubicacion.tipoUbicacion === 'FRIO' ? 'info' : 'neutral')">
                {{ ubicacion.tipoUbicacion }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <span *hasPermiso="'almacen:ubicacion:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(ubicacion)">Editar</button>
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                               hover:bg-danger-soft" (click)="eliminar(ubicacion)">Eliminar</button>
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
              [titulo]="editando() ? 'Editar ubicacion' : 'Nueva ubicacion'"
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="grid gap-3 sm:grid-cols-2">
        <ui-select etiqueta="Almacen" formControlName="almacenId" [numerico]="true"
                   [requerido]="true" [opciones]="opcionesAlmacen()" class="sm:col-span-2" />
        <ui-input etiqueta="Codigo" formControlName="codigo" [requerido]="true"
                  ayuda="Ejemplo: A-01-01" />
        <ui-select etiqueta="Tipo de ubicacion" formControlName="tipoUbicacion" [opciones]="tipos" />
        <ui-input etiqueta="Descripcion" formControlName="descripcion" class="sm:col-span-2" />
        <ui-input etiqueta="Zona" formControlName="zona" />
        <ui-input etiqueta="Rack" formControlName="rack" />
        <ui-input etiqueta="Nivel" formControlName="nivel" />
        <ui-checkbox etiqueta="Ubicacion activa" formControlName="activo" />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class UbicacionesComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(UbicacionService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tipos: Opcion<string>[] = [
    { valor: 'PICKING', etiqueta: 'Picking (preparacion de pedidos)' },
    { valor: 'RESERVA', etiqueta: 'Reserva' },
    { valor: 'FRIO', etiqueta: 'Refrigeracion 2-8 C' },
    { valor: 'CUARENTENA', etiqueta: 'Cuarentena' },
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'codigo', titulo: 'Codigo' },
    { clave: 'almacen', titulo: 'Almacen' },
    { clave: 'descripcion', titulo: 'Descripcion', ocultarEn: 'movil' },
    { clave: 'zona', titulo: 'Zona / rack / nivel', alineacion: 'centro', ocultarEn: 'tablet' },
    { clave: 'tipo', titulo: 'Tipo', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Ubicacion>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'codigo,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Ubicacion | null>(null);
  protected readonly guardando = signal(false);

  protected readonly opcionesAlmacen = computed<Opcion<number>[]>(() =>
    this.contexto.almacenes().map((a) => ({ valor: a.id, etiqueta: `${a.codigo} · ${a.nombre}` })));

  protected readonly formulario = this.fb.nonNullable.group({
    almacenId: [null as number | null, [Validators.required]],
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    descripcion: [''],
    zona: [''],
    rack: [''],
    nivel: [''],
    tipoUbicacion: ['PICKING'],
    activo: [true],
  });

  constructor() {
    this.lista.recargar();
  }

  protected filtrarAlmacen(valor: string): void {
    this.lista.filtrar({ almacenId: valor || null });
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({
      almacenId: this.contexto.seleccionadoId(),
      tipoUbicacion: 'PICKING',
      activo: true,
    });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(ubicacion: Ubicacion): void {
    this.editando.set(ubicacion);
    this.formulario.patchValue({
      almacenId: ubicacion.almacenId,
      codigo: ubicacion.codigo,
      descripcion: ubicacion.descripcion ?? '',
      zona: ubicacion.zona ?? '',
      rack: ubicacion.rack ?? '',
      nivel: ubicacion.nivel ?? '',
      tipoUbicacion: ubicacion.tipoUbicacion,
      activo: ubicacion.activo,
    });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: UbicacionRequest = {
      ...valores,
      almacenId: Number(valores.almacenId),
      tipoUbicacion: valores.tipoUbicacion as UbicacionRequest['tipoUbicacion'],
      descripcion: valores.descripcion || null,
      zona: valores.zona || null,
      rack: valores.rack || null,
      nivel: valores.nivel || null,
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
        this.notificacion.exito('Ubicacion guardada', cuerpo.codigo);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async eliminar(ubicacion: Ubicacion): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Eliminar ubicacion',
      mensaje: `Se eliminara la ubicacion ${ubicacion.codigo} de ${ubicacion.almacenNombre}.`,
      textoConfirmar: 'Eliminar',
      peligroso: true,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.eliminar(ubicacion.id).subscribe(() => {
      this.notificacion.exito('Ubicacion eliminada');
      this.lista.recargar();
    });
  }
}
