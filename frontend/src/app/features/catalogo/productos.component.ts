import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Opcion } from '../../core/models/api.model';
import { Producto, ProductoRequest } from '../../core/models/catalogo.model';
import { NotificacionService } from '../../core/services/notificacion.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
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
import { ProductoService } from './catalogo.service';

/**
 * Maestro de productos (HU-07, HU-11). Concentra los datos que exige el control
 * sanitario y los parametros con los que el sistema calcula la reposicion.
 */
@Component({
  selector: 'sgai-productos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent, UiInputComponent,
    UiSelectComponent, UiCheckboxComponent, UiTextareaComponent, CantidadPipe, MonedaPipe,
    FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Catalogo de productos"
                    descripcion="Insumos, medicamentos y materiales con sus datos sanitarios y parametros de reposicion.">
      <div acciones>
        <span *hasPermiso="'catalogo:producto:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nuevo producto</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <!-- Filtros -->
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-5">
        <label class="block lg:col-span-2">
          <span class="sr-only">Buscar</span>
          <input class="sgai-input" type="search" placeholder="Codigo, nombre o marca..."
                 [value]="filtroTexto()" (input)="buscar($any($event.target).value)" />
        </label>
        <select class="sgai-input" [value]="filtroCategoria() ?? ''"
                (change)="filtrarCategoria($any($event.target).value)">
          <option value="">Todas las categorias</option>
          @for (categoria of catalogo.categorias(); track categoria.id) {
            <option [value]="categoria.id">{{ categoria.nombre }}</option>
          }
        </select>
        <select class="sgai-input" [value]="filtroTipo()"
                (change)="lista.filtrar({ tipo: $any($event.target).value || null })">
          <option value="">Todos los tipos</option>
          @for (tipo of catalogo.tiposProducto; track tipo.valor) {
            <option [value]="tipo.valor">{{ tipo.etiqueta }}</option>
          }
        </select>
        <select class="sgai-input" [value]="filtroEstado()"
                (change)="filtrarEstado($any($event.target).value)">
          <option value="">Activos e inactivos</option>
          <option value="true">Solo activos</option>
          <option value="false">Solo inactivos</option>
        </select>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin productos"
                  descripcionVacio="Ajuste los filtros o registre un producto nuevo.">
          <ng-template #fila let-producto>
            <td class="px-3 py-2.5">
              <div class="flex items-start gap-2">
                <div class="min-w-0">
                  <p class="truncate font-medium text-fg">{{ producto.nombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ producto.codigo }}</p>
                </div>
                @if (producto.critico) {
                  <ui-badge tono="peligro">Critico</ui-badge>
                }
                @if (producto.requiereCadenaFrio) {
                  <ui-badge tono="info">Frio 2-8</ui-badge>
                }
              </div>
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted sm:table-cell">
              {{ producto.categoriaNombre }}
            </td>
            <td class="hidden px-3 py-2.5 lg:table-cell">{{ producto.unidadMedidaCodigo }}</td>
            <td class="hidden px-3 py-2.5 text-center lg:table-cell">
              <ui-badge [punto]="false" tono="neutral">{{ producto.clasificacionAbc }}</ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">
              <span class="text-fg-muted">{{ producto.stockMinimo | cantidad }}</span>
              <span class="mx-1 text-fg-subtle">/</span>
              <span class="font-medium">{{ producto.puntoReorden | cantidad }}</span>
              <span class="mx-1 text-fg-subtle">/</span>
              <span class="text-fg-muted">{{ producto.stockMaximo | cantidad }}</span>
            </td>
            <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
              {{ producto.costoPromedio | moneda }}
            </td>
            <td class="hidden px-3 py-2.5 lg:table-cell">
              @if (producto.registroSanitario) {
                <span class="block text-xs text-fg">{{ producto.registroSanitario }}</span>
                <span class="block text-xs"
                      [class.text-danger]="!producto.registroSanitarioVigente"
                      [class.text-fg-muted]="producto.registroSanitarioVigente">
                  {{ producto.venceRegistroSanitario | fechaSgai }}
                </span>
              } @else {
                <span class="text-xs text-fg-subtle">No aplica</span>
              }
            </td>
            <td class="px-3 py-2.5 text-right">
              <div class="flex items-center justify-end gap-1">
                <a [routerLink]="['/inventario/kardex']" [queryParams]="{ productoId: producto.id }"
                   class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                          hover:bg-surface-muted dark:text-brand-300">Kardex</a>
                <span *hasPermiso="'catalogo:producto:escribir'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                                 hover:bg-surface-muted hover:text-fg"
                          (click)="abrirEdicion(producto)">Editar</button>
                </span>
                <span *hasPermiso="'catalogo:producto:escribir'">
                  <button type="button"
                          class="rounded-lg px-2 py-1 text-xs font-medium hover:bg-surface-muted"
                          [class.text-danger]="producto.activo"
                          [class.text-success]="!producto.activo"
                          (click)="alternarEstado(producto)">
                    {{ producto.activo ? 'Desactivar' : 'Activar' }}
                  </button>
                </span>
              </div>
            </td>
          </ng-template>

          <ng-template #tarjeta let-producto>
            <article class="sgai-card p-3">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium text-fg">{{ producto.nombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ producto.codigo }}</p>
                </div>
                <ui-badge [tono]="producto.activo ? 'exito' : 'neutral'">
                  {{ producto.activo ? 'Activo' : 'Inactivo' }}
                </ui-badge>
              </div>
              <dl class="mt-2 grid grid-cols-2 gap-1.5 text-xs">
                <div><dt class="text-fg-subtle">Categoria</dt>
                  <dd class="text-fg">{{ producto.categoriaNombre }}</dd></div>
                <div><dt class="text-fg-subtle">Unidad</dt>
                  <dd class="text-fg">{{ producto.unidadMedidaCodigo }}</dd></div>
                <div><dt class="text-fg-subtle">Min / reorden / max</dt>
                  <dd class="tabular-nums text-fg">
                    {{ producto.stockMinimo | cantidad }} / {{ producto.puntoReorden | cantidad }} /
                    {{ producto.stockMaximo | cantidad }}
                  </dd></div>
                <div><dt class="text-fg-subtle">Costo promedio</dt>
                  <dd class="tabular-nums text-fg">{{ producto.costoPromedio | moneda }}</dd></div>
              </dl>
              <div class="mt-2 flex justify-end gap-2">
                <span *hasPermiso="'catalogo:producto:escribir'">
                  <ui-button tamano="sm" variante="secundario" (presionado)="abrirEdicion(producto)">
                    Editar
                  </ui-button>
                </span>
              </div>
            </article>
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
              [titulo]="editando() ? 'Editar producto' : 'Nuevo producto'"
              subtitulo="Los campos sanitarios y de reposicion alimentan las alertas y la sugerencia de compra."
              (cerrado)="cerrarModal()">
      <form [formGroup]="formulario" class="space-y-5">
        <section class="grid gap-3 sm:grid-cols-2">
          <ui-input etiqueta="Codigo interno" formControlName="codigo" [requerido]="true"
                    [error]="error('codigo')" ayuda="Identificador unico del catalogo" />
          <ui-input etiqueta="Nombre" formControlName="nombre" [requerido]="true"
                    [error]="error('nombre')" />
          <ui-textarea etiqueta="Descripcion" formControlName="descripcion" class="sm:col-span-2"
                       [filas]="2" />
          <ui-select etiqueta="Categoria" formControlName="categoriaId" [numerico]="true"
                     [requerido]="true" [opciones]="opcionesCategoria()" [error]="error('categoriaId')" />
          <ui-select etiqueta="Unidad de medida" formControlName="unidadMedidaId" [numerico]="true"
                     [requerido]="true" [opciones]="opcionesUnidad()"
                     [error]="error('unidadMedidaId')" />
          <ui-select etiqueta="Tipo de producto" formControlName="tipoProducto" [requerido]="true"
                     [opciones]="catalogo.tiposProducto" [error]="error('tipoProducto')"
                     ayuda="Los tipos sanitarios exigen lote y vencimiento" />
          <ui-input etiqueta="Presentacion" formControlName="presentacion"
                    marcador="Caja x 20, frasco 5 mL..." />
          <ui-input etiqueta="Marca" formControlName="marca" />
        </section>

        <section class="rounded-xl border border-border bg-surface-muted/60 p-3">
          <h3 class="mb-3 text-sm font-semibold text-fg">Control sanitario</h3>
          <div class="grid gap-3 sm:grid-cols-2">
            <ui-input etiqueta="Registro sanitario" formControlName="registroSanitario"
                      marcador="DM-2024-00000 / EE-00000" />
            <ui-input etiqueta="Vence registro sanitario" tipo="date"
                      formControlName="venceRegistroSanitario" />
            <ui-select etiqueta="Condicion de almacenamiento" formControlName="condicionAlmacenamiento"
                       [opciones]="catalogo.condicionesAlmacenamiento" [requerido]="true" />
            <ui-input etiqueta="Vida util minima en recepcion (dias)" tipo="number"
                      formControlName="vidaUtilMinimaDias" [minimo]="0"
                      ayuda="Se rechaza el lote cuyo vencimiento sea menor a este plazo" />
            <div class="space-y-1 sm:col-span-2">
              <ui-checkbox etiqueta="Controla lote" formControlName="controlaLote"
                           descripcion="Obligatorio en insumos y medicamentos" />
              <ui-checkbox etiqueta="Controla vencimiento" formControlName="controlaVencimiento"
                           descripcion="Habilita alertas por tramos y politica FEFO" />
              <ui-checkbox etiqueta="Insumo critico" formControlName="critico"
                           descripcion="Su quiebre suspende sesiones de hemodialisis" />
            </div>
          </div>
        </section>

        <section class="rounded-xl border border-border bg-surface-muted/60 p-3">
          <h3 class="mb-3 text-sm font-semibold text-fg">Parametros de reposicion</h3>
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <ui-input etiqueta="Stock minimo" tipo="number" formControlName="stockMinimo"
                      [minimo]="0" paso="0.0001" [requerido]="true" />
            <ui-input etiqueta="Punto de reorden" tipo="number" formControlName="puntoReorden"
                      [minimo]="0" paso="0.0001" [requerido]="true" />
            <ui-input etiqueta="Stock maximo" tipo="number" formControlName="stockMaximo"
                      [minimo]="0" paso="0.0001" [requerido]="true" />
            <ui-input etiqueta="Dias de cobertura" tipo="number" formControlName="diasCobertura"
                      [minimo]="1" [maximo]="365" />
            <ui-select etiqueta="Clasificacion ABC" formControlName="clasificacionAbc"
                       [opciones]="catalogo.clasificacionesAbc" class="lg:col-span-2" />
            <ui-checkbox etiqueta="Producto activo" formControlName="activo"
                         descripcion="Los inactivos no se pueden mover ni solicitar"
                         class="lg:col-span-2" />
          </div>
          @if (parametrosIncoherentes()) {
            <p class="mt-2 text-xs font-medium text-danger">
              Debe cumplirse: stock minimo ≤ punto de reorden ≤ stock maximo.
            </p>
          }
        </section>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="cerrarModal()">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">
          {{ editando() ? 'Guardar cambios' : 'Crear producto' }}
        </ui-button>
      </div>
    </ui-modal>
  `,
})
export class ProductosComponent {
  protected readonly catalogo = inject(CatalogoRefService);
  private readonly servicio = inject(ProductoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'nombre', titulo: 'Producto', ordenPor: 'nombre' },
    { clave: 'categoria', titulo: 'Categoria', ocultarEn: 'movil' },
    { clave: 'unidad', titulo: 'UM', ocultarEn: 'tablet' },
    { clave: 'abc', titulo: 'ABC', alineacion: 'centro', ocultarEn: 'tablet' },
    { clave: 'parametros', titulo: 'Min / reorden / max', alineacion: 'der' },
    { clave: 'costo', titulo: 'Costo prom.', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'sanitario', titulo: 'Registro sanitario', ocultarEn: 'tablet' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Producto>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'nombre,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Producto | null>(null);
  protected readonly guardando = signal(false);
  protected readonly filtroTexto = signal('');
  protected readonly filtroCategoria = signal<number | null>(null);
  protected readonly filtroTipo = signal('');
  protected readonly filtroEstado = signal('');

  protected readonly formulario = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    descripcion: [''],
    categoriaId: [null as number | null, [Validators.required]],
    unidadMedidaId: [null as number | null, [Validators.required]],
    tipoProducto: ['INSUMO_DIALISIS', [Validators.required]],
    presentacion: [''],
    marca: [''],
    registroSanitario: [''],
    venceRegistroSanitario: [''],
    condicionAlmacenamiento: ['AMBIENTE', [Validators.required]],
    controlaLote: [true],
    controlaVencimiento: [true],
    vidaUtilMinimaDias: [180],
    clasificacionAbc: ['C'],
    stockMinimo: [0],
    puntoReorden: [0],
    stockMaximo: [0],
    diasCobertura: [30],
    critico: [false],
    activo: [true],
  });

  protected readonly opcionesCategoria = computed<Opcion<number>[]>(() =>
    this.catalogo.aOpciones(this.catalogo.categorias(), (c) => `${c.codigo} · ${c.nombre}`));
  protected readonly opcionesUnidad = computed<Opcion<number>[]>(() =>
    this.catalogo.aOpciones(this.catalogo.unidades(), (u) => `${u.codigo} · ${u.nombre}`));

  protected readonly parametrosIncoherentes = computed(() => {
    const { stockMinimo, puntoReorden, stockMaximo } = this.formulario.getRawValue();
    return !(Number(stockMinimo) <= Number(puntoReorden)
      && Number(puntoReorden) <= Number(stockMaximo));
  });

  constructor() {
    this.catalogo.categoriasActivas().subscribe();
    this.catalogo.unidadesActivas().subscribe();
    this.lista.recargar();
  }

  protected error(campo: string): string {
    const control = this.formulario.get(campo);
    if (!control || control.valid || !(control.dirty || control.touched)) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Este dato es obligatorio';
    }
    if (control.hasError('maxlength')) {
      return 'Supera la longitud permitida';
    }
    return 'Valor invalido';
  }

  protected buscar(texto: string): void {
    this.filtroTexto.set(texto);
    this.lista.filtrar({ texto: texto || null });
  }

  protected filtrarCategoria(valor: string): void {
    this.filtroCategoria.set(valor ? Number(valor) : null);
    this.lista.filtrar({ categoriaId: valor || null });
  }

  protected filtrarEstado(valor: string): void {
    this.filtroEstado.set(valor);
    this.lista.filtrar({ activo: valor || null });
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({
      tipoProducto: 'INSUMO_DIALISIS',
      condicionAlmacenamiento: 'AMBIENTE',
      controlaLote: true,
      controlaVencimiento: true,
      vidaUtilMinimaDias: 180,
      clasificacionAbc: 'C',
      stockMinimo: 0,
      puntoReorden: 0,
      stockMaximo: 0,
      diasCobertura: 30,
      critico: false,
      activo: true,
    });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(producto: Producto): void {
    this.editando.set(producto);
    this.formulario.patchValue({
      codigo: producto.codigo,
      nombre: producto.nombre,
      descripcion: producto.descripcion ?? '',
      categoriaId: producto.categoriaId,
      unidadMedidaId: producto.unidadMedidaId,
      tipoProducto: producto.tipoProducto,
      presentacion: producto.presentacion ?? '',
      marca: producto.marca ?? '',
      registroSanitario: producto.registroSanitario ?? '',
      venceRegistroSanitario: producto.venceRegistroSanitario ?? '',
      condicionAlmacenamiento: producto.condicionAlmacenamiento,
      controlaLote: producto.controlaLote,
      controlaVencimiento: producto.controlaVencimiento,
      vidaUtilMinimaDias: producto.vidaUtilMinimaDias,
      clasificacionAbc: producto.clasificacionAbc,
      stockMinimo: producto.stockMinimo,
      puntoReorden: producto.puntoReorden,
      stockMaximo: producto.stockMaximo,
      diasCobertura: producto.diasCobertura,
      critico: producto.critico,
      activo: producto.activo,
    });
    this.modalAbierto.set(true);
  }

  protected cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.parametrosIncoherentes()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: ProductoRequest = {
      ...valores,
      categoriaId: Number(valores.categoriaId),
      unidadMedidaId: Number(valores.unidadMedidaId),
      tipoProducto: valores.tipoProducto as ProductoRequest['tipoProducto'],
      condicionAlmacenamiento:
        valores.condicionAlmacenamiento as ProductoRequest['condicionAlmacenamiento'],
      clasificacionAbc: valores.clasificacionAbc as ProductoRequest['clasificacionAbc'],
      descripcion: valores.descripcion || null,
      presentacion: valores.presentacion || null,
      marca: valores.marca || null,
      registroSanitario: valores.registroSanitario || null,
      venceRegistroSanitario: valores.venceRegistroSanitario || null,
      stockMinimo: Number(valores.stockMinimo),
      puntoReorden: Number(valores.puntoReorden),
      stockMaximo: Number(valores.stockMaximo),
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
        this.notificacion.exito(editando ? 'Producto actualizado' : 'Producto creado',
          cuerpo.nombre);
        this.catalogo.invalidar();
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async alternarEstado(producto: Producto): Promise<void> {
    const activar = !producto.activo;
    const respuesta = await this.confirmacion.preguntar({
      titulo: activar ? 'Activar producto' : 'Desactivar producto',
      mensaje: activar
        ? `${producto.nombre} volvera a estar disponible para movimientos y pedidos.`
        : `${producto.nombre} no podra moverse ni solicitarse. Su historial se conserva.`,
      textoConfirmar: activar ? 'Activar' : 'Desactivar',
      peligroso: !activar,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.cambiarEstado(producto.id, activar).subscribe(() => {
      this.notificacion.exito(activar ? 'Producto activado' : 'Producto desactivado');
      this.catalogo.invalidar();
      this.lista.recargar();
    });
  }
}
