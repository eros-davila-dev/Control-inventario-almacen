import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';
import { ProductoResumen } from '../../core/models/catalogo.model';
import {
  AjusteRequest,
  IngresoRequest,
  Movimiento,
  SalidaRequest,
  SugerenciaLote,
} from '../../core/models/inventario.model';
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
import { LoteConsultaService, MovimientoConsultaService, StockConsultaService } from './inventario.service';

type TipoOperacion = 'ingreso' | 'salida' | 'ajuste';

/**
 * Registro y consulta de movimientos de inventario (HU-19 a HU-24).
 * El ingreso exige lote y vencimiento cuando el producto lo controla; la salida
 * propone lotes por FEFO y exige motivo si se elige otro; la anulacion genera un
 * movimiento inverso en lugar de borrar historia.
 */
@Component({
  selector: 'sgai-movimientos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    SelectorProductoComponent, CantidadPipe, MonedaPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Movimientos de inventario"
                    descripcion="Ingresos, salidas, ajustes y anulaciones del almacen seleccionado.">
      <div acciones>
        <span *hasPermiso="'inventario:movimiento:registrar'">
          <ui-button variante="exito" (presionado)="abrir('ingreso')">Ingreso</ui-button>
        </span>
        <span *hasPermiso="'inventario:movimiento:registrar'">
          <ui-button variante="primario" (presionado)="abrir('salida')">Salida</ui-button>
        </span>
        <span *hasPermiso="'inventario:movimiento:registrar'">
          <ui-button variante="secundario" (presionado)="abrir('ajuste')">Ajuste</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-5">
        <input class="sgai-input lg:col-span-2" type="search" placeholder="Numero o documento..."
               (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
        <select class="sgai-input" (change)="lista.filtrar({ tipo: $any($event.target).value || null })">
          <option value="">Todos los tipos</option>
          @for (tipo of tiposMovimiento; track tipo.valor) {
            <option [value]="tipo.valor">{{ tipo.etiqueta }}</option>
          }
        </select>
        <input class="sgai-input" type="date" aria-label="Desde"
               (change)="lista.filtrar({ desde: $any($event.target).value || null })" />
        <input class="sgai-input" type="date" aria-label="Hasta"
               (change)="lista.filtrar({ hasta: $any($event.target).value || null })" />
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin movimientos"
                  descripcionVacio="Registre un ingreso, salida o ajuste para verlo aqui.">
          <ng-template #fila let-movimiento>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ movimiento.numero }}</p>
              <p class="text-xs text-fg-muted">{{ movimiento.fecha | fechaSgai }}</p>
            </td>
            <td class="px-3 py-2.5">
              <ui-badge [tono]="movimiento.tipoMovimiento.startsWith('INGRESO') ? 'exito' : 'info'">
                {{ movimiento.tipoDescripcion }}
              </ui-badge>
            </td>
            <td class="hidden px-3 py-2.5 text-sm text-fg-muted sm:table-cell">
              {{ movimiento.almacenOrigenNombre }}
              @if (movimiento.almacenDestinoNombre) {
                <span class="block text-xs">→ {{ movimiento.almacenDestinoNombre }}</span>
              }
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ movimiento.documentoTipo || '—' }}
              @if (movimiento.documentoNumero) {
                <span class="block">{{ movimiento.documentoNumero }}</span>
              }
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ movimiento.areaDestino || movimiento.proveedorNombre || '—' }}
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ movimiento.valorTotal | moneda }}</td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(movimiento.estadoMovimiento)">
                {{ etiqueta(movimiento.estadoMovimiento) }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(movimiento)">Detalle</button>
              @if (movimiento.estadoMovimiento === 'REGISTRADO') {
                <span *hasPermiso="'inventario:movimiento:anular'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-danger
                                 hover:bg-danger-soft" (click)="anular(movimiento)">Anular</button>
                </span>
              }
            </td>
          </ng-template>

          <ng-template #tarjeta let-movimiento>
            <article class="sgai-card p-3">
              <div class="flex items-start justify-between gap-2">
                <div>
                  <p class="font-mono text-sm font-medium text-fg">{{ movimiento.numero }}</p>
                  <p class="text-xs text-fg-muted">{{ movimiento.fecha | fechaSgai }}</p>
                </div>
                <ui-badge [tono]="tono(movimiento.estadoMovimiento)">
                  {{ etiqueta(movimiento.estadoMovimiento) }}
                </ui-badge>
              </div>
              <p class="mt-2 text-xs text-fg-muted">{{ movimiento.tipoDescripcion }}</p>
              <p class="text-sm font-medium tabular-nums text-fg">
                {{ movimiento.valorTotal | moneda }}
              </p>
              <div class="mt-2 flex justify-end gap-2">
                <ui-button tamano="sm" variante="secundario" (presionado)="verDetalle(movimiento)">
                  Detalle
                </ui-button>
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

    <!-- Registro de movimiento -->
    <ui-modal [abierto]="!!operacion()" tamano="xl" [titulo]="tituloModal()"
              [subtitulo]="subtituloModal()" (cerrado)="cerrar()">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label class="block">
            <span class="sgai-label">Almacen</span>
            <select class="sgai-input" formControlName="almacenId">
              @for (almacen of contexto.almacenes(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>

          @if (operacion() === 'ingreso') {
            <label class="block">
              <span class="sgai-label">Tipo de ingreso</span>
              <select class="sgai-input" formControlName="tipoMovimiento">
                <option value="INGRESO_COMPRA">Compra (con guia de remision)</option>
                <option value="INGRESO_DEVOLUCION">Devolucion de area</option>
                <option value="INGRESO_AJUSTE">Ajuste / saldo inicial</option>
              </select>
            </label>
            <label class="block">
              <span class="sgai-label">Proveedor</span>
              <select class="sgai-input" formControlName="proveedorId">
                <option value="">Sin proveedor</option>
                @for (proveedor of catalogo.proveedores(); track proveedor.id) {
                  <option [value]="proveedor.id">{{ proveedor.razonSocial }}</option>
                }
              </select>
            </label>
            <label class="block">
              <span class="sgai-label">Documento</span>
              <input class="sgai-input" formControlName="documentoNumero"
                     placeholder="Guia o factura" />
            </label>
          }

          @if (operacion() === 'salida') {
            <label class="block">
              <span class="sgai-label">Tipo de salida</span>
              <select class="sgai-input" formControlName="tipoMovimiento">
                <option value="SALIDA_DESPACHO">Despacho a area</option>
                <option value="SALIDA_MERMA">Merma o rotura</option>
                <option value="SALIDA_VENCIMIENTO">Baja por vencimiento</option>
                <option value="SALIDA_DEVOLUCION_PROVEEDOR">Devolucion a proveedor</option>
              </select>
            </label>
            <label class="block">
              <span class="sgai-label">Centro de costo</span>
              <input class="sgai-input" formControlName="centroCosto" placeholder="CC-HD-SJL" />
            </label>
            <label class="block">
              <span class="sgai-label">Area destino</span>
              <input class="sgai-input" formControlName="areaDestino"
                     placeholder="Hemodialisis turno manana" />
            </label>
          }

          @if (operacion() === 'ajuste') {
            <label class="block">
              <span class="sgai-label">Motivo del ajuste</span>
              <select class="sgai-input" formControlName="motivoAjuste">
                @for (motivo of motivosAjuste; track motivo.valor) {
                  <option [value]="motivo.valor">{{ motivo.etiqueta }}</option>
                }
              </select>
            </label>
            <label class="block">
              <span class="sgai-label">Sentido</span>
              <select class="sgai-input" formControlName="incrementa">
                <option value="true">Suma inventario (sobrante)</option>
                <option value="false">Resta inventario (faltante)</option>
              </select>
            </label>
          }
        </div>

        <label class="block">
          <span class="sgai-label">
            Observacion
            @if (operacion() === 'ajuste') {
              <span class="text-danger" aria-hidden="true">*</span>
            }
          </span>
          <textarea class="sgai-input" rows="2" formControlName="observacion" maxlength="500"
                    [placeholder]="operacion() === 'ajuste'
                      ? 'Sustento del ajuste: queda en la auditoria'
                      : 'Observacion del documento'"></textarea>
        </label>

        <!-- Lineas -->
        <div class="rounded-xl border border-border">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border px-3 py-2">
            <h3 class="text-sm font-semibold text-fg">Detalle del movimiento</h3>
            <div class="w-full sm:w-80">
              <sgai-selector-producto etiqueta="" [excluidos]="productosEnLineas()"
                                      (seleccionado)="agregarLinea($event)" />
            </div>
          </div>

          @if (!lineas.length) {
            <p class="px-3 py-6 text-center text-sm text-fg-muted">
              Busque un producto arriba para agregarlo al documento.
            </p>
          } @else {
            <div class="sgai-scroll overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-border text-left text-xs uppercase tracking-wide
                             text-fg-muted">
                    <th class="px-3 py-2 font-semibold">Producto</th>
                    @if (operacion() === 'ingreso') {
                      <th class="px-3 py-2 font-semibold">Lote</th>
                      <th class="px-3 py-2 font-semibold">Vence</th>
                    } @else {
                      <th class="px-3 py-2 font-semibold">Lote</th>
                    }
                    <th class="px-3 py-2 font-semibold">Ubicacion</th>
                    <th class="px-3 py-2 text-right font-semibold">Cantidad</th>
                    @if (operacion() !== 'salida') {
                      <th class="px-3 py-2 text-right font-semibold">Costo unitario</th>
                    }
                    <th class="px-3 py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  @for (grupo of lineas.controls; track $index; let i = $index) {
                    <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                      <td class="px-3 py-2">
                        <p class="text-sm font-medium text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                        <p class="font-mono text-xs text-fg-muted">
                          {{ asGrupo(grupo).value.codigo }} · {{ asGrupo(grupo).value.unidad }}
                        </p>
                      </td>

                      @if (operacion() === 'ingreso') {
                        <td class="px-3 py-2">
                          <input class="sgai-input py-1.5 text-xs" formControlName="codigoLote"
                                 [attr.placeholder]="asGrupo(grupo).value.controlaLote
                                   ? 'Obligatorio' : 'No aplica'"
                                 [attr.disabled]="asGrupo(grupo).value.controlaLote ? null : true" />
                        </td>
                        <td class="px-3 py-2">
                          <input class="sgai-input py-1.5 text-xs" type="date"
                                 formControlName="fechaVencimiento" />
                        </td>
                      } @else {
                        <td class="px-3 py-2">
                          @if (asGrupo(grupo).value.controlaLote) {
                            <select class="sgai-input py-1.5 text-xs" formControlName="loteId">
                              <option value="">FEFO automatico</option>
                              @for (lote of lotesDe(asGrupo(grupo).value.productoId);
                                    track lote.loteId) {
                                <option [value]="lote.loteId">
                                  {{ lote.codigoLote }} · vence {{ lote.fechaVencimiento }}
                                  (disp. {{ lote.disponible }})
                                </option>
                              }
                            </select>
                            @if (asGrupo(grupo).value.loteId) {
                              <input class="sgai-input mt-1 py-1.5 text-xs"
                                     formControlName="motivoNoFefo"
                                     placeholder="Motivo si no es el lote FEFO" />
                            }
                          } @else {
                            <span class="text-xs text-fg-subtle">Sin lote</span>
                          }
                        </td>
                      }

                      <td class="px-3 py-2">
                        <select class="sgai-input py-1.5 text-xs" formControlName="ubicacionId">
                          <option value="">Sin ubicacion</option>
                          @for (ubicacion of ubicaciones(); track ubicacion.valor) {
                            <option [value]="ubicacion.valor">{{ ubicacion.etiqueta }}</option>
                          }
                        </select>
                      </td>

                      <td class="px-3 py-2">
                        <input class="sgai-input py-1.5 text-right text-xs tabular-nums"
                               type="number" step="0.0001" min="0.0001" formControlName="cantidad" />
                      </td>

                      @if (operacion() !== 'salida') {
                        <td class="px-3 py-2">
                          <input class="sgai-input py-1.5 text-right text-xs tabular-nums"
                                 type="number" step="0.000001" min="0"
                                 formControlName="costoUnitario" />
                        </td>
                      }

                      <td class="px-3 py-2 text-right">
                        <button type="button" aria-label="Quitar linea"
                                class="rounded-lg p-1.5 text-danger hover:bg-danger-soft"
                                (click)="quitarLinea(i)">
                          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                               stroke-width="1.8" stroke-linecap="round">
                            <path d="M6 6l12 12M18 6 6 18" />
                          </svg>
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            @if (operacion() !== 'salida') {
              <div class="flex justify-end border-t border-border px-3 py-2">
                <p class="text-sm">
                  <span class="text-fg-muted">Valor del documento:</span>
                  <span class="ml-2 font-semibold tabular-nums text-fg">{{ total() | moneda }}</span>
                </p>
              </div>
            }
          }
        </div>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="cerrar()">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineas.length"
                   (presionado)="registrar()">
          Registrar {{ operacion() }}
        </ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Movimiento ' + (detalle()?.numero ?? '')"
              [subtitulo]="detalle()?.tipoDescripcion ?? ''" (cerrado)="detalle.set(null)">
      @let movimiento = detalle();
      @if (movimiento) {
        <dl class="mb-4 grid gap-3 text-sm sm:grid-cols-3">
          <div><dt class="text-xs text-fg-subtle">Fecha</dt>
            <dd class="text-fg">{{ movimiento.fecha | fechaSgai }}</dd></div>
          <div><dt class="text-xs text-fg-subtle">Almacen</dt>
            <dd class="text-fg">{{ movimiento.almacenOrigenNombre }}</dd></div>
          <div><dt class="text-xs text-fg-subtle">Registrado por</dt>
            <dd class="text-fg">{{ movimiento.registradoPor || '—' }}</dd></div>
          <div><dt class="text-xs text-fg-subtle">Documento</dt>
            <dd class="text-fg">
              {{ movimiento.documentoTipo || '—' }} {{ movimiento.documentoNumero }}
            </dd></div>
          <div><dt class="text-xs text-fg-subtle">Centro de costo / area</dt>
            <dd class="text-fg">
              {{ movimiento.centroCosto || '—' }} · {{ movimiento.areaDestino || '—' }}
            </dd></div>
          <div><dt class="text-xs text-fg-subtle">Valor total</dt>
            <dd class="font-medium tabular-nums text-fg">{{ movimiento.valorTotal | moneda }}</dd></div>
          @if (movimiento.observacion) {
            <div class="sm:col-span-3"><dt class="text-xs text-fg-subtle">Observacion</dt>
              <dd class="text-fg">{{ movimiento.observacion }}</dd></div>
          }
        </dl>

        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase tracking-wide text-fg-muted">
              <th class="py-2 pr-3 font-semibold">Producto</th>
              <th class="py-2 pr-3 font-semibold">Lote</th>
              <th class="py-2 pr-3 font-semibold">Ubicacion</th>
              <th class="py-2 pr-3 text-right font-semibold">Cantidad</th>
              <th class="py-2 pr-3 text-right font-semibold">Costo</th>
              <th class="py-2 text-center font-semibold">FEFO</th>
            </tr>
          </thead>
          <tbody>
            @for (linea of movimiento.lineas; track linea.id) {
              <tr class="border-b border-border/60 last:border-0">
                <td class="py-2 pr-3">
                  <p class="text-fg">{{ linea.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ linea.productoCodigo }}</p>
                </td>
                <td class="py-2 pr-3 font-mono text-xs">
                  {{ linea.codigoLote || '—' }}
                  @if (linea.fechaVencimiento) {
                    <span class="block text-fg-muted">{{ linea.fechaVencimiento | fechaSgai }}</span>
                  }
                </td>
                <td class="py-2 pr-3 font-mono text-xs text-fg-muted">
                  {{ linea.ubicacionCodigo || '—' }}
                </td>
                <td class="py-2 pr-3 text-right tabular-nums">{{ linea.cantidad | cantidad }}</td>
                <td class="py-2 pr-3 text-right tabular-nums">{{ linea.costoTotal | moneda }}</td>
                <td class="py-2 text-center">
                  @if (linea.fefoRespetado) {
                    <ui-badge tono="exito" [punto]="false">Si</ui-badge>
                  } @else {
                    <ui-badge tono="advertencia" [punto]="false"
                              [attr.title]="linea.motivoNoFefo">Desviacion</ui-badge>
                  }
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
export class MovimientosComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  protected readonly catalogo = inject(CatalogoRefService);
  private readonly servicio = inject(MovimientoConsultaService);
  private readonly stock = inject(StockConsultaService);
  private readonly lotesServicio = inject(LoteConsultaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly tiposMovimiento: Opcion<string>[] = [
    { valor: 'INGRESO_COMPRA', etiqueta: 'Ingreso por compra' },
    { valor: 'INGRESO_DEVOLUCION', etiqueta: 'Ingreso por devolucion' },
    { valor: 'INGRESO_AJUSTE', etiqueta: 'Ingreso por ajuste' },
    { valor: 'INGRESO_TRANSFERENCIA', etiqueta: 'Ingreso por transferencia' },
    { valor: 'SALIDA_DESPACHO', etiqueta: 'Salida por despacho' },
    { valor: 'SALIDA_MERMA', etiqueta: 'Salida por merma' },
    { valor: 'SALIDA_VENCIMIENTO', etiqueta: 'Salida por vencimiento' },
    { valor: 'SALIDA_AJUSTE', etiqueta: 'Salida por ajuste' },
    { valor: 'SALIDA_TRANSFERENCIA', etiqueta: 'Salida por transferencia' },
  ];

  protected readonly motivosAjuste: Opcion<string>[] = [
    { valor: 'CONTEO_FISICO', etiqueta: 'Diferencia de conteo fisico' },
    { valor: 'MERMA', etiqueta: 'Merma operativa' },
    { valor: 'ROTURA', etiqueta: 'Rotura o dano' },
    { valor: 'VENCIMIENTO', etiqueta: 'Baja por vencimiento' },
    { valor: 'ERROR_REGISTRO', etiqueta: 'Correccion de error de registro' },
    { valor: 'DONACION', etiqueta: 'Ingreso por donacion' },
    { valor: 'SALDO_INICIAL', etiqueta: 'Carga de saldo inicial' },
    { valor: 'DEVOLUCION_AREA', etiqueta: 'Devolucion desde area' },
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Documento', ordenPor: 'numero' },
    { clave: 'tipo', titulo: 'Tipo' },
    { clave: 'almacen', titulo: 'Almacen', ocultarEn: 'movil' },
    { clave: 'documento', titulo: 'Referencia', ocultarEn: 'tablet' },
    { clave: 'destino', titulo: 'Area / proveedor', ocultarEn: 'tablet' },
    { clave: 'valor', titulo: 'Valor', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Movimiento>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fecha,desc' });

  protected readonly operacion = signal<TipoOperacion | null>(null);
  protected readonly guardando = signal(false);
  protected readonly detalle = signal<Movimiento | null>(null);
  protected readonly ubicaciones = signal<Opcion<number>[]>([]);
  protected readonly lotesPorProducto = signal<Record<number, SugerenciaLote[]>>({});

  protected readonly formulario = this.fb.group({
    almacenId: [null as number | null, [Validators.required]],
    tipoMovimiento: ['INGRESO_COMPRA'],
    proveedorId: [null as number | null],
    documentoNumero: [''],
    centroCosto: [''],
    areaDestino: [''],
    motivoAjuste: ['CONTEO_FISICO'],
    incrementa: ['true'],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected readonly productosEnLineas = computed<number[]>(() =>
    this.lineas.controls.map((grupo) => Number(grupo.value.productoId)));

  protected readonly total = computed(() => this.lineas.controls.reduce((suma, grupo) => {
    const valores = grupo.value as { cantidad?: number; costoUnitario?: number };
    return suma + Number(valores.cantidad ?? 0) * Number(valores.costoUnitario ?? 0);
  }, 0));

  protected readonly tituloModal = computed(() => {
    switch (this.operacion()) {
      case 'ingreso': return 'Registrar ingreso de inventario';
      case 'salida': return 'Registrar salida de inventario';
      case 'ajuste': return 'Registrar ajuste de inventario';
      default: return '';
    }
  });

  protected readonly subtituloModal = computed(() => {
    switch (this.operacion()) {
      case 'ingreso':
        return 'Los productos con control sanitario exigen lote y fecha de vencimiento.';
      case 'salida':
        return 'Si no indica lote, el sistema aplica FEFO: consume primero el que vence antes.';
      case 'ajuste':
        return 'El ajuste exige motivo y sustento; queda registrado en la auditoria.';
      default:
        return '';
    }
  });

  constructor() {
    this.catalogo.proveedoresActivos().subscribe();
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
      if (almacenId) {
        this.catalogo.ubicacionesDe(almacenId).subscribe((lista) =>
          this.ubicaciones.set(lista.map((u) => ({
            valor: u.id, etiqueta: `${u.codigo}${u.descripcion ? ' · ' + u.descripcion : ''}`,
          }))));
      }
    });
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  /** Lotes disponibles del producto en orden FEFO (los calcula el servidor). */
  protected lotesDe(productoId: number): SugerenciaLote[] {
    return this.lotesPorProducto()[productoId] ?? [];
  }

  protected abrir(operacion: TipoOperacion): void {
    this.operacion.set(operacion);
    this.lineas.clear();
    this.formulario.patchValue({
      almacenId: this.contexto.seleccionadoId(),
      tipoMovimiento: operacion === 'ingreso' ? 'INGRESO_COMPRA' : 'SALIDA_DESPACHO',
      proveedorId: null,
      documentoNumero: '',
      centroCosto: '',
      areaDestino: '',
      motivoAjuste: 'CONTEO_FISICO',
      incrementa: 'true',
      observacion: '',
    });
  }

  protected cerrar(): void {
    this.operacion.set(null);
  }

  protected agregarLinea(producto: ProductoResumen): void {
    this.lineas.push(this.fb.group({
      productoId: [producto.id],
      codigo: [producto.codigo],
      nombre: [producto.nombre],
      unidad: [producto.unidadMedida],
      controlaLote: [producto.controlaLote],
      codigoLote: [''],
      fechaVencimiento: [''],
      loteId: [''],
      motivoNoFefo: [''],
      ubicacionId: [''],
      cantidad: [1],
      costoUnitario: [0],
    }));

    // En las salidas se consultan al servidor los lotes disponibles (orden FEFO).
    if (this.operacion() !== 'ingreso' && producto.controlaLote) {
      const almacenId = Number(this.formulario.value.almacenId);
      if (almacenId) {
        this.stock.sugerenciaFefo(producto.id, almacenId, 0.0001).subscribe({
          next: (sugerencias) => this.lotesPorProducto.update((actual) => ({
            ...actual, [producto.id]: sugerencias,
          })),
          error: () => this.notificacion.advertencia(
            `${producto.nombre} no tiene saldo disponible en este almacen`),
        });
      }
    }
  }

  protected quitarLinea(indice: number): void {
    this.lineas.removeAt(indice);
  }

  protected registrar(): void {
    const operacion = this.operacion();
    if (!operacion || !this.lineas.length) {
      return;
    }
    const valores = this.formulario.getRawValue();
    const almacenId = Number(valores.almacenId);
    if (!almacenId) {
      this.notificacion.advertencia('Seleccione el almacen');
      return;
    }
    if (operacion === 'ajuste' && !(valores.observacion ?? '').trim()) {
      this.notificacion.advertencia('El ajuste exige una observacion de sustento');
      return;
    }

    const clave = `${operacion}-${almacenId}-${Date.now()}`;
    this.guardando.set(true);

    const finalizar = (numero: string) => {
      this.guardando.set(false);
      this.operacion.set(null);
      this.notificacion.exito('Movimiento registrado', numero);
      this.lista.recargar();
    };
    const fallar = () => this.guardando.set(false);

    if (operacion === 'ingreso') {
      const cuerpo: IngresoRequest = {
        almacenId,
        tipoMovimiento: valores.tipoMovimiento as IngresoRequest['tipoMovimiento'],
        documentoTipo: valores.tipoMovimiento === 'INGRESO_COMPRA' ? 'GUIA_REMISION' : 'ACTA',
        documentoNumero: valores.documentoNumero || null,
        proveedorId: valores.proveedorId ? Number(valores.proveedorId) : null,
        observacion: valores.observacion || null,
        lineas: this.lineas.controls.map((grupo) => {
          const linea = grupo.getRawValue();
          return {
            productoId: Number(linea.productoId),
            codigoLote: linea.controlaLote ? (linea.codigoLote || null) : null,
            fechaVencimiento: linea.fechaVencimiento || null,
            ubicacionId: linea.ubicacionId ? Number(linea.ubicacionId) : null,
            cantidad: Number(linea.cantidad),
            costoUnitario: Number(linea.costoUnitario),
          };
        }),
      };
      this.servicio.registrarIngreso(cuerpo, clave).subscribe({
        next: (movimiento) => finalizar(movimiento.numero),
        error: fallar,
      });
      return;
    }

    if (operacion === 'salida') {
      const cuerpo: SalidaRequest = {
        almacenId,
        tipoMovimiento: valores.tipoMovimiento as SalidaRequest['tipoMovimiento'],
        centroCosto: valores.centroCosto || null,
        areaDestino: valores.areaDestino || null,
        observacion: valores.observacion || null,
        lineas: this.lineas.controls.map((grupo) => {
          const linea = grupo.getRawValue();
          return {
            productoId: Number(linea.productoId),
            loteId: linea.loteId ? Number(linea.loteId) : null,
            ubicacionId: linea.ubicacionId ? Number(linea.ubicacionId) : null,
            cantidad: Number(linea.cantidad),
            motivoNoFefo: linea.motivoNoFefo || null,
          };
        }),
      };
      this.servicio.registrarSalida(cuerpo, clave).subscribe({
        next: (movimiento) => finalizar(movimiento.numero),
        error: fallar,
      });
      return;
    }

    const cuerpo: AjusteRequest = {
      almacenId,
      motivoAjuste: valores.motivoAjuste as AjusteRequest['motivoAjuste'],
      incrementa: valores.incrementa === 'true',
      observacion: valores.observacion ?? '',
      lineas: this.lineas.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          productoId: Number(linea.productoId),
          loteId: linea.loteId ? Number(linea.loteId) : null,
          ubicacionId: linea.ubicacionId ? Number(linea.ubicacionId) : null,
          cantidad: Number(linea.cantidad),
          costoUnitario: linea.costoUnitario ? Number(linea.costoUnitario) : null,
        };
      }),
    };
    this.servicio.registrarAjuste(cuerpo).subscribe({
      next: (movimiento) => finalizar(movimiento.numero),
      error: fallar,
    });
  }

  protected verDetalle(movimiento: Movimiento): void {
    this.servicio.obtener(movimiento.id).subscribe((completo) => this.detalle.set(completo));
  }

  protected async anular(movimiento: Movimiento): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Anular ${movimiento.numero}`,
      mensaje: 'La anulacion no borra el movimiento: genera un documento inverso enlazado que '
        + 'revierte el saldo. Indique el motivo para la auditoria.',
      textoConfirmar: 'Anular movimiento',
      peligroso: true,
      pedirMotivo: true,
      etiquetaMotivo: 'Motivo de la anulacion',
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.anular(movimiento.id, respuesta.motivo).subscribe((inverso) => {
      this.notificacion.exito('Movimiento anulado', `Documento inverso ${inverso.numero}`);
      this.lista.recargar();
    });
  }
}
