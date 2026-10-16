import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Requerimiento, RequerimientoRequest, SugerenciaReposicion } from '../../core/models/compras.model';
import { ProductoResumen } from '../../core/models/catalogo.model';
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
  UiTabsComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { RequerimientoService } from './compras.service';

/**
 * Requerimientos de compra (HU-27). La pestana de sugerencia muestra lo que el
 * sistema propone reponer segun punto de reorden, consumo promedio y cobertura:
 * es el mecanismo que evita los quiebres de insumos criticos.
 */
@Component({
  selector: 'sgai-requerimientos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiTabsComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    SelectorProductoComponent, CantidadPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Requerimientos de compra"
                    descripcion="Reposicion sugerida por el sistema y requerimientos enviados a Compras.">
      <div acciones>
        <span *hasPermiso="'compras:requerimiento:crear'">
          <ui-button variante="secundario" (presionado)="crearDesdeSugerencia()">
            Generar desde sugerencia
          </ui-button>
          <ui-button (presionado)="abrirNuevo()">Nuevo requerimiento</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-tabs [pestanas]="pestanas()" [(activa)]="vista" />

    @if (vista() === 'sugerencia') {
      <ui-card titulo="Reposicion sugerida"
               [subtitulo]="'Calculo: max(stock maximo − disponible, consumo diario × dias de cobertura) · '
                 + (contexto.seleccionado()?.nombre ?? 'seleccione un almacen')"
               [sinRelleno]="true">
        <div class="px-4 py-3">
          <ui-table [columnas]="columnasSugerencia" [datos]="sugerencia()"
                    [cargando]="cargandoSugerencia()"
                    tituloVacio="Nada por reponer"
                    descripcionVacio="Ningun producto esta por debajo de su punto de reorden en este almacen.">
            <ng-template #fila let-item>
              <td class="px-3 py-2.5">
                <p class="font-medium text-fg">{{ item.productoNombre }}</p>
                <p class="font-mono text-xs text-fg-muted">
                  {{ item.productoCodigo }} · {{ item.unidadMedida }}
                  @if (item.critico) { · critico }
                </p>
              </td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ item.disponible | cantidad }}</td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums text-fg-muted sm:table-cell">
                {{ item.stockMinimo | cantidad }} / {{ item.puntoReorden | cantidad }} /
                {{ item.stockMaximo | cantidad }}
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                {{ item.consumoPromedioDiario | cantidad: 2 }}
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                @if (item.coberturaDias !== null && item.coberturaDias !== undefined) {
                  <span [class.text-danger]="item.coberturaDias < 15">
                    {{ item.coberturaDias | cantidad: 1 }} d
                  </span>
                } @else {
                  <span class="text-fg-subtle">—</span>
                }
              </td>
              <td class="px-3 py-2.5 text-center">
                <ui-badge [tono]="tono(item.estadoReposicion)">
                  {{ etiqueta(item.estadoReposicion) }}
                </ui-badge>
              </td>
              <td class="px-3 py-2.5 text-right font-medium tabular-nums text-brand-700
                         dark:text-brand-300">
                {{ item.cantidadSugerida | cantidad }}
              </td>
            </ng-template>
          </ui-table>
        </div>
      </ui-card>
    } @else {
      <ui-card [sinRelleno]="true">
        <div class="border-b border-border px-4 py-3">
          <select class="sgai-input max-w-xs"
                  (change)="lista.filtrar({ estado: $any($event.target).value || null })">
            <option value="">Todos los estados</option>
            <option value="BORRADOR">Borrador</option>
            <option value="ENVIADO">Enviado</option>
            <option value="APROBADO">Aprobado</option>
            <option value="RECHAZADO">Rechazado</option>
            <option value="ATENDIDO">Atendido con OC</option>
          </select>
        </div>
        <div class="px-4 py-3">
          <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                    tituloVacio="Sin requerimientos">
            <ng-template #fila let-requerimiento>
              <td class="px-3 py-2.5">
                <p class="font-mono text-sm font-medium text-fg">{{ requerimiento.numero }}</p>
                <p class="text-xs text-fg-muted">{{ requerimiento.fecha | fechaSgai }}</p>
              </td>
              <td class="px-3 py-2.5 text-sm text-fg-muted">{{ requerimiento.almacenNombre }}</td>
              <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
                {{ requerimiento.solicitante || '—' }}
              </td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ requerimiento.lineas.length }}</td>
              <td class="px-3 py-2.5 text-center">
                <ui-badge [tono]="tono(requerimiento.estado)">
                  {{ etiqueta(requerimiento.estado) }}
                </ui-badge>
                @if (requerimiento.aprobador) {
                  <p class="mt-0.5 text-[11px] text-fg-subtle">por {{ requerimiento.aprobador }}</p>
                }
              </td>
              <td class="px-3 py-2.5 text-right">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                               hover:bg-surface-muted dark:text-brand-300"
                        (click)="verDetalle(requerimiento)">Detalle</button>
                @if (requerimiento.estado === 'BORRADOR') {
                  <span *hasPermiso="'compras:requerimiento:crear'">
                    <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                   text-success hover:bg-success-soft"
                            (click)="enviar(requerimiento)">Enviar</button>
                  </span>
                }
                @if (requerimiento.estado === 'ENVIADO') {
                  <span *hasPermiso="'compras:requerimiento:aprobar'">
                    <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                   text-success hover:bg-success-soft"
                            (click)="aprobar(requerimiento)">Aprobar</button>
                    <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                   text-danger hover:bg-danger-soft"
                            (click)="rechazar(requerimiento)">Rechazar</button>
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
    }

    <!-- Nuevo requerimiento -->
    <ui-modal [abierto]="modalAbierto()" tamano="lg" titulo="Nuevo requerimiento de compra"
              subtitulo="Las cantidades sugeridas son editables antes de enviar a Compras."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block">
            <span class="sgai-label">Almacen solicitante</span>
            <select class="sgai-input" formControlName="almacenId">
              @for (almacen of contexto.almacenes(); track almacen.id) {
                <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
              }
            </select>
          </label>
          <label class="block">
            <span class="sgai-label">Justificacion</span>
            <input class="sgai-input" formControlName="justificacion"
                   placeholder="Reposicion de insumos criticos" />
          </label>
        </div>

        <div class="rounded-xl border border-border">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border
                      px-3 py-2">
            <h3 class="text-sm font-semibold text-fg">Productos</h3>
            <div class="w-full sm:w-80">
              <sgai-selector-producto etiqueta="" [excluidos]="productosEnLineas()"
                                      (seleccionado)="agregarLinea($event)" />
            </div>
          </div>
          @if (!lineas.length) {
            <p class="px-3 py-6 text-center text-sm text-fg-muted">
              Agregue productos o use "Generar desde sugerencia".
            </p>
          } @else {
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                  <th class="px-3 py-2 font-semibold">Producto</th>
                  <th class="px-3 py-2 text-right font-semibold">Sugerido</th>
                  <th class="px-3 py-2 text-right font-semibold">Solicitado</th>
                  <th class="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                @for (grupo of lineas.controls; track $index; let i = $index) {
                  <tr class="border-b border-border/60 last:border-0" [formGroup]="asGrupo(grupo)">
                    <td class="px-3 py-2">
                      <p class="text-sm text-fg">{{ asGrupo(grupo).value.nombre }}</p>
                      <p class="font-mono text-xs text-fg-muted">{{ asGrupo(grupo).value.codigo }}</p>
                    </td>
                    <td class="px-3 py-2 text-right text-xs tabular-nums text-fg-muted">
                      {{ asGrupo(grupo).value.sugerido | cantidad }}
                    </td>
                    <td class="px-3 py-2">
                      <input class="sgai-input py-1.5 text-right text-xs tabular-nums" type="number"
                             step="0.0001" min="0.0001" formControlName="cantidadSolicitada" />
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
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" [deshabilitado]="!lineas.length"
                   (presionado)="guardar()">Crear requerimiento</ui-button>
      </div>
    </ui-modal>

    <!-- Detalle -->
    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Requerimiento ' + (detalle()?.numero ?? '')"
              [subtitulo]="detalle()?.justificacion ?? ''" (cerrado)="detalle.set(null)">
      @let requerimiento = detalle();
      @if (requerimiento) {
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="py-2 pr-3 font-semibold">Producto</th>
              <th class="py-2 pr-3 text-right font-semibold">Solicitado</th>
              <th class="py-2 pr-3 text-right font-semibold">Sugerido</th>
              <th class="py-2 text-right font-semibold">Disponible al pedir</th>
            </tr>
          </thead>
          <tbody>
            @for (linea of requerimiento.lineas; track linea.id) {
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
                <td class="py-2 pr-3 text-right tabular-nums text-fg-muted">
                  {{ linea.cantidadSugerida | cantidad }}
                </td>
                <td class="py-2 text-right tabular-nums text-fg-muted">
                  {{ linea.disponibleActual | cantidad }}
                </td>
              </tr>
            }
          </tbody>
        </table>
        @if (requerimiento.motivoRechazo) {
          <p class="mt-3 rounded-lg border border-danger/30 bg-danger-soft px-3 py-2 text-sm
                    text-danger">
            Rechazado: {{ requerimiento.motivoRechazo }}
          </p>
        }
      }
      <div pie>
        <ui-button variante="secundario" (presionado)="detalle.set(null)">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class RequerimientosComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(RequerimientoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly vista = signal<'sugerencia' | 'requerimientos'>('sugerencia');
  protected readonly sugerencia = signal<SugerenciaReposicion[]>([]);
  protected readonly cargandoSugerencia = signal(false);
  protected readonly modalAbierto = signal(false);
  protected readonly detalle = signal<Requerimiento | null>(null);
  protected readonly guardando = signal(false);

  protected readonly columnasSugerencia: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'disponible', titulo: 'Disponible', alineacion: 'der' },
    { clave: 'parametros', titulo: 'Min / reorden / max', alineacion: 'der', ocultarEn: 'movil' },
    { clave: 'consumo', titulo: 'Consumo diario', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'cobertura', titulo: 'Cobertura', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Situacion', alineacion: 'centro' },
    { clave: 'sugerido', titulo: 'A comprar', alineacion: 'der' },
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Documento' },
    { clave: 'almacen', titulo: 'Almacen' },
    { clave: 'solicitante', titulo: 'Solicitante', ocultarEn: 'tablet' },
    { clave: 'items', titulo: 'Items', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Requerimiento>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fecha,desc' });

  protected readonly formulario = this.fb.group({
    almacenId: [null as number | null],
    justificacion: [''],
    lineas: this.fb.array<FormGroup>([]),
  });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected readonly productosEnLineas = computed<number[]>(() =>
    this.lineas.controls.map((grupo) => Number(grupo.value.productoId)));

  protected readonly pestanas = computed(() => [
    { clave: 'sugerencia', titulo: 'Reposicion sugerida', contador: this.sugerencia().length },
    { clave: 'requerimientos', titulo: 'Requerimientos', contador: this.lista.total() },
  ]);

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
      if (almacenId) {
        this.cargandoSugerencia.set(true);
        this.servicio.sugerencia(almacenId).subscribe({
          next: (lista) => {
            this.sugerencia.set(lista);
            this.cargandoSugerencia.set(false);
          },
          error: () => this.cargandoSugerencia.set(false),
        });
      }
    });
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  protected abrirNuevo(): void {
    this.lineas.clear();
    this.formulario.patchValue({
      almacenId: this.contexto.seleccionadoId(),
      justificacion: '',
    });
    this.modalAbierto.set(true);
  }

  /** Precarga el requerimiento con lo que el sistema propone reponer. */
  protected crearDesdeSugerencia(): void {
    const propuesta = this.sugerencia();
    if (!propuesta.length) {
      this.notificacion.info('No hay productos por reponer en este almacen');
      return;
    }
    this.lineas.clear();
    this.formulario.patchValue({
      almacenId: this.contexto.seleccionadoId(),
      justificacion: 'Reposicion automatica por punto de reorden',
    });
    propuesta.forEach((item) => this.lineas.push(this.fb.group({
      productoId: [item.productoId],
      codigo: [item.productoCodigo],
      nombre: [item.productoNombre],
      sugerido: [item.cantidadSugerida],
      cantidadSolicitada: [item.cantidadSugerida],
    })));
    this.modalAbierto.set(true);
  }

  protected agregarLinea(producto: ProductoResumen): void {
    this.lineas.push(this.fb.group({
      productoId: [producto.id],
      codigo: [producto.codigo],
      nombre: [producto.nombre],
      sugerido: [0],
      cantidadSolicitada: [1],
    }));
  }

  protected guardar(): void {
    const valores = this.formulario.getRawValue();
    if (!valores.almacenId) {
      this.notificacion.advertencia('Seleccione el almacen');
      return;
    }
    const cuerpo: RequerimientoRequest = {
      almacenId: Number(valores.almacenId),
      justificacion: valores.justificacion || null,
      lineas: this.lineas.controls.map((grupo) => {
        const linea = grupo.getRawValue();
        return {
          productoId: Number(linea.productoId),
          cantidadSolicitada: Number(linea.cantidadSolicitada),
        };
      }),
    };
    this.guardando.set(true);
    this.servicio.crear(cuerpo).subscribe({
      next: (requerimiento) => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.vista.set('requerimientos');
        this.notificacion.exito('Requerimiento creado', requerimiento.numero);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected enviar(requerimiento: Requerimiento): void {
    this.servicio.enviar(requerimiento.id).subscribe(() => {
      this.notificacion.exito('Requerimiento enviado', requerimiento.numero);
      this.lista.recargar();
    });
  }

  protected async aprobar(requerimiento: Requerimiento): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Aprobar ${requerimiento.numero}`,
      mensaje: 'Al aprobar, Compras podra emitir la orden de compra. Recuerde que quien registra '
        + 'el documento no puede aprobarlo.',
      textoConfirmar: 'Aprobar',
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.aprobar(requerimiento.id).subscribe(() => {
      this.notificacion.exito('Requerimiento aprobado', requerimiento.numero);
      this.lista.recargar();
    });
  }

  protected async rechazar(requerimiento: Requerimiento): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Rechazar ${requerimiento.numero}`,
      mensaje: 'Indique el motivo del rechazo para el solicitante.',
      peligroso: true,
      pedirMotivo: true,
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.rechazar(requerimiento.id, respuesta.motivo).subscribe(() => {
      this.notificacion.exito('Requerimiento rechazado');
      this.lista.recargar();
    });
  }

  protected verDetalle(requerimiento: Requerimiento): void {
    this.servicio.obtener(requerimiento.id).subscribe((completo) => this.detalle.set(completo));
  }
}
