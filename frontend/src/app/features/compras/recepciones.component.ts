import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin, map } from 'rxjs';
import { Ubicacion } from '../../core/models/almacen.model';
import {
  OrdenCompra,
  OrdenCompraLinea,
  Recepcion,
  RecepcionRequest,
  ResultadoCalidad,
} from '../../core/models/compras.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import { CatalogoRefService } from '../../shared/services/catalogo-ref.service';
import { ListaPaginada } from '../../shared/services/lista-paginada';
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
import { OrdenCompraService, RecepcionService } from './compras.service';

/** Rango de la cadena de frio exigido por el fabricante de los insumos refrigerados. */
const TEMP_FRIO_MIN = 2;
const TEMP_FRIO_MAX = 8;

/**
 * Recepcion de mercaderia con control de calidad sanitario (HU-30, HU-31).
 * Es el punto donde el lote entra al sistema: aqui se registran lote, fecha de
 * vencimiento, temperatura de llegada e integridad del empaque, y el resultado
 * decide si la mercaderia ingresa a cuarentena o se rechaza al proveedor.
 */
@Component({
  selector: 'sgai-recepciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    CantidadPipe, MonedaPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Recepcion de mercaderia"
                    descripcion="Control de calidad al ingreso: lote, vencimiento, cadena de frio e integridad del empaque.">
      <div acciones>
        <ui-button *hasPermiso="'compras:recepcion:registrar'" (presionado)="abrirRegistro()">
          Registrar recepcion
        </ui-button>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin recepciones"
                  descripcionVacio="Las recepciones aparecen al recibir mercaderia de una orden aprobada.">
          <ng-template #fila let-recepcion>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ recepcion.numero }}</p>
              <p class="text-xs text-fg-muted">{{ recepcion.fecha | fechaSgai: 'hora' }}</p>
            </td>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm text-fg">{{ recepcion.ordenNumero }}</p>
              <p class="text-xs text-fg-muted">{{ recepcion.almacenNombre }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              @if (recepcion.guiaRemision) { Guia {{ recepcion.guiaRemision }}<br /> }
              @if (recepcion.factura) { Factura {{ recepcion.factura }} }
              @if (!recepcion.guiaRemision && !recepcion.factura) { — }
            </td>
            <td class="hidden px-3 py-2.5 text-center text-sm sm:table-cell">
              @if (recepcion.temperaturaLlegada !== null && recepcion.temperaturaLlegada !== undefined) {
                <span class="tabular-nums"
                      [class.text-danger]="fueraDeRango(recepcion.temperaturaLlegada)">
                  {{ recepcion.temperaturaLlegada }} °C
                </span>
              } @else {
                <span class="text-fg-subtle">—</span>
              }
            </td>
            <td class="px-3 py-2.5 text-center">
              @if (recepcion.empaqueIntegro) {
                <ui-badge tono="exito">Integro</ui-badge>
              } @else {
                <ui-badge tono="peligro">Danado</ui-badge>
              }
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(recepcion.resultadoCalidad)">
                {{ recepcion.resultadoDescripcion }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(recepcion)">Detalle</button>
            </td>
          </ng-template>

          <ng-template #tarjeta let-recepcion>
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-mono text-sm font-medium text-fg">{{ recepcion.numero }}</p>
                <p class="text-xs text-fg-muted">
                  OC {{ recepcion.ordenNumero }} · {{ recepcion.fecha | fechaSgai }}
                </p>
              </div>
              <ui-badge [tono]="tono(recepcion.resultadoCalidad)">
                {{ recepcion.resultadoDescripcion }}
              </ui-badge>
            </div>
            <div class="mt-2">
              <ui-button tamano="sm" variante="secundario" (presionado)="verDetalle(recepcion)">
                Detalle
              </ui-button>
            </div>
          </ng-template>
        </ui-table>

        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <!-- Registro de recepcion -->
    <ui-modal [abierto]="modalAbierto()" tamano="xl" titulo="Registrar recepcion"
              subtitulo="Verifique fisicamente la mercaderia antes de confirmar: lo que se registre aqui ingresa al kardex."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <label class="block">
          <span class="sgai-label">Orden de compra <span class="text-danger">*</span></span>
          <select class="sgai-input" (change)="cargarOrden($any($event.target).value)">
            <option value="">Seleccione la orden recibida...</option>
            @for (orden of ordenesRecibibles(); track orden.id) {
              <option [value]="orden.id">
                {{ orden.numero }} · {{ orden.proveedorRazonSocial }} ·
                {{ orden.almacenDestinoNombre }}
              </option>
            }
          </select>
          @if (!ordenesRecibibles().length) {
            <span class="mt-1 block text-xs text-fg-muted">
              No hay ordenes aprobadas pendientes de recibir.
            </span>
          }
        </label>

        @let orden = ordenSeleccionada();
        @if (orden) {
          @if (requiereFrio()) {
            <p class="rounded-lg border border-info/30 bg-info-soft px-3 py-2 text-sm text-info">
              Esta orden incluye productos de cadena de frio: registre la temperatura de llegada,
              que debe estar entre {{ TEMP_MIN }} y {{ TEMP_MAX }} °C.
            </p>
          }

          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label class="block">
              <span class="sgai-label">Guia de remision</span>
              <input class="sgai-input" formControlName="guiaRemision" placeholder="001-000123"
                     maxlength="30" />
            </label>
            <label class="block">
              <span class="sgai-label">Factura</span>
              <input class="sgai-input" formControlName="factura" placeholder="F001-00045"
                     maxlength="30" />
            </label>
            <label class="block">
              <span class="sgai-label">Transportista</span>
              <input class="sgai-input" formControlName="transportista" maxlength="120" />
            </label>
            <label class="block">
              <span class="sgai-label">
                Temperatura de llegada (°C)
                @if (requiereFrio()) { <span class="text-danger">*</span> }
              </span>
              <input class="sgai-input" type="number" step="0.1" formControlName="temperaturaLlegada"
                     placeholder="4.5" />
              @if (temperaturaFueraDeRango()) {
                <span class="mt-1 block text-xs text-danger">
                  Fuera del rango {{ TEMP_MIN }}-{{ TEMP_MAX }} °C: la mercaderia debe rechazarse.
                </span>
              }
            </label>
            <label class="block">
              <span class="sgai-label">Ubicacion de ingreso</span>
              <select class="sgai-input" formControlName="ubicacionId">
                <option [value]="null">Zona de cuarentena por defecto</option>
                @for (ubicacion of ubicaciones(); track ubicacion.id) {
                  <option [value]="ubicacion.id">
                    {{ ubicacion.codigo }} · {{ ubicacion.descripcion || ubicacion.tipoUbicacion }}
                  </option>
                }
              </select>
            </label>
            <label class="block">
              <span class="sgai-label">Resultado del control de calidad</span>
              <select class="sgai-input" formControlName="resultadoCalidad">
                <option value="CONFORME">Conforme · ingresa a cuarentena</option>
                <option value="OBSERVADO">Observado · ingresa con observacion</option>
                <option value="RECHAZADO">Rechazado · no ingresa al almacen</option>
              </select>
            </label>
          </div>

          <label class="flex items-center gap-2">
            <input type="checkbox" class="size-4 rounded border-border accent-brand-600"
                   formControlName="empaqueIntegro" />
            <span class="text-sm text-fg">
              Empaque primario y secundario integros, sin humedad ni roturas
            </span>
          </label>

          <div class="rounded-xl border border-border">
            <div class="border-b border-border px-3 py-2">
              <h3 class="text-sm font-semibold text-fg">
                Lineas pendientes de {{ orden.numero }}
              </h3>
              <p class="text-xs text-fg-muted">
                Ajuste la cantidad a lo realmente recibido; el saldo queda pendiente en la orden.
              </p>
            </div>

            @if (!lineas.length) {
              <p class="px-3 py-6 text-center text-sm text-fg-muted">
                Esta orden ya fue recibida en su totalidad.
              </p>
            } @else {
              <div class="divide-y divide-border">
                @for (grupo of lineas.controls; track $index) {
                  <div class="space-y-3 px-3 py-3" [formGroup]="asGrupo(grupo)">
                    <div class="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <p class="text-sm font-medium text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                        <p class="font-mono text-xs text-fg-muted">
                          {{ asGrupo(grupo).value.codigo }} · pendiente
                          {{ asGrupo(grupo).value.pendiente | cantidad }}
                          @if (asGrupo(grupo).value.requiereCadenaFrio) { · cadena de frio }
                        </p>
                      </div>
                      <label class="flex items-center gap-2">
                        <input type="checkbox" class="size-4 rounded border-border accent-brand-600"
                               formControlName="conforme" />
                        <span class="text-xs text-fg-muted">Linea conforme</span>
                      </label>
                    </div>

                    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                      <label class="block">
                        <span class="sgai-label">Cantidad recibida</span>
                        <input class="sgai-input py-1.5 text-right text-sm tabular-nums"
                               type="number" step="0.0001" min="0" formControlName="cantidad" />
                      </label>
                      @if (asGrupo(grupo).value.controlaLote) {
                        <label class="block">
                          <span class="sgai-label">Lote <span class="text-danger">*</span></span>
                          <input class="sgai-input py-1.5 text-sm uppercase"
                                 formControlName="codigoLote" placeholder="LOTE-2026-001"
                                 maxlength="50" />
                        </label>
                      }
                      <label class="block">
                        <span class="sgai-label">Fabricacion</span>
                        <input class="sgai-input py-1.5 text-sm" type="date"
                               formControlName="fechaFabricacion" />
                      </label>
                      @if (asGrupo(grupo).value.controlaVencimiento) {
                        <label class="block">
                          <span class="sgai-label">
                            Vencimiento <span class="text-danger">*</span>
                          </span>
                          <input class="sgai-input py-1.5 text-sm" type="date"
                                 formControlName="fechaVencimiento" />
                          <span class="mt-1 block text-xs"
                                [class.text-danger]="vidaUtilInsuficiente(grupo)"
                                [class.text-fg-muted]="!vidaUtilInsuficiente(grupo)">
                            Vida util minima exigida:
                            {{ asGrupo(grupo).value.vidaUtilMinimaDias }} dias
                          </span>
                        </label>
                      }
                    </div>

                    <input class="sgai-input py-1.5 text-sm" formControlName="observacion"
                           placeholder="Observacion de la linea (opcional)" maxlength="255" />
                  </div>
                }
              </div>
            }
          </div>

          <label class="block">
            <span class="sgai-label">
              Observacion del acta
              @if (formulario.value.resultadoCalidad !== 'CONFORME') {
                <span class="text-danger">* obligatoria si no es conforme</span>
              }
            </span>
            <textarea class="sgai-input" rows="2" formControlName="observacion"
                      placeholder="Hallazgos del control de calidad, acuerdos con el proveedor..."></textarea>
          </label>
        }
      </form>

      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineas.length"
                   (presionado)="registrar()">Confirmar recepcion</ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Recepcion ' + (detalle()?.numero ?? '')"
              [subtitulo]="'Orden ' + (detalle()?.ordenNumero ?? '')"
              (cerrado)="detalle.set(null)">
      @let recepcion = detalle();
      @if (recepcion) {
        <dl class="mb-4 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
          <div>
            <dt class="text-xs text-fg-subtle">Fecha</dt>
            <dd class="text-fg">{{ recepcion.fecha | fechaSgai: 'hora' }}</dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Recibido por</dt>
            <dd class="text-fg">{{ recepcion.recibidoPor || '—' }}</dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Temperatura</dt>
            <dd class="text-fg">
              {{ recepcion.temperaturaLlegada !== null && recepcion.temperaturaLlegada !== undefined
                 ? recepcion.temperaturaLlegada + ' °C' : '—' }}
            </dd>
          </div>
          <div>
            <dt class="text-xs text-fg-subtle">Resultado</dt>
            <dd>
              <ui-badge [tono]="tono(recepcion.resultadoCalidad)">
                {{ recepcion.resultadoDescripcion }}
              </ui-badge>
            </dd>
          </div>
        </dl>

        <div class="overflow-x-auto">
          <table class="w-full min-w-[34rem] text-sm">
            <thead>
              <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                <th class="py-2 pr-3 font-semibold">Producto</th>
                <th class="py-2 pr-3 font-semibold">Lote</th>
                <th class="py-2 pr-3 font-semibold">Vence</th>
                <th class="py-2 pr-3 text-right font-semibold">Cantidad</th>
                <th class="py-2 text-right font-semibold">P. unitario</th>
              </tr>
            </thead>
            <tbody>
              @for (linea of recepcion.lineas; track linea.id) {
                <tr class="border-b border-border/60 last:border-0">
                  <td class="py-2 pr-3">
                    <p class="text-fg">{{ linea.productoNombre }}</p>
                    <p class="font-mono text-xs text-fg-muted">{{ linea.productoCodigo }}</p>
                    @if (linea.observacion) {
                      <p class="text-xs text-warning">{{ linea.observacion }}</p>
                    }
                  </td>
                  <td class="py-2 pr-3 font-mono text-xs text-fg">{{ linea.codigoLote || '—' }}</td>
                  <td class="py-2 pr-3 text-xs text-fg-muted">
                    {{ linea.fechaVencimiento ? (linea.fechaVencimiento | fechaSgai) : '—' }}
                  </td>
                  <td class="py-2 pr-3 text-right tabular-nums"
                      [class.text-danger]="!linea.conforme">
                    {{ linea.cantidad | cantidad }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-fg-muted">
                    {{ linea.precioUnitario | moneda }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (recepcion.observacion) {
          <p class="mt-3 rounded-lg bg-surface-muted px-3 py-2 text-sm text-fg-muted">
            {{ recepcion.observacion }}
          </p>
        }
        @if (recepcion.movimientoId) {
          <p class="mt-2 text-xs text-fg-subtle">
            Genero el movimiento de ingreso #{{ recepcion.movimientoId }} en el kardex.
          </p>
        }
      }
      <div pie>
        <ui-button variante="secundario" (presionado)="detalle.set(null)">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class RecepcionesComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly catalogos = inject(CatalogoRefService);
  private readonly servicio = inject(RecepcionService);
  private readonly ordenes = inject(OrdenCompraService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;
  protected readonly TEMP_MIN = TEMP_FRIO_MIN;
  protected readonly TEMP_MAX = TEMP_FRIO_MAX;

  protected readonly modalAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly detalle = signal<Recepcion | null>(null);
  protected readonly ordenesRecibibles = signal<OrdenCompra[]>([]);
  protected readonly ordenSeleccionada = signal<OrdenCompra | null>(null);
  protected readonly ubicaciones = signal<Ubicacion[]>([]);
  private readonly revision = signal(0);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Recepcion', ordenPor: 'numero' },
    { clave: 'orden', titulo: 'Orden / almacen' },
    { clave: 'documentos', titulo: 'Documentos', ocultarEn: 'tablet' },
    { clave: 'temperatura', titulo: 'Temp.', alineacion: 'centro', ocultarEn: 'movil' },
    { clave: 'empaque', titulo: 'Empaque', alineacion: 'centro' },
    { clave: 'resultado', titulo: 'Calidad', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Recepcion>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fecha,desc' });

  protected readonly formulario = this.fb.group({
    ordenId: [null as number | null],
    guiaRemision: [''],
    factura: [''],
    transportista: [''],
    temperaturaLlegada: [null as number | null],
    ubicacionId: [null as number | null],
    empaqueIntegro: [true],
    resultadoCalidad: ['CONFORME' as ResultadoCalidad],
    observacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected readonly requiereFrio = computed(() =>
    (this.ordenSeleccionada()?.lineas ?? []).some((linea) => linea.requiereCadenaFrio));

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
    });
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  protected fueraDeRango(temperatura: number): boolean {
    return temperatura < TEMP_FRIO_MIN || temperatura > TEMP_FRIO_MAX;
  }

  protected temperaturaFueraDeRango(): boolean {
    const temperatura = this.formulario.value.temperaturaLlegada;
    return this.requiereFrio() && temperatura !== null && temperatura !== undefined
      && this.fueraDeRango(Number(temperatura));
  }

  /** Avisa en pantalla lo que el servidor rechazaria por vida util corta. */
  protected vidaUtilInsuficiente(control: unknown): boolean {
    this.revision();
    const valores = (control as FormGroup).getRawValue();
    if (!valores.fechaVencimiento) {
      return false;
    }
    const dias = Math.floor(
      (new Date(valores.fechaVencimiento).getTime() - Date.now()) / 86_400_000);
    return dias < Number(valores.vidaUtilMinimaDias ?? 0);
  }

  protected abrirRegistro(): void {
    this.lineas.clear();
    this.ordenSeleccionada.set(null);
    this.ubicaciones.set([]);
    this.formulario.reset({
      ordenId: null,
      guiaRemision: '',
      factura: '',
      transportista: '',
      temperaturaLlegada: null,
      ubicacionId: null,
      empaqueIntegro: true,
      resultadoCalidad: 'CONFORME',
      observacion: '',
    });
    forkJoin({
      aprobadas: this.ordenes.buscar({ page: 0, size: 50, estado: 'APROBADA', sort: 'fechaEmision,desc' }),
      parciales: this.ordenes.buscar({
        page: 0, size: 50, estado: 'RECIBIDA_PARCIAL', sort: 'fechaEmision,desc',
      }),
    }).pipe(map(({ aprobadas, parciales }) => [...aprobadas.contenido, ...parciales.contenido]))
      .subscribe((ordenes) => this.ordenesRecibibles.set(ordenes));
    this.modalAbierto.set(true);
  }

  /** Carga la orden y deja una linea por cada item con saldo pendiente. */
  protected cargarOrden(valor: string): void {
    const id = Number(valor);
    this.lineas.clear();
    this.ordenSeleccionada.set(null);
    this.ubicaciones.set([]);
    if (!id) {
      this.formulario.patchValue({ ordenId: null });
      return;
    }
    this.ordenes.obtener(id).subscribe((orden) => {
      this.ordenSeleccionada.set(orden);
      this.formulario.patchValue({ ordenId: orden.id });
      orden.lineas
        .filter((linea) => linea.pendiente > 0)
        .forEach((linea) => this.lineas.push(this.grupoDeLinea(linea)));
      this.revision.update((v) => v + 1);
      this.catalogos.ubicacionesDe(orden.almacenDestinoId).subscribe({
        next: (lista) => this.ubicaciones.set(lista),
        error: () => this.ubicaciones.set([]),
      });
    });
  }

  private grupoDeLinea(linea: OrdenCompraLinea): FormGroup {
    const grupo = this.fb.group({
      ordenLineaId: [linea.id],
      codigo: [linea.productoCodigo],
      nombre: [linea.productoNombre],
      pendiente: [linea.pendiente],
      controlaLote: [linea.controlaLote],
      controlaVencimiento: [linea.controlaVencimiento],
      requiereCadenaFrio: [linea.requiereCadenaFrio],
      vidaUtilMinimaDias: [linea.vidaUtilMinimaDias],
      cantidad: [linea.pendiente],
      codigoLote: [''],
      fechaFabricacion: [''],
      fechaVencimiento: [''],
      conforme: [true],
      observacion: [''],
    });
    grupo.controls.fechaVencimiento.valueChanges.subscribe(
      () => this.revision.update((v) => v + 1));
    return grupo;
  }

  protected async registrar(): Promise<void> {
    const valores = this.formulario.getRawValue();
    if (!valores.ordenId) {
      this.notificacion.advertencia('Seleccione la orden de compra recibida');
      return;
    }
    const resultado = valores.resultadoCalidad as ResultadoCalidad;
    if (resultado !== 'CONFORME' && !valores.observacion?.trim()) {
      this.notificacion.advertencia('Una recepcion no conforme necesita observacion');
      return;
    }
    if (this.requiereFrio()
      && (valores.temperaturaLlegada === null || valores.temperaturaLlegada === undefined)) {
      this.notificacion.advertencia('Registre la temperatura de llegada: hay productos refrigerados');
      return;
    }
    const lineas = this.lineas.controls
      .map((grupo) => grupo.getRawValue())
      .filter((linea) => Number(linea.cantidad) > 0);
    if (!lineas.length) {
      this.notificacion.advertencia('Indique al menos una cantidad recibida');
      return;
    }
    const faltaLote = lineas.some((linea) => linea.controlaLote && !linea.codigoLote?.trim());
    const faltaVence = lineas.some(
      (linea) => linea.controlaVencimiento && !linea.fechaVencimiento);
    if (resultado !== 'RECHAZADO' && (faltaLote || faltaVence)) {
      this.notificacion.advertencia(
        'Complete lote y fecha de vencimiento: son obligatorios para la trazabilidad');
      return;
    }
    const excede = lineas.find((linea) => Number(linea.cantidad) > Number(linea.pendiente));
    if (excede) {
      this.notificacion.advertencia(
        `${excede.nombre}: la cantidad supera el pendiente de la orden`);
      return;
    }

    const respuesta = await this.confirmacion.preguntar({
      titulo: 'Confirmar recepcion',
      mensaje: resultado === 'RECHAZADO'
        ? 'La mercaderia se registrara como rechazada y no ingresara al almacen.'
        : `Ingresaran ${lineas.length} linea(s) a cuarentena, a la espera de la liberacion `
          + 'de Quimico Farmaceutico. El movimiento de kardex no se puede editar luego.',
      textoConfirmar: 'Confirmar',
      peligroso: resultado === 'RECHAZADO',
    });
    if (!respuesta.confirmado) {
      return;
    }

    const cuerpo: RecepcionRequest = {
      ordenId: Number(valores.ordenId),
      guiaRemision: valores.guiaRemision || null,
      factura: valores.factura || null,
      transportista: valores.transportista || null,
      temperaturaLlegada: valores.temperaturaLlegada !== null
        && valores.temperaturaLlegada !== undefined
        ? Number(valores.temperaturaLlegada) : null,
      empaqueIntegro: !!valores.empaqueIntegro,
      resultadoCalidad: resultado,
      observacion: valores.observacion || null,
      ubicacionId: valores.ubicacionId ? Number(valores.ubicacionId) : null,
      lineas: lineas.map((linea) => ({
        ordenLineaId: Number(linea.ordenLineaId),
        codigoLote: linea.codigoLote?.trim().toUpperCase() || null,
        fechaFabricacion: linea.fechaFabricacion || null,
        fechaVencimiento: linea.fechaVencimiento || null,
        cantidad: Number(linea.cantidad),
        conforme: !!linea.conforme,
        observacion: linea.observacion || null,
      })),
    };

    this.guardando.set(true);
    this.servicio.registrar(cuerpo).subscribe({
      next: (recepcion) => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.notificacion.exito('Recepcion registrada',
          `${recepcion.numero} · ${recepcion.resultadoDescripcion}`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected verDetalle(recepcion: Recepcion): void {
    this.servicio.obtener(recepcion.id).subscribe((completa) => this.detalle.set(completa));
  }
}
