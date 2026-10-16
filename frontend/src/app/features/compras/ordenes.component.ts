import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  OrdenCompra,
  OrdenCompraRequest,
  Recepcion,
  Requerimiento,
} from '../../core/models/compras.model';
import { ProductoResumen } from '../../core/models/catalogo.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import { CatalogoRefService } from '../../shared/services/catalogo-ref.service';
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
import { OrdenCompraService, RequerimientoService } from './compras.service';

/** IGV vigente en Peru; el servidor recalcula los totales al guardar. */
const TASA_IGV = 0.18;

/**
 * Ordenes de compra (HU-28, HU-29). La orden nace de un requerimiento aprobado
 * para que exista trazabilidad "necesidad -> compra -> recepcion", y la
 * aprobacion esta separada del registro (segregacion de funciones, RNF-05).
 */
@Component({
  selector: 'sgai-ordenes-compra',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    SelectorProductoComponent, CantidadPipe, MonedaPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Ordenes de compra"
                    descripcion="Emision contra requerimiento aprobado, aprobacion y seguimiento de lo pendiente de recibir.">
      <div acciones>
        <ui-button *hasPermiso="'compras:orden:crear'" (presionado)="abrirNueva()">
          Nueva orden
        </ui-button>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-4">
        <label class="block">
          <span class="sgai-label">Buscar</span>
          <input class="sgai-input" type="search" placeholder="Numero de orden o proveedor"
                 (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
        </label>
        <label class="block">
          <span class="sgai-label">Estado</span>
          <select class="sgai-input"
                  (change)="lista.filtrar({ estado: $any($event.target).value || null })">
            <option value="">Todos</option>
            <option value="BORRADOR">Borrador</option>
            <option value="APROBADA">Aprobada</option>
            <option value="RECIBIDA_PARCIAL">Recibida parcial</option>
            <option value="RECIBIDA">Recibida</option>
            <option value="RECHAZADA">Rechazada</option>
            <option value="ANULADA">Anulada</option>
          </select>
        </label>
        <label class="block">
          <span class="sgai-label">Proveedor</span>
          <select class="sgai-input"
                  (change)="lista.filtrar({ proveedorId: $any($event.target).value || null })">
            <option value="">Todos</option>
            @for (proveedor of catalogos.proveedores(); track proveedor.id) {
              <option [value]="proveedor.id">{{ proveedor.razonSocial }}</option>
            }
          </select>
        </label>
        <label class="flex items-end gap-2 pb-1">
          <input type="checkbox" class="size-4 rounded border-border accent-brand-600"
                 [checked]="soloAlmacen()" (change)="alternarAlmacen($any($event.target).checked)" />
          <span class="text-sm text-fg-muted">Solo {{ contexto.seleccionado()?.codigo ?? 'almacen actual' }}</span>
        </label>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin ordenes de compra"
                  descripcionVacio="Emita una orden desde un requerimiento aprobado.">
          <ng-template #fila let-orden>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ orden.numero }}</p>
              <p class="text-xs text-fg-muted">
                {{ orden.fechaEmision | fechaSgai }}
                @if (orden.requerimientoNumero) { · ref. {{ orden.requerimientoNumero }} }
              </p>
            </td>
            <td class="px-3 py-2.5">
              <p class="text-sm text-fg">{{ orden.proveedorRazonSocial }}</p>
              <p class="font-mono text-xs text-fg-muted">RUC {{ orden.proveedorRuc }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-sm text-fg-muted lg:table-cell">
              {{ orden.almacenDestinoNombre }}
            </td>
            <td class="hidden px-3 py-2.5 text-sm lg:table-cell">
              @if (orden.fechaEsperada) {
                <span [class.text-danger]="atrasada(orden)">{{ orden.fechaEsperada | fechaSgai }}</span>
              } @else {
                <span class="text-fg-subtle">—</span>
              }
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">
              <p class="font-medium text-fg">{{ orden.total | moneda: orden.moneda }}</p>
              <p class="text-[11px] text-fg-subtle">
                neto {{ orden.subtotal | moneda: orden.moneda }}
              </p>
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(orden.estado)">{{ etiqueta(orden.estado) }}</ui-badge>
              @if (pendienteDe(orden) > 0 && orden.estado === 'RECIBIDA_PARCIAL') {
                <p class="mt-0.5 text-[11px] text-warning">
                  pendiente {{ pendienteDe(orden) | cantidad }}
                </p>
              }
            </td>
            <td class="px-3 py-2.5 text-right whitespace-nowrap">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(orden)">Detalle</button>
              @if (orden.estado === 'BORRADOR') {
                <span *hasPermiso="'compras:orden:aprobar'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-success
                                 hover:bg-success-soft" (click)="aprobar(orden)">Aprobar</button>
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                                 hover:bg-danger-soft" (click)="rechazar(orden)">Rechazar</button>
                </span>
              }
              @if (orden.estado === 'APROBADA') {
                <span *hasPermiso="'compras:orden:aprobar'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                                 hover:bg-danger-soft" (click)="anular(orden)">Anular</button>
                </span>
              }
            </td>
          </ng-template>

          <ng-template #tarjeta let-orden>
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-mono text-sm font-medium text-fg">{{ orden.numero }}</p>
                <p class="text-xs text-fg-muted">{{ orden.proveedorRazonSocial }}</p>
              </div>
              <ui-badge [tono]="tono(orden.estado)">{{ etiqueta(orden.estado) }}</ui-badge>
            </div>
            <dl class="mt-2 grid grid-cols-2 gap-2 text-xs">
              <div>
                <dt class="text-fg-subtle">Emision</dt>
                <dd class="text-fg">{{ orden.fechaEmision | fechaSgai }}</dd>
              </div>
              <div>
                <dt class="text-fg-subtle">Total</dt>
                <dd class="font-medium tabular-nums text-fg">
                  {{ orden.total | moneda: orden.moneda }}
                </dd>
              </div>
            </dl>
            <div class="mt-2 flex gap-2">
              <ui-button tamano="sm" variante="secundario" (presionado)="verDetalle(orden)">
                Detalle
              </ui-button>
              @if (orden.estado === 'BORRADOR') {
                <span *hasPermiso="'compras:orden:aprobar'">
                  <ui-button tamano="sm" (presionado)="aprobar(orden)">Aprobar</ui-button>
                </span>
              }
            </div>
          </ng-template>
        </ui-table>

        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <!-- Nueva orden -->
    <ui-modal [abierto]="modalAbierto()" tamano="xl" titulo="Nueva orden de compra"
              subtitulo="Seleccione un requerimiento aprobado para arrastrar sus lineas o arme la orden manualmente."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <label class="block">
            <span class="sgai-label">Requerimiento aprobado</span>
            <select class="sgai-input"
                    (change)="cargarRequerimiento($any($event.target).value)">
              <option value="">Sin requerimiento (compra directa)</option>
              @for (requerimiento of requerimientosAprobados(); track requerimiento.id) {
                <option [value]="requerimiento.id">
                  {{ requerimiento.numero }} · {{ requerimiento.almacenNombre }}
                </option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Proveedor <span class="text-danger">*</span></span>
            <select class="sgai-input" formControlName="proveedorId">
              <option [value]="null">Seleccione...</option>
              @for (proveedor of catalogos.proveedores(); track proveedor.id) {
                <option [value]="proveedor.id">
                  {{ proveedor.razonSocial }}
                  @if (proveedor.leadTimeDias) { · entrega {{ proveedor.leadTimeDias }} d }
                </option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Almacen destino <span class="text-danger">*</span></span>
            <select class="sgai-input" formControlName="almacenDestinoId">
              @for (almacen of contexto.almacenes(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Fecha esperada de entrega</span>
            <input class="sgai-input" type="date" formControlName="fechaEsperada" />
          </label>
          <label class="block">
            <span class="sgai-label">Condicion de pago</span>
            <input class="sgai-input" formControlName="condicionPago"
                   placeholder="Credito 30 dias" maxlength="60" />
          </label>
          <label class="block">
            <span class="sgai-label">Moneda</span>
            <select class="sgai-input" formControlName="moneda">
              <option value="PEN">PEN · Soles</option>
              <option value="USD">USD · Dolares</option>
            </select>
          </label>
        </div>

        <div class="rounded-xl border border-border">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border
                      px-3 py-2">
            <h3 class="text-sm font-semibold text-fg">Lineas de la orden</h3>
            <div class="w-full sm:w-80">
              <sgai-selector-producto etiqueta="" [excluidos]="productosEnLineas()"
                                      (seleccionado)="agregarLinea($event)" />
            </div>
          </div>

          @if (!lineas.length) {
            <p class="px-3 py-6 text-center text-sm text-fg-muted">
              Agregue productos o seleccione un requerimiento aprobado.
            </p>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full min-w-[42rem] text-sm">
                <thead>
                  <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                    <th class="px-3 py-2 font-semibold">Producto</th>
                    <th class="px-3 py-2 text-right font-semibold">Cantidad</th>
                    <th class="px-3 py-2 text-right font-semibold">P. unitario</th>
                    <th class="px-3 py-2 text-right font-semibold">Importe</th>
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
                        <input class="sgai-input w-24 py-1.5 text-right text-xs tabular-nums"
                               type="number" step="0.0001" min="0.0001"
                               formControlName="cantidad" (input)="recalcular()" />
                      </td>
                      <td class="px-3 py-2">
                        <input class="sgai-input w-28 py-1.5 text-right text-xs tabular-nums"
                               type="number" step="0.0001" min="0"
                               formControlName="precioUnitario" (input)="recalcular()" />
                      </td>
                      <td class="px-3 py-2 text-right text-xs font-medium tabular-nums text-fg">
                        {{ importeDe(grupo) | moneda: monedaActual() }}
                      </td>
                      <td class="px-3 py-2 text-right">
                        <button type="button" aria-label="Quitar linea"
                                class="rounded-lg p-1.5 text-danger hover:bg-danger-soft"
                                (click)="quitarLinea(i)">×</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="flex justify-end border-t border-border px-3 py-3">
              <dl class="w-56 space-y-1 text-sm">
                <div class="flex justify-between">
                  <dt class="text-fg-muted">Subtotal</dt>
                  <dd class="tabular-nums text-fg">{{ subtotal() | moneda: monedaActual() }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-fg-muted">IGV (18 %)</dt>
                  <dd class="tabular-nums text-fg">{{ igv() | moneda: monedaActual() }}</dd>
                </div>
                <div class="flex justify-between border-t border-border pt-1 font-semibold">
                  <dt class="text-fg">Total</dt>
                  <dd class="tabular-nums text-fg">{{ total() | moneda: monedaActual() }}</dd>
                </div>
              </dl>
            </div>
          }
        </div>

        <label class="block">
          <span class="sgai-label">Observacion</span>
          <textarea class="sgai-input" rows="2" formControlName="observacion"
                    placeholder="Indicaciones de entrega, cadena de frio, horario de recepcion..."></textarea>
        </label>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineas.length"
                   (presionado)="guardar()">Emitir orden</ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="xl"
              [titulo]="'Orden ' + (detalle()?.numero ?? '')"
              [subtitulo]="(detalle()?.proveedorRazonSocial ?? '')
                + (detalle()?.condicionPago ? ' · ' + detalle()?.condicionPago : '')"
              (cerrado)="cerrarDetalle()">
      @let orden = detalle();
      @if (orden) {
        <dl class="mb-4 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
          <div>
            <dt class="text-xs text-fg-subtle">Emision</dt>
            <dd class="text-fg">{{ orden.fechaEmision | fechaSgai }}</dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Entrega esperada</dt>
            <dd class="text-fg">{{ orden.fechaEsperada ? (orden.fechaEsperada | fechaSgai) : '—' }}</dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Destino</dt>
            <dd class="text-fg">{{ orden.almacenDestinoNombre }}</dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Aprobada por</dt>
            <dd class="text-fg">{{ orden.aprobador || 'pendiente' }}</dd>
          </div>
        </dl>

        <div class="overflow-x-auto">
          <table class="w-full min-w-[36rem] text-sm">
            <thead>
              <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                <th class="py-2 pr-3 font-semibold">Producto</th>
                <th class="py-2 pr-3 text-right font-semibold">Pedido</th>
                <th class="py-2 pr-3 text-right font-semibold">Recibido</th>
                <th class="py-2 pr-3 text-right font-semibold">Pendiente</th>
                <th class="py-2 pr-3 text-right font-semibold">P. unitario</th>
                <th class="py-2 text-right font-semibold">Importe</th>
              </tr>
            </thead>
            <tbody>
              @for (linea of orden.lineas; track linea.id) {
                <tr class="border-b border-border/60 last:border-0">
                  <td class="py-2 pr-3">
                    <p class="text-fg">{{ linea.productoNombre }}</p>
                    <p class="font-mono text-xs text-fg-muted">
                      {{ linea.productoCodigo }} · {{ linea.unidadMedida }}
                      @if (linea.controlaLote) { · exige lote }
                    </p>
                  </td>
                  <td class="py-2 pr-3 text-right tabular-nums">{{ linea.cantidad | cantidad }}</td>
                  <td class="py-2 pr-3 text-right tabular-nums text-fg-muted">
                    {{ linea.cantidadRecibida | cantidad }}
                  </td>
                  <td class="py-2 pr-3 text-right tabular-nums"
                      [class.text-warning]="linea.pendiente > 0">
                    {{ linea.pendiente | cantidad }}
                  </td>
                  <td class="py-2 pr-3 text-right tabular-nums text-fg-muted">
                    {{ linea.precioUnitario | moneda: orden.moneda }}
                  </td>
                  <td class="py-2 text-right tabular-nums">
                    {{ linea.importe | moneda: orden.moneda }}
                  </td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr class="border-t border-border">
                <td colspan="5" class="py-2 pr-3 text-right text-xs uppercase text-fg-muted">
                  Subtotal / IGV / Total
                </td>
                <td class="py-2 text-right text-sm font-semibold tabular-nums text-fg">
                  {{ orden.subtotal | moneda: orden.moneda }} ·
                  {{ orden.igv | moneda: orden.moneda }} ·
                  {{ orden.total | moneda: orden.moneda }}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        <h3 class="mt-4 text-sm font-semibold text-fg">Recepciones registradas</h3>
        @if (!recepciones().length) {
          <p class="mt-1 text-sm text-fg-muted">Aun no se recibio mercaderia contra esta orden.</p>
        } @else {
          <ul class="mt-2 space-y-2">
            @for (recepcion of recepciones(); track recepcion.id) {
              <li class="rounded-lg border border-border px-3 py-2 text-sm">
                <div class="flex flex-wrap items-center justify-between gap-2">
                  <span class="font-mono text-xs font-medium text-fg">{{ recepcion.numero }}</span>
                  <ui-badge [tono]="tono(recepcion.resultadoCalidad)">
                    {{ recepcion.resultadoDescripcion }}
                  </ui-badge>
                </div>
                <p class="mt-1 text-xs text-fg-muted">
                  {{ recepcion.fecha | fechaSgai: 'hora' }}
                  @if (recepcion.guiaRemision) { · guia {{ recepcion.guiaRemision }} }
                  @if (recepcion.factura) { · factura {{ recepcion.factura }} }
                  @if (recepcion.temperaturaLlegada !== null && recepcion.temperaturaLlegada !== undefined) {
                    · {{ recepcion.temperaturaLlegada }} °C
                  }
                </p>
              </li>
            }
          </ul>
        }

        @if (orden.observacion) {
          <p class="mt-3 rounded-lg bg-surface-muted px-3 py-2 text-sm text-fg-muted">
            {{ orden.observacion }}
          </p>
        }
      }
      <div pie>
        <ui-button variante="secundario" (presionado)="cerrarDetalle()">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class OrdenesComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  protected readonly catalogos = inject(CatalogoRefService);
  private readonly servicio = inject(OrdenCompraService);
  private readonly requerimientos = inject(RequerimientoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly soloAlmacen = signal(true);
  protected readonly modalAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly detalle = signal<OrdenCompra | null>(null);
  protected readonly recepciones = signal<Recepcion[]>([]);
  protected readonly requerimientosAprobados = signal<Requerimiento[]>([]);
  /** Se incrementa al editar cantidades o precios para recalcular los totales. */
  private readonly revision = signal(0);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Orden', ordenPor: 'numero' },
    { clave: 'proveedor', titulo: 'Proveedor' },
    { clave: 'destino', titulo: 'Destino', ocultarEn: 'tablet' },
    { clave: 'esperada', titulo: 'Entrega', ocultarEn: 'tablet' },
    { clave: 'total', titulo: 'Total', alineacion: 'der', ordenPor: 'total' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<OrdenCompra>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fechaEmision,desc' });

  protected readonly formulario = this.fb.group({
    requerimientoId: [null as number | null],
    proveedorId: [null as number | null],
    almacenDestinoId: [null as number | null],
    fechaEsperada: [''],
    condicionPago: [''],
    moneda: ['PEN'],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected readonly monedaActual = signal('PEN');

  protected readonly productosEnLineas = computed<number[]>(() => {
    this.revision();
    return this.lineas.controls.map((grupo) => Number(grupo.value.productoId));
  });

  protected readonly subtotal = computed(() => {
    this.revision();
    return this.lineas.controls.reduce(
      (acumulado, grupo) => acumulado + this.importeDe(grupo), 0);
  });

  protected readonly igv = computed(() => this.redondear(this.subtotal() * TASA_IGV));
  protected readonly total = computed(() => this.redondear(this.subtotal()) + this.igv());

  constructor() {
    this.catalogos.proveedoresActivos().subscribe();
    effect(() => {
      const almacenId = this.soloAlmacen() ? this.contexto.seleccionadoId() : null;
      this.lista.filtrar({ almacenId });
    });
    this.formulario.controls.moneda.valueChanges.subscribe(
      (moneda) => this.monedaActual.set(moneda ?? 'PEN'));
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  protected importeDe(control: unknown): number {
    const valores = (control as FormGroup).getRawValue();
    return this.redondear(Number(valores.cantidad ?? 0) * Number(valores.precioUnitario ?? 0));
  }

  protected recalcular(): void {
    this.revision.update((valor) => valor + 1);
  }

  protected alternarAlmacen(activo: boolean): void {
    this.soloAlmacen.set(activo);
  }

  protected atrasada(orden: OrdenCompra): boolean {
    if (!orden.fechaEsperada || orden.estado === 'RECIBIDA' || orden.estado === 'ANULADA') {
      return false;
    }
    return orden.fechaEsperada < new Date().toISOString().slice(0, 10);
  }

  protected pendienteDe(orden: OrdenCompra): number {
    return (orden.lineas ?? []).reduce((acumulado, linea) => acumulado + linea.pendiente, 0);
  }

  protected abrirNueva(): void {
    this.lineas.clear();
    this.recalcular();
    this.formulario.reset({
      requerimientoId: null,
      proveedorId: null,
      almacenDestinoId: this.contexto.seleccionadoId(),
      fechaEsperada: '',
      condicionPago: 'Credito 30 dias',
      moneda: 'PEN',
      observacion: '',
    });
    this.monedaActual.set('PEN');
    this.requerimientos.buscar({ page: 0, size: 50, estado: 'APROBADO', sort: 'fecha,desc' })
      .subscribe((pagina) => this.requerimientosAprobados.set([...pagina.contenido]));
    this.modalAbierto.set(true);
  }

  /** Arrastra las lineas del requerimiento aprobado para no retipear la necesidad. */
  protected cargarRequerimiento(valor: string): void {
    const id = Number(valor);
    this.lineas.clear();
    if (!id) {
      this.formulario.patchValue({ requerimientoId: null });
      this.recalcular();
      return;
    }
    this.requerimientos.obtener(id).subscribe((requerimiento) => {
      this.formulario.patchValue({
        requerimientoId: requerimiento.id,
        almacenDestinoId: requerimiento.almacenId,
      });
      requerimiento.lineas.forEach((linea) => this.lineas.push(this.fb.group({
        productoId: [linea.productoId],
        codigo: [linea.productoCodigo],
        nombre: [linea.productoNombre],
        unidad: [linea.unidadMedida],
        cantidad: [linea.cantidadSolicitada],
        precioUnitario: [0],
      })));
      this.recalcular();
    });
  }

  protected agregarLinea(producto: ProductoResumen): void {
    this.lineas.push(this.fb.group({
      productoId: [producto.id],
      codigo: [producto.codigo],
      nombre: [producto.nombre],
      unidad: [producto.unidadMedida],
      cantidad: [1],
      precioUnitario: [0],
    }));
    this.recalcular();
  }

  protected quitarLinea(indice: number): void {
    this.lineas.removeAt(indice);
    this.recalcular();
  }

  protected guardar(): void {
    const valores = this.formulario.getRawValue();
    if (!valores.proveedorId || !valores.almacenDestinoId) {
      this.notificacion.advertencia('Indique el proveedor y el almacen destino');
      return;
    }
    if (this.lineas.controls.some((grupo) => Number(grupo.value.precioUnitario) <= 0)) {
      this.notificacion.advertencia('Todas las lineas necesitan precio unitario');
      return;
    }
    const cuerpo: OrdenCompraRequest = {
      proveedorId: Number(valores.proveedorId),
      requerimientoId: valores.requerimientoId ? Number(valores.requerimientoId) : null,
      almacenDestinoId: Number(valores.almacenDestinoId),
      fechaEsperada: valores.fechaEsperada || null,
      moneda: valores.moneda || 'PEN',
      condicionPago: valores.condicionPago || null,
      observacion: valores.observacion || null,
      lineas: this.lineas.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          productoId: Number(linea.productoId),
          cantidad: Number(linea.cantidad),
          precioUnitario: Number(linea.precioUnitario),
        };
      }),
    };
    this.guardando.set(true);
    this.servicio.crear(cuerpo).subscribe({
      next: (orden) => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.notificacion.exito('Orden emitida', `${orden.numero} · requiere aprobacion`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async aprobar(orden: OrdenCompra): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Aprobar ${orden.numero}`,
      mensaje: `Se compromete ${this.formatear(orden.total, orden.moneda)} con `
        + `${orden.proveedorRazonSocial}. Quien registro la orden no puede aprobarla.`,
      textoConfirmar: 'Aprobar orden',
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.aprobar(orden.id).subscribe(() => {
      this.notificacion.exito('Orden aprobada', orden.numero);
      this.lista.recargar();
    });
  }

  protected async rechazar(orden: OrdenCompra): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Rechazar ${orden.numero}`,
      mensaje: 'Indique por que no se aprueba la orden.',
      peligroso: true,
      pedirMotivo: true,
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.rechazar(orden.id, respuesta.motivo).subscribe(() => {
      this.notificacion.exito('Orden rechazada', orden.numero);
      this.lista.recargar();
    });
  }

  protected async anular(orden: OrdenCompra): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Anular ${orden.numero}`,
      mensaje: 'La orden quedara sin efecto y no admitira recepciones. Indique el motivo.',
      peligroso: true,
      pedirMotivo: true,
      textoConfirmar: 'Anular',
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.anular(orden.id, respuesta.motivo).subscribe(() => {
      this.notificacion.exito('Orden anulada', orden.numero);
      this.lista.recargar();
    });
  }

  protected verDetalle(orden: OrdenCompra): void {
    this.recepciones.set([]);
    this.servicio.obtener(orden.id).subscribe((completa) => this.detalle.set(completa));
    this.servicio.recepcionesDe(orden.id).subscribe({
      next: (lista) => this.recepciones.set(lista),
      error: () => this.recepciones.set([]),
    });
  }

  protected cerrarDetalle(): void {
    this.detalle.set(null);
    this.recepciones.set([]);
  }

  private redondear(valor: number): number {
    return Math.round(valor * 100) / 100;
  }

  private formatear(valor: number, moneda: string): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: moneda })
      .format(valor);
  }
}
