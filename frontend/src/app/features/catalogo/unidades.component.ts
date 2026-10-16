import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';
import { UnidadMedida, UnidadMedidaRequest } from '../../core/models/catalogo.model';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { CatalogoRefService } from '../../shared/services/catalogo-ref.service';
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
import { UnidadMedidaService } from './catalogo.service';

/** Unidades de medida y su conversion a la unidad base (HU-09). */
@Component({
  selector: 'sgai-unidades',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent, UiInputComponent,
    UiSelectComponent, UiCheckboxComponent, CantidadPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Unidades de medida"
                    descripcion="El factor de conversion permite comprar en caja y despachar en unidad sin perder el saldo.">
      <div acciones>
        <span *hasPermiso="'catalogo:unidad:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nueva unidad</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()">
          <ng-template #fila let-unidad>
            <td class="px-3 py-2.5">
              <p class="font-medium text-fg">{{ unidad.nombre }}</p>
              <p class="font-mono text-xs text-fg-muted">{{ unidad.codigo }}</p>
            </td>
            <td class="px-3 py-2.5 text-fg-muted">{{ unidad.simbolo || '—' }}</td>
            <td class="px-3 py-2.5 text-center">
              @if (unidad.esBase) {
                <ui-badge tono="marca">Unidad base</ui-badge>
              } @else {
                <span class="text-sm text-fg-muted">{{ unidad.umBaseCodigo }}</span>
              }
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">
              {{ unidad.factorABase | cantidad: 6 }}
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="unidad.activo ? 'exito' : 'neutral'">
                {{ unidad.activo ? 'Activa' : 'Inactiva' }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <span *hasPermiso="'catalogo:unidad:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(unidad)">Editar</button>
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                               hover:bg-danger-soft" (click)="eliminar(unidad)">Eliminar</button>
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

    <ui-modal [abierto]="modalAbierto()" tamano="sm"
              [titulo]="editando() ? 'Editar unidad' : 'Nueva unidad de medida'"
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-3">
        <ui-input etiqueta="Codigo" formControlName="codigo" [requerido]="true"
                  ayuda="Ejemplo: UND, CJ20, GAL" />
        <ui-input etiqueta="Nombre" formControlName="nombre" [requerido]="true" />
        <ui-input etiqueta="Simbolo" formControlName="simbolo" marcador="und, cja, gal" />
        <ui-select etiqueta="Unidad base" formControlName="umBaseId" [numerico]="true"
                   marcador="Es una unidad base" [opciones]="opcionesBase()" />
        <ui-input etiqueta="Factor de conversion a la base" tipo="number" paso="0.000001"
                  formControlName="factorABase" [requerido]="true" [minimo]="0.000001"
                  ayuda="Una caja de 20 unidades tiene factor 20" />
        <ui-checkbox etiqueta="Unidad activa" formControlName="activo" />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class UnidadesComponent {
  private readonly servicio = inject(UnidadMedidaService);
  private readonly catalogo = inject(CatalogoRefService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'nombre', titulo: 'Unidad' },
    { clave: 'simbolo', titulo: 'Simbolo' },
    { clave: 'base', titulo: 'Convierte a', alineacion: 'centro' },
    { clave: 'factor', titulo: 'Factor', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<UnidadMedida>(
    (parametros) => this.servicio.listar(parametros), { sort: 'nombre,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<UnidadMedida | null>(null);
  protected readonly guardando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    simbolo: [''],
    umBaseId: [null as number | null],
    factorABase: [1, [Validators.required, Validators.min(0.000001)]],
    activo: [true],
  });

  protected readonly opcionesBase = computed<Opcion<number>[]>(() =>
    this.catalogo.aOpciones(
      this.catalogo.unidades().filter((u) => u.esBase && u.id !== this.editando()?.id),
      (u) => `${u.codigo} · ${u.nombre}`));

  constructor() {
    this.catalogo.unidadesActivas().subscribe();
    this.lista.recargar();
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({ factorABase: 1, activo: true });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(unidad: UnidadMedida): void {
    this.editando.set(unidad);
    this.formulario.patchValue({
      codigo: unidad.codigo,
      nombre: unidad.nombre,
      simbolo: unidad.simbolo ?? '',
      umBaseId: unidad.umBaseId ?? null,
      factorABase: unidad.factorABase,
      activo: unidad.activo,
    });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: UnidadMedidaRequest = {
      ...valores,
      simbolo: valores.simbolo || null,
      umBaseId: valores.umBaseId ? Number(valores.umBaseId) : null,
      factorABase: Number(valores.factorABase),
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
        this.notificacion.exito('Unidad guardada', cuerpo.nombre);
        this.catalogo.invalidar();
        this.catalogo.unidadesActivas().subscribe();
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async eliminar(unidad: UnidadMedida): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Eliminar unidad de medida',
      mensaje: `Se eliminara "${unidad.nombre}". Solo es posible si ningun producto la usa.`,
      textoConfirmar: 'Eliminar',
      peligroso: true,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.eliminar(unidad.id).subscribe(() => {
      this.notificacion.exito('Unidad eliminada');
      this.catalogo.invalidar();
      this.lista.recargar();
    });
  }
}
