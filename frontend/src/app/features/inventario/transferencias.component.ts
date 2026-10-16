import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';
import { ProductoResumen } from '../../core/models/catalogo.model';
import {
  RecepcionTransferenciaRequest,
  SugerenciaLote,
  Transferencia,
  TransferenciaRequest,
} from '../../core/models/inventario.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
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
import { StockConsultaService, TransferenciaConsultaService } from './inventario.service';

/**
 * Transferencias entre almacenes con confirmacion en destino (HU-22).
 * Mientras no se confirme la recepcion, la mercaderia figura en transito; la
 * diferencia entre lo enviado y lo recibido genera una merma justificada.
 */
@Component({
  selector: 'sgai-transferencias',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    SelectorProductoComponent, CantidadPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Transferencias entre almacenes"
                    descripcion="Traslados entre la sede central y las unidades de hemodialisis.">
      <div acciones>
        <span *hasPermiso="'inventario:transferencia:registrar'">
          <ui-button (presionado)="abrirEnvio()">Nueva transferencia</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="border-b border-border px-4 py-3">
        <select class="sgai-input max-w-xs"
                (change)="lista.filtrar({ estado: $any($event.target).value || null })">
          <option value="">Todos los estados</option>
          <option value="ENVIADA">En transito</option>
          <option value="RECIBIDA">Recibidas conformes</option>
          <option value="RECIBIDA_PARCIAL">Recibidas con diferencias</option>
          <option value="ANULADA">Anuladas</option>
        </select>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  tituloVacio="Sin transferencias registradas">
          <ng-template #fila let-transferencia>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ transferencia.numero }}</p>
              <p class="text-xs text-fg-muted">{{ transferencia.fechaEnvio | fechaSgai }}</p>
            </td>
            <td class="px-3 py-2.5 text-sm">
              <p class="text-fg">{{ transferencia.almacenOrigenNombre }}</p>
              <p class="text-xs text-fg-muted">→ {{ transferencia.almacenDestinoNombre }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ transferencia.guiaRemision || '—' }}
              @if (transferencia.transportista) {
                <span class="block">{{ transferencia.transportista }}</span>
              }
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ transferencia.lineas.length }}</td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(transferencia.estado)">
                {{ etiqueta(transferencia.estado) }}
              </ui-badge>
              @if (transferencia.tieneDiferencias) {
                <p class="mt-0.5 text-[11px] text-warning">con diferencias</p>
              }
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(transferencia)">Detalle</button>
              @if (transferencia.estado === 'ENVIADA') {
                <span *hasPermiso="'inventario:transferencia:recibir'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-success
                                 hover:bg-success-soft" (click)="abrirRecepcion(transferencia)">
                    Recibir
                  </button>
                </span>
              }
            </td>
          </ng-template>
        </ui-table>

        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <!-- Envio -->
    <ui-modal [abierto]="envioAbierto()" tamano="lg" titulo="Enviar transferencia"
              subtitulo="El stock sale del origen y queda en transito hasta que destino lo confirme."
              (cerrado)="envioAbierto.set(false)">
      <form [formGroup]="formularioEnvio" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block">
            <span class="sgai-label">Almacen de origen</span>
            <select class="sgai-input" formControlName="almacenOrigenId">
              @for (almacen of contexto.almacenes(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Almacen de destino</span>
            <select class="sgai-input" formControlName="almacenDestinoId">
              <option value="">Seleccione destino</option>
              @for (almacen of destinos(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Guia de remision</span>
            <input class="sgai-input" formControlName="guiaRemision" />
          </label>
          <label class="block">
            <span class="sgai-label">Transportista</span>
            <input class="sgai-input" formControlName="transportista" />
          </label>
          <label class="block sm:col-span-2">
            <span class="sgai-label">Observacion</span>
            <textarea class="sgai-input" rows="2" formControlName="observacion"></textarea>
          </label>
        </div>

        <div class="rounded-xl border border-border">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border
                      px-3 py-2">
            <h3 class="text-sm font-semibold text-fg">Productos a transferir</h3>
            <div class="w-full sm:w-80">
              <sgai-selector-producto etiqueta="" (seleccionado)="agregarLinea($event)" />
            </div>
          </div>
          @if (!lineasEnvio.length) {
            <p class="px-3 py-6 text-center text-sm text-fg-muted">
              Agregue los productos que se trasladan.
            </p>
          } @else {
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                  <th class="px-3 py-2 font-semibold">Producto</th>
                  <th class="px-3 py-2 font-semibold">Lote</th>
                  <th class="px-3 py-2 text-right font-semibold">Cantidad</th>
                  <th class="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                @for (grupo of lineasEnvio.controls; track $index; let i = $index) {
                  <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                    <td class="px-3 py-2">
                      <p class="text-sm text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                      <p class="font-mono text-xs text-fg-muted">{{ asGrupo(grupo).value.codigo }}</p>
                    </td>
                    <td class="px-3 py-2">
                      @if (asGrupo(grupo).value.controlaLote) {
                        <select class="sgai-input py-1.5 text-xs" formControlName="loteId">
                          <option value="">FEFO automatico</option>
                          @for (lote of lotesDe(asGrupo(grupo).value.productoId);
                                track lote.loteId) {
                            <option [value]="lote.loteId">
                              {{ lote.codigoLote }} (disp. {{ lote.disponible }})
                            </option>
                          }
                        </select>
                      } @else {
                        <span class="text-xs text-fg-subtle">Sin lote</span>
                      }
                    </td>
                    <td class="px-3 py-2">
                      <input class="sgai-input py-1.5 text-right text-xs tabular-nums" type="number"
                             step="0.0001" min="0.0001" formControlName="cantidad" />
                    </td>
                    <td class="px-3 py-2 text-right">
                      <button type="button" aria-label="Quitar"
                              class="rounded-lg p-1.5 text-danger hover:bg-danger-soft"
                              (click)="lineasEnvio.removeAt(i)">×</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="envioAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineasEnvio.length"
                   (presionado)="enviar()">Enviar transferencia</ui-button>
      </div>
    </ui-modal>

    <!-- Recepcion -->
    <ui-modal [abierto]="!!enRecepcion()" tamano="lg"
              [titulo]="'Recibir ' + (enRecepcion()?.numero ?? '')"
              subtitulo="Confirme lo que llego fisicamente: la diferencia se registra como merma en transito."
              (cerrado)="enRecepcion.set(null)">
      <form [formGroup]="formularioRecepcion" class="space-y-4">
        <label class="block">
          <span class="sgai-label">Ubicacion de destino</span>
          <select class="sgai-input" formControlName="ubicacionDestinoId">
            <option value="">Sin ubicacion especifica</option>
            @for (ubicacion of ubicacionesDestino(); track ubicacion.valor) {
              <option [value]="ubicacion.valor">{{ ubicacion.etiqueta }}</option>
            }
          </select>
        </label>

        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="px-2 py-2 font-semibold">Producto</th>
              <th class="px-2 py-2 font-semibold">Lote</th>
              <th class="px-2 py-2 text-right font-semibold">Enviado</th>
              <th class="px-2 py-2 text-right font-semibold">Recibido</th>
              <th class="px-2 py-2 font-semibold">Observacion</th>
            </tr>
          </thead>
          <tbody>
            @for (grupo of lineasRecepcion.controls; track $index) {
              <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                <td class="px-2 py-2">
                  <p class="text-sm text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                </td>
                <td class="px-2 py-2 font-mono text-xs">{{ asGrupo(grupo).value.codigoLote || '—' }}</td>
                <td class="px-2 py-2 text-right tabular-nums">
                  {{ asGrupo(grupo).value.cantidadEnviada | cantidad }}
                </td>
                <td class="px-2 py-2">
                  <input class="sgai-input py-1.5 text-right text-xs tabular-nums" type="number"
                         step="0.0001" min="0" formControlName="cantidadRecibida" />
                </td>
                <td class="px-2 py-2">
                  <input class="sgai-input py-1.5 text-xs" formControlName="observacion"
                         placeholder="Motivo de la diferencia" />
                </td>
              </tr>
            }
          </tbody>
        </table>

        <label class="block">
          <span class="sgai-label">
            Observacion general
            @if (hayDiferencias()) {
              <span class="text-danger" aria-hidden="true">*</span>
            }
          </span>
          <textarea class="sgai-input" rows="2" formControlName="observacion"
                    placeholder="Obligatoria cuando hay diferencias entre lo enviado y lo recibido"></textarea>
        </label>
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="enRecepcion.set(null)">Cancelar</ui-button>
        <ui-button variante="exito" [cargando]="guardando()" (presionado)="recibir()">
          Confirmar recepcion
        </ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Transferencia ' + (detalle()?.numero ?? '')"
              [subtitulo]="(detalle()?.almacenOrigenNombre ?? '') + ' → ' + (detalle()?.almacenDestinoNombre ?? '')"
              (cerrado)="detalle.set(null)">
      @let transferencia = detalle();
      @if (transferencia) {
        <dl class="mb-3 grid gap-3 text-sm sm:grid-cols-3">
          <div><dt class="text-xs text-fg-subtle">Enviada</dt>
            <dd class="text-fg">
              {{ transferencia.fechaEnvio | fechaSgai }} por {{ transferencia.enviadoPor || '—' }}
            </dd></div>
          <div><dt class="text-xs text-fg-subtle">Recibida</dt>
            <dd class="text-fg">
              {{ transferencia.fechaRecepcion ? (transferencia.fechaRecepcion | fechaSgai) : 'pendiente' }}
              @if (transferencia.recibidoPor) { por {{ transferencia.recibidoPor }} }
            </dd></div>
          <div><dt class="text-xs text-fg-subtle">Guia</dt>
            <dd class="text-fg">{{ transferencia.guiaRemision || '—' }}</dd></div>
        </dl>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="py-2 pr-3 font-semibold">Producto</th>
              <th class="py-2 pr-3 font-semibold">Lote</th>
              <th class="py-2 pr-3 text-right font-semibold">Enviado</th>
              <th class="py-2 pr-3 text-right font-semibold">Recibido</th>
              <th class="py-2 text-right font-semibold">Diferencia</th>
            </tr>
          </thead>
          <tbody>
            @for (linea of transferencia.lineas; track linea.id) {
              <tr class="border-b border-border/60 last:border-0">
                <td class="py-2 pr-3">{{ linea.productoNombre }}</td>
                <td class="py-2 pr-3 font-mono text-xs">{{ linea.codigoLote || '—' }}</td>
                <td class="py-2 pr-3 text-right tabular-nums">
                  {{ linea.cantidadEnviada | cantidad }}
                </td>
                <td class="py-2 pr-3 text-right tabular-nums">
                  {{ linea.cantidadRecibida | cantidad }}
                </td>
                <td class="py-2 text-right tabular-nums"
                    [class.text-danger]="linea.diferencia !== 0">
                  {{ linea.diferencia | cantidad }}
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
export class TransferenciasComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(TransferenciaConsultaService);
  private readonly stock = inject(StockConsultaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Documento' },
    { clave: 'ruta', titulo: 'Origen → destino' },
    { clave: 'guia', titulo: 'Guia / transportista', ocultarEn: 'tablet' },
    { clave: 'lineas', titulo: 'Items', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Transferencia>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fechaEnvio,desc' });

  protected readonly envioAbierto = signal(false);
  protected readonly enRecepcion = signal<Transferencia | null>(null);
  protected readonly detalle = signal<Transferencia | null>(null);
  protected readonly guardando = signal(false);
  protected readonly lotes = signal<Record<number, SugerenciaLote[]>>({});
  protected readonly ubicacionesDestino = signal<Opcion<number>[]>([]);

  protected readonly formularioEnvio = this.fb.group({
    almacenOrigenId: [null as number | null, [Validators.required]],
    almacenDestinoId: [null as number | null, [Validators.required]],
    guiaRemision: [''],
    transportista: [''],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected readonly formularioRecepcion = this.fb.group({
    ubicacionDestinoId: [null as number | null],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineasEnvio(): FormArray<FormGroup> {
    return this.formularioEnvio.controls.lineas;
  }

  protected get lineasRecepcion(): FormArray<FormGroup> {
    return this.formularioRecepcion.controls.lineas;
  }

  protected readonly destinos = computed(() =>
    this.contexto.almacenes()
      .filter((a) => a.id !== Number(this.formularioEnvio.value.almacenOrigenId)));

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
    });
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  /** Lotes disponibles del producto, en orden FEFO, para el selector de la linea. */
  protected lotesDe(productoId: number): SugerenciaLote[] {
    return this.lotes()[productoId] ?? [];
  }

  protected hayDiferencias(): boolean {
    return this.lineasRecepcion.controls.some((grupo) => {
      const valores = grupo.value as { cantidadEnviada: number; cantidadRecibida: number };
      return Number(valores.cantidadRecibida) !== Number(valores.cantidadEnviada);
    });
  }

  protected abrirEnvio(): void {
    this.lineasEnvio.clear();
    this.formularioEnvio.patchValue({
      almacenOrigenId: this.contexto.seleccionadoId(),
      almacenDestinoId: null,
      guiaRemision: '',
      transportista: '',
      observacion: '',
    });
    this.envioAbierto.set(true);
  }

  protected agregarLinea(producto: ProductoResumen): void {
    this.lineasEnvio.push(this.fb.group({
      productoId: [producto.id],
      codigo: [producto.codigo],
      nombre: [producto.nombre],
      controlaLote: [producto.controlaLote],
      loteId: [''],
      cantidad: [1],
    }));

    const almacenId = Number(this.formularioEnvio.value.almacenOrigenId);
    if (producto.controlaLote && almacenId) {
      this.stock.sugerenciaFefo(producto.id, almacenId, 0.0001).subscribe({
        next: (sugerencias) => this.lotes.update((actual) => ({
          ...actual, [producto.id]: sugerencias,
        })),
        error: () => this.notificacion.advertencia(
          `${producto.nombre} no tiene saldo en el almacen de origen`),
      });
    }
  }

  protected enviar(): void {
    const valores = this.formularioEnvio.getRawValue();
    if (!valores.almacenOrigenId || !valores.almacenDestinoId) {
      this.notificacion.advertencia('Indique el almacen de origen y de destino');
      return;
    }
    const cuerpo: TransferenciaRequest = {
      almacenOrigenId: Number(valores.almacenOrigenId),
      almacenDestinoId: Number(valores.almacenDestinoId),
      guiaRemision: valores.guiaRemision || null,
      transportista: valores.transportista || null,
      observacion: valores.observacion || null,
      lineas: this.lineasEnvio.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          productoId: Number(linea.productoId),
          loteId: linea.loteId ? Number(linea.loteId) : null,
          cantidad: Number(linea.cantidad),
        };
      }),
    };
    this.guardando.set(true);
    this.servicio.enviar(cuerpo).subscribe({
      next: (transferencia) => {
        this.guardando.set(false);
        this.envioAbierto.set(false);
        this.notificacion.exito('Transferencia enviada', transferencia.numero);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected abrirRecepcion(transferencia: Transferencia): void {
    this.servicio.obtener(transferencia.id).subscribe((completa) => {
      this.enRecepcion.set(completa);
      this.lineasRecepcion.clear();
      completa.lineas.forEach((linea) => this.lineasRecepcion.push(this.fb.group({
        transferenciaLineaId: [linea.id],
        nombre: [linea.productoNombre],
        codigoLote: [linea.codigoLote ?? ''],
        cantidadEnviada: [linea.cantidadEnviada],
        cantidadRecibida: [linea.cantidadEnviada],
        observacion: [''],
      })));
      this.formularioRecepcion.patchValue({ ubicacionDestinoId: null, observacion: '' });
      this.contexto.almacenes()
        .filter((a) => a.id === completa.almacenDestinoId)
        .forEach(() => undefined);
      this.ubicacionesDestino.set([]);
    });
  }

  protected recibir(): void {
    const transferencia = this.enRecepcion();
    if (!transferencia) {
      return;
    }
    const valores = this.formularioRecepcion.getRawValue();
    if (this.hayDiferencias() && !(valores.observacion ?? '').trim()) {
      this.notificacion.advertencia('Indique la observacion: hay diferencias entre lo enviado y lo recibido');
      return;
    }
    const cuerpo: RecepcionTransferenciaRequest = {
      ubicacionDestinoId: valores.ubicacionDestinoId ? Number(valores.ubicacionDestinoId) : null,
      observacion: valores.observacion || null,
      lineas: this.lineasRecepcion.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          transferenciaLineaId: Number(linea.transferenciaLineaId),
          cantidadRecibida: Number(linea.cantidadRecibida),
          observacion: linea.observacion || null,
        };
      }),
    };
    this.guardando.set(true);
    this.servicio.recibir(transferencia.id, cuerpo).subscribe({
      next: (actualizada) => {
        this.guardando.set(false);
        this.enRecepcion.set(null);
        this.notificacion.exito('Recepcion confirmada',
          `${actualizada.numero}: ${actualizada.estadoDescripcion}`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected verDetalle(transferencia: Transferencia): void {
    this.servicio.obtener(transferencia.id).subscribe((completa) => this.detalle.set(completa));
  }
}
