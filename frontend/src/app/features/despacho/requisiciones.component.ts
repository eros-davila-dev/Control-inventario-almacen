import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductoResumen } from '../../core/models/catalogo.model';
import {
  DespachoRequest,
  Requisicion,
  RequisicionRequest,
} from '../../core/models/despacho.model';
import { SugerenciaLote } from '../../core/models/inventario.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import { SelectorProductoComponent } from '../../shared/ui/selector-producto.component';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { StockConsultaService } from '../inventario/inventario.service';
import { DespachoService, RequisicionService } from './despacho.service';

/**
 * Requisiciones internas y su atencion con FEFO (HU-32 a HU-35).
 * La pantalla de atencion muestra la propuesta de lotes del servidor: el
 * almacenero ve exactamente de que lote y ubicacion debe tomar el material.
 */
@Component({
  selector: 'sgai-requisiciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    SelectorProductoComponent, CantidadPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Requisiciones internas"
                    descripcion="Pedidos de las areas asistenciales al almacen, con prioridad y centro de costo.">
      <div acciones>
        <span *hasPermiso="'despacho:requisicion:crear'">
          <ui-button (presionado)="abrirNueva()">Nueva requisicion</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-3">
        <select class="sgai-input" (change)="lista.filtrar({ estado: $any($event.target).value || null })">
          <option value="">Todos los estados</option>
          <option value="BORRADOR">Borrador</option>
          <option value="ENVIADA">Enviada</option>
          <option value="EN_ATENCION">En atencion</option>
          <option value="ATENDIDA_PARCIAL">Atendida parcial</option>
          <option value="ATENDIDA">Atendida</option>
          <option value="ANULADA">Anulada</option>
        </select>
        <label class="flex items-center gap-2 text-sm text-fg">
          <input type="checkbox" class="size-4 accent-brand-600"
                 (change)="lista.filtrar({ soloMias: $any($event.target).checked })" />
          Solo mis requisiciones
        </label>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  tituloVacio="Sin requisiciones"
                  descripcionVacio="Cuando un area solicite insumos, el pedido aparecera aqui.">
          <ng-template #fila let-requisicion>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ requisicion.numero }}</p>
              <p class="text-xs text-fg-muted">{{ requisicion.fecha | fechaSgai }}</p>
            </td>
            <td class="px-3 py-2.5 text-sm">
              <p class="text-fg">{{ requisicion.area }}</p>
              <p class="text-xs text-fg-muted">{{ requisicion.centroCosto }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted sm:table-cell">
              {{ requisicion.almacenSolicitanteNombre }}
              <span class="block">← {{ requisicion.almacenAtencionNombre }}</span>
            </td>
            <td class="px-3 py-2.5 text-center">
              @if (requisicion.prioridad === 'URGENTE') {
                <ui-badge tono="peligro">Urgente</ui-badge>
              } @else {
                <span class="text-xs text-fg-muted">Normal</span>
              }
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ requisicion.lineas.length }}</td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(requisicion.estado)">
                {{ etiqueta(requisicion.estado) }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(requisicion)">Detalle</button>
              @if (requisicion.estado === 'BORRADOR') {
                <span *hasPermiso="'despacho:requisicion:crear'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-success
                                 hover:bg-success-soft" (click)="enviar(requisicion)">Enviar</button>
                </span>
              }
              @if (requisicion.estado === 'ENVIADA' || requisicion.estado === 'EN_ATENCION'
                   || requisicion.estado === 'ATENDIDA_PARCIAL') {
                <span *hasPermiso="'despacho:requisicion:atender'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                                 hover:bg-surface-muted dark:text-brand-300"
                          (click)="abrirAtencion(requisicion)">Atender</button>
                </span>
              }
            </td>
          </ng-template>

          <ng-template #tarjeta let-requisicion>
            <article class="sgai-card p-3">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="font-mono text-sm font-medium text-fg">{{ requisicion.numero }}</p>
                  <p class="truncate text-xs text-fg-muted">{{ requisicion.area }}</p>
                </div>
                <ui-badge [tono]="tono(requisicion.estado)">
                  {{ etiqueta(requisicion.estado) }}
                </ui-badge>
              </div>
              <p class="mt-1 text-xs text-fg-muted">
                {{ requisicion.lineas.length }} item(s) ·
                {{ requisicion.prioridad === 'URGENTE' ? 'urgente' : 'normal' }}
              </p>
              <div class="mt-2 flex justify-end gap-2">
                <ui-button tamano="sm" variante="secundario" (presionado)="verDetalle(requisicion)">
                  Detalle
                </ui-button>
                @if (requisicion.estado !== 'ATENDIDA' && requisicion.estado !== 'ANULADA') {
                  <span *hasPermiso="'despacho:requisicion:atender'">
                    <ui-button tamano="sm" (presionado)="abrirAtencion(requisicion)">Atender</ui-button>
                  </span>
                }
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

    <!-- Nueva requisicion -->
    <ui-modal [abierto]="nuevaAbierta()" tamano="lg" titulo="Nueva requisicion interna"
              subtitulo="Indique el area, el centro de costo y los insumos que necesita."
              (cerrado)="nuevaAbierta.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block">
            <span class="sgai-label">Almacen solicitante</span>
            <select class="sgai-input" formControlName="almacenSolicitanteId">
              @for (almacen of contexto.almacenes(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Almacen que atiende</span>
            <select class="sgai-input" formControlName="almacenAtencionId">
              @for (almacen of contexto.despachables(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Area solicitante</span>
            <input class="sgai-input" formControlName="area"
                   placeholder="Hemodialisis SJL - turno manana" />
          </label>
          <label class="block">
            <span class="sgai-label">Centro de costo</span>
            <input class="sgai-input" formControlName="centroCosto" placeholder="CC-HD-SJL" />
          </label>
          <label class="block">
            <span class="sgai-label">Prioridad</span>
            <select class="sgai-input" formControlName="prioridad">
              <option value="NORMAL">Normal</option>
              <option value="URGENTE">Urgente (sesion comprometida)</option>
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Fecha requerida</span>
            <input class="sgai-input" type="date" formControlName="fechaRequerida" />
          </label>
          <label class="block sm:col-span-2">
            <span class="sgai-label">Observacion</span>
            <textarea class="sgai-input" rows="2" formControlName="observacion"></textarea>
          </label>
        </div>

        <div class="rounded-xl border border-border">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border
                      px-3 py-2">
            <h3 class="text-sm font-semibold text-fg">Insumos solicitados</h3>
            <div class="w-full sm:w-80">
              <sgai-selector-producto etiqueta="" [excluidos]="productosEnLineas()"
                                      (seleccionado)="agregarLinea($event)" />
            </div>
          </div>
          @if (!lineas.length) {
            <p class="px-3 py-6 text-center text-sm text-fg-muted">
              Agregue los insumos que necesita el area.
            </p>
          } @else {
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                  <th class="px-3 py-2 font-semibold">Producto</th>
                  <th class="px-3 py-2 text-right font-semibold">Cantidad</th>
                  <th class="px-3 py-2 font-semibold">Observacion</th>
                  <th class="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                @for (grupo of lineas.controls; track $index; let i = $index) {
                  <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                    <td class="px-3 py-2">
                      <p class="text-sm text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                      <p class="font-mono text-xs text-fg-muted">
                        {{ asGrupo(grupo).value.codigo }} · {{ asGrupo(grupo).value.unidad }}
                      </p>
                    </td>
                    <td class="px-3 py-2">
                      <input class="sgai-input py-1.5 text-right text-xs tabular-nums" type="number"
                             step="0.0001" min="0.0001" formControlName="cantidadSolicitada" />
                    </td>
                    <td class="px-3 py-2">
                      <input class="sgai-input py-1.5 text-xs" formControlName="observacion" />
                    </td>
                    <td class="px-3 py-2 text-right">
                      <button type="button" aria-label="Quitar"
                              class="rounded-lg p-1.5 text-danger hover:bg-danger-soft"
                              (click)="lineas.removeAt(i)">×</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="nuevaAbierta.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineas.length"
                   (presionado)="crear()">Crear requisicion</ui-button>
      </div>
    </ui-modal>

    <!-- Atencion con FEFO -->
    <ui-modal [abierto]="!!enAtencion()" tamano="xl"
              [titulo]="'Atender ' + (enAtencion()?.numero ?? '')"
              subtitulo="El sistema propone los lotes por FEFO; ajuste solo si hay una razon y registrela."
              (cerrado)="enAtencion.set(null)">
      <form [formGroup]="formularioAtencion" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block">
            <span class="sgai-label">Entregado a</span>
            <input class="sgai-input" formControlName="entregadoA"
                   placeholder="Nombre de quien recibe" />
          </label>
          <label class="block">
            <span class="sgai-label">Observacion</span>
            <input class="sgai-input" formControlName="observacion" />
          </label>
        </div>

        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="px-2 py-2 font-semibold">Producto</th>
              <th class="px-2 py-2 text-right font-semibold">Solicitado</th>
              <th class="px-2 py-2 text-right font-semibold">Pendiente</th>
              <th class="px-2 py-2 text-right font-semibold">A despachar</th>
              <th class="px-2 py-2 font-semibold">Lote propuesto (FEFO)</th>
            </tr>
          </thead>
          <tbody>
            @for (grupo of lineasAtencion.controls; track $index) {
              <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                <td class="px-2 py-2">
                  <p class="text-sm text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ asGrupo(grupo).value.codigo }}</p>
                </td>
                <td class="px-2 py-2 text-right tabular-nums text-fg-muted">
                  {{ asGrupo(grupo).value.cantidadSolicitada | cantidad }}
                </td>
                <td class="px-2 py-2 text-right tabular-nums">
                  {{ asGrupo(grupo).value.pendiente | cantidad }}
                </td>
                <td class="px-2 py-2">
                  <input class="sgai-input py-1.5 text-right text-xs tabular-nums" type="number"
                         step="0.0001" min="0" formControlName="cantidad"
                         (change)="recalcularSugerencia(asGrupo(grupo))" />
                </td>
                <td class="px-2 py-2">
                  @let sugerencias = sugerenciasDe(asGrupo(grupo).value.productoId);
                  @if (sugerencias.length) {
                    <ul class="space-y-0.5 text-xs">
                      @for (sugerencia of sugerencias; track sugerencia.loteId) {
                        <li class="flex items-center gap-1.5">
                          <span class="font-mono text-fg">{{ sugerencia.codigoLote }}</span>
                          <span class="text-fg-muted">
                            vence {{ sugerencia.fechaVencimiento | fechaSgai }}
                          </span>
                          <span class="tabular-nums font-medium text-fg">
                            → {{ sugerencia.cantidadSugerida | cantidad }}
                          </span>
                          @if (sugerencia.ubicacionCodigo) {
                            <span class="font-mono text-fg-subtle">
                              ({{ sugerencia.ubicacionCodigo }})
                            </span>
                          }
                        </li>
                      }
                    </ul>
                  } @else {
                    <span class="text-xs text-fg-subtle">
                      Sin saldo disponible o producto sin control de lote
                    </span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>

        <p class="rounded-lg border border-border bg-surface-muted px-3 py-2 text-xs text-fg-muted">
          Al confirmar se genera la salida de inventario y el documento de despacho. Si lo
          despachado es menor a lo solicitado, la requisicion queda atendida parcialmente.
        </p>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="enAtencion.set(null)">Cancelar</ui-button>
        <ui-button variante="exito" [cargando]="guardando()" (presionado)="atender()">
          Confirmar despacho
        </ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Requisicion ' + (detalle()?.numero ?? '')"
              [subtitulo]="(detalle()?.area ?? '') + ' · ' + (detalle()?.centroCosto ?? '')"
              (cerrado)="detalle.set(null)">
      @let requisicion = detalle();
      @if (requisicion) {
        <dl class="mb-3 grid gap-3 text-sm sm:grid-cols-3">
          <div><dt class="text-xs text-fg-subtle">Solicitante</dt>
            <dd class="text-fg">{{ requisicion.solicitante || '—' }}</dd></div>
          <div><dt class="text-xs text-fg-subtle">Fecha requerida</dt>
            <dd class="text-fg">{{ requisicion.fechaRequerida | fechaSgai }}</dd></div>
          <div><dt class="text-xs text-fg-subtle">Estado</dt>
            <dd><ui-badge [tono]="tono(requisicion.estado)">
              {{ requisicion.estadoDescripcion }}
            </ui-badge></dd></div>
        </dl>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="py-2 pr-3 font-semibold">Producto</th>
              <th class="py-2 pr-3 text-right font-semibold">Solicitado</th>
              <th class="py-2 pr-3 text-right font-semibold">Atendido</th>
              <th class="py-2 text-right font-semibold">Pendiente</th>
            </tr>
          </thead>
          <tbody>
            @for (linea of requisicion.lineas; track linea.id) {
              <tr class="border-b border-border/60 last:border-0">
                <td class="py-2 pr-3">
                  <p class="text-fg">{{ linea.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">
                    {{ linea.productoCodigo }} · {{ linea.unidadMedida }}
                  </p>
                </td>
                <td class="py-2 pr-3 text-right tabular-nums">
                  {{ linea.cantidadSolicitada | cantidad }}
                </td>
                <td class="py-2 pr-3 text-right tabular-nums">
                  {{ linea.cantidadAtendida | cantidad }}
                </td>
                <td class="py-2 text-right tabular-nums"
                    [class.text-warning]="linea.pendiente > 0">
                  {{ linea.pendiente | cantidad }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
      <div pie>
        <ui-button variante="secundario" (presionado)="detalle.set(null)">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class RequisicionesComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(RequisicionService);
  private readonly despachoServicio = inject(DespachoService);
  private readonly stock = inject(StockConsultaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Documento' },
    { clave: 'area', titulo: 'Area / centro de costo' },
    { clave: 'almacenes', titulo: 'Solicita / atiende', ocultarEn: 'movil' },
    { clave: 'prioridad', titulo: 'Prioridad', alineacion: 'centro' },
    { clave: 'items', titulo: 'Items', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Requisicion>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fecha,desc' });

  protected readonly nuevaAbierta = signal(false);
  protected readonly enAtencion = signal<Requisicion | null>(null);
  protected readonly detalle = signal<Requisicion | null>(null);
  protected readonly guardando = signal(false);
  protected readonly sugerencias = signal<Record<number, SugerenciaLote[]>>({});

  protected readonly formulario = this.fb.group({
    almacenSolicitanteId: [null as number | null, [Validators.required]],
    almacenAtencionId: [null as number | null, [Validators.required]],
    area: ['', [Validators.required]],
    centroCosto: ['', [Validators.required]],
    prioridad: ['NORMAL'],
    fechaRequerida: [''],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected readonly formularioAtencion = this.fb.group({
    entregadoA: [''],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected get lineasAtencion(): FormArray<FormGroup> {
    return this.formularioAtencion.controls.lineas;
  }

  protected readonly productosEnLineas = computed<number[]>(() =>
    this.lineas.controls.map((grupo) => Number(grupo.value.productoId)));

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenAtencionId: almacenId });
    });
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  protected sugerenciasDe(productoId: number): SugerenciaLote[] {
    return this.sugerencias()[productoId] ?? [];
  }

  protected abrirNueva(): void {
    this.lineas.clear();
    this.formulario.patchValue({
      almacenSolicitanteId: this.contexto.seleccionadoId(),
      almacenAtencionId: this.contexto.despachables()[0]?.id ?? null,
      area: '',
      centroCosto: '',
      prioridad: 'NORMAL',
      fechaRequerida: '',
      observacion: '',
    });
    this.nuevaAbierta.set(true);
  }

  protected agregarLinea(producto: ProductoResumen): void {
    this.lineas.push(this.fb.group({
      productoId: [producto.id],
      codigo: [producto.codigo],
      nombre: [producto.nombre],
      unidad: [producto.unidadMedida],
      cantidadSolicitada: [1],
      observacion: [''],
    }));
  }

  protected crear(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.notificacion.advertencia('Complete el area y el centro de costo');
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: RequisicionRequest = {
      almacenSolicitanteId: Number(valores.almacenSolicitanteId),
      almacenAtencionId: Number(valores.almacenAtencionId),
      area: valores.area ?? '',
      centroCosto: valores.centroCosto ?? '',
      prioridad: valores.prioridad as RequisicionRequest['prioridad'],
      fechaRequerida: valores.fechaRequerida || null,
      observacion: valores.observacion || null,
      lineas: this.lineas.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          productoId: Number(linea.productoId),
          cantidadSolicitada: Number(linea.cantidadSolicitada),
          observacion: linea.observacion || null,
        };
      }),
    };
    this.guardando.set(true);
    this.servicio.crear(cuerpo).subscribe({
      next: (requisicion) => {
        this.guardando.set(false);
        this.nuevaAbierta.set(false);
        this.notificacion.exito('Requisicion creada',
          `${requisicion.numero}. Enviela al almacen para su atencion.`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected enviar(requisicion: Requisicion): void {
    this.servicio.enviar(requisicion.id).subscribe(() => {
      this.notificacion.exito('Requisicion enviada', requisicion.numero);
      this.lista.recargar();
    });
  }

  protected abrirAtencion(requisicion: Requisicion): void {
    this.servicio.obtener(requisicion.id).subscribe((completa) => {
      this.enAtencion.set(completa);
      this.lineasAtencion.clear();
      this.sugerencias.set({});
      completa.lineas
        .filter((linea) => linea.pendiente > 0)
        .forEach((linea) => {
          this.lineasAtencion.push(this.fb.group({
            requisicionLineaId: [linea.id],
            productoId: [linea.productoId],
            codigo: [linea.productoCodigo],
            nombre: [linea.productoNombre],
            cantidadSolicitada: [linea.cantidadSolicitada],
            pendiente: [linea.pendiente],
            cantidad: [linea.pendiente],
            controlaLote: [linea.controlaLote],
          }));
          if (linea.controlaLote) {
            this.pedirSugerencia(completa.almacenAtencionId, linea.productoId, linea.pendiente);
          }
        });
      if (completa.estado === 'ENVIADA') {
        this.servicio.marcarEnAtencion(completa.id).subscribe();
      }
    });
  }

  /** La propuesta de lotes la calcula el servidor con la politica FEFO. */
  private pedirSugerencia(almacenId: number, productoId: number, cantidad: number): void {
    if (cantidad <= 0) {
      return;
    }
    this.stock.sugerenciaFefo(productoId, almacenId, cantidad).subscribe({
      next: (lista) => this.sugerencias.update((actual) => ({ ...actual, [productoId]: lista })),
      error: () => this.sugerencias.update((actual) => ({ ...actual, [productoId]: [] })),
    });
  }

  protected recalcularSugerencia(grupo: FormGroup): void {
    const requisicion = this.enAtencion();
    const valores = grupo.getRawValue();
    if (!requisicion || !valores.controlaLote) {
      return;
    }
    this.pedirSugerencia(requisicion.almacenAtencionId, Number(valores.productoId),
      Number(valores.cantidad));
  }

  protected atender(): void {
    const requisicion = this.enAtencion();
    if (!requisicion) {
      return;
    }
    const valores = this.formularioAtencion.getRawValue();
    const lineas = this.lineasAtencion.controls
      .map((grupo) => grupo.getRawValue())
      .filter((linea) => Number(linea.cantidad) > 0)
      .map((linea) => ({
        requisicionLineaId: Number(linea.requisicionLineaId),
        cantidad: Number(linea.cantidad),
      }));

    if (!lineas.length) {
      this.notificacion.advertencia('Indique al menos una cantidad a despachar');
      return;
    }

    const cuerpo: DespachoRequest = {
      requisicionId: requisicion.id,
      entregadoA: valores.entregadoA || null,
      observacion: valores.observacion || null,
      lineas,
    };
    this.guardando.set(true);
    this.despachoServicio.atender(cuerpo).subscribe({
      next: (despacho) => {
        this.guardando.set(false);
        this.enAtencion.set(null);
        const desviaciones = despacho.lineas.filter((l) => !l.fefoRespetado).length;
        this.notificacion.exito('Despacho registrado',
          `${despacho.numero}${desviaciones ? ` · ${desviaciones} desviacion(es) FEFO` : ''}`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected verDetalle(requisicion: Requisicion): void {
    this.servicio.obtener(requisicion.id).subscribe((completa) => this.detalle.set(completa));
  }

  protected async anular(requisicion: Requisicion): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Anular ${requisicion.numero}`,
      mensaje: 'La requisicion quedara anulada y no podra atenderse.',
      peligroso: true,
      pedirMotivo: true,
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.anular(requisicion.id, respuesta.motivo).subscribe(() => {
      this.notificacion.exito('Requisicion anulada');
      this.lista.recargar();
    });
  }
}
