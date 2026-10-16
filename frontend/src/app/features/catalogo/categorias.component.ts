import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Categoria, CategoriaRequest } from '../../core/models/catalogo.model';
import { Opcion } from '../../core/models/api.model';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
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
  UiTextareaComponent,
} from '../../shared/ui';
import { CategoriaService } from './catalogo.service';

/** Categorias jerarquicas del catalogo (HU-08). */
@Component({
  selector: 'sgai-categorias',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    UiInputComponent, UiSelectComponent, UiCheckboxComponent, UiTextareaComponent,
    HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Categorias del catalogo"
                    descripcion="Agrupan los productos para reportes, conteos ciclicos y valorizacion.">
      <div acciones>
        <span *hasPermiso="'catalogo:categoria:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nueva categoria</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)">
          <ng-template #fila let-categoria>
            <td class="px-3 py-2.5">
              <p class="font-medium text-fg">{{ categoria.nombre }}</p>
              <p class="font-mono text-xs text-fg-muted">{{ categoria.codigo }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted sm:table-cell">
              {{ categoria.categoriaPadreNombre || '—' }}
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted lg:table-cell">
              {{ categoria.descripcion || '—' }}
            </td>
            <td class="px-3 py-2.5 text-center">
              @if (categoria.requiereCadenaFrio) {
                <ui-badge tono="info">Cadena de frio</ui-badge>
              }
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="categoria.activo ? 'exito' : 'neutral'">
                {{ categoria.activo ? 'Activa' : 'Inactiva' }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <span *hasPermiso="'catalogo:categoria:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(categoria)">Editar</button>
                <button type="button"
                        class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                               hover:bg-danger-soft"
                        (click)="eliminar(categoria)">Eliminar</button>
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
              [titulo]="editando() ? 'Editar categoria' : 'Nueva categoria'"
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="grid gap-3 sm:grid-cols-2">
        <ui-input etiqueta="Codigo" formControlName="codigo" [requerido]="true" />
        <ui-input etiqueta="Nombre" formControlName="nombre" [requerido]="true" />
        <ui-select etiqueta="Categoria padre" formControlName="categoriaPadreId" [numerico]="true"
                   marcador="Sin categoria padre (nivel raiz)" [opciones]="opcionesPadre()" />
        <ui-textarea etiqueta="Descripcion" formControlName="descripcion" [filas]="2"
                     class="sm:col-span-2" />
        <ui-checkbox etiqueta="Requiere cadena de frio" formControlName="requiereCadenaFrio"
                     descripcion="Los productos de esta categoria se conservan entre 2 y 8 C" />
        <ui-checkbox etiqueta="Categoria activa" formControlName="activo" />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class CategoriasComponent {
  private readonly servicio = inject(CategoriaService);
  private readonly catalogo = inject(CatalogoRefService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'nombre', titulo: 'Categoria', ordenPor: 'nombre' },
    { clave: 'padre', titulo: 'Depende de', ocultarEn: 'movil' },
    { clave: 'descripcion', titulo: 'Descripcion', ocultarEn: 'tablet' },
    { clave: 'frio', titulo: 'Conservacion', alineacion: 'centro' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Categoria>(
    (parametros) => this.servicio.listar(parametros), { sort: 'nombre,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Categoria | null>(null);
  protected readonly guardando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: [''],
    categoriaPadreId: [null as number | null],
    requiereCadenaFrio: [false],
    activo: [true],
  });

  protected readonly opcionesPadre = computed<Opcion<number>[]>(() =>
    this.catalogo.aOpciones(
      this.catalogo.categorias().filter((c) => c.id !== this.editando()?.id),
      (c) => `${c.codigo} · ${c.nombre}`));

  constructor() {
    this.catalogo.categoriasActivas().subscribe();
    this.lista.recargar();
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({ requiereCadenaFrio: false, activo: true });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(categoria: Categoria): void {
    this.editando.set(categoria);
    this.formulario.patchValue({
      codigo: categoria.codigo,
      nombre: categoria.nombre,
      descripcion: categoria.descripcion ?? '',
      categoriaPadreId: categoria.categoriaPadreId ?? null,
      requiereCadenaFrio: categoria.requiereCadenaFrio,
      activo: categoria.activo,
    });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: CategoriaRequest = {
      ...valores,
      descripcion: valores.descripcion || null,
      categoriaPadreId: valores.categoriaPadreId ? Number(valores.categoriaPadreId) : null,
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
        this.notificacion.exito('Categoria guardada', cuerpo.nombre);
        this.catalogo.invalidar();
        this.catalogo.categoriasActivas().subscribe();
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async eliminar(categoria: Categoria): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Eliminar categoria',
      mensaje: `Se eliminara "${categoria.nombre}". Solo es posible si no tiene productos ni subcategorias.`,
      textoConfirmar: 'Eliminar',
      peligroso: true,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.eliminar(categoria.id).subscribe(() => {
      this.notificacion.exito('Categoria eliminada');
      this.catalogo.invalidar();
      this.lista.recargar();
    });
  }
}
