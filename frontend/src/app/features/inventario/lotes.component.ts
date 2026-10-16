import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';
import { EstadoLote, Lote, TrazabilidadLote } from '../../core/models/inventario.model';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiSelectComponent,
  UiTableComponent,
  UiTextareaComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { LoteConsultaService } from './inventario.service';

/**
 * Lotes, vencimientos y control sanitario (HU-15 a HU-18, HU-45).
 * El quimico farmaceutico libera, retiene o rechaza; cualquier cambio exige
 * motivo y la trazabilidad muestra donde esta y donde se consumio cada lote.
 */
@Component({
  selector: 'sgai-lotes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    UiSelectComponent, UiTextareaComponent, CantidadPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Lotes y vencimientos"
                    descripcion="Trazabilidad sanitaria: estado de cada lote, saldo y fecha de vencimiento." />

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-4">
        <input class="sgai-input lg:col-span-2" type="search"
               placeholder="Codigo de lote o producto..."
               (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
        <select class="sgai-input" (change)="lista.filtrar({ estado: $any($event.target).value || null })">
          <option value="">Todos los estados</option>
          @for (estado of estados; track estado.valor) {
            <option [value]="estado.valor">{{ estado.etiqueta }}</option>
          }
        </select>
        <label class="block">
          <span class="sr-only">Vence hasta</span>
          <input class="sgai-input" type="date" title="Vence hasta"
                 (change)="lista.filtrar({ venceHasta: $any($event.target).value || null })" />
        </label>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin lotes registrados">
          <ng-template #fila let-lote>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ lote.codigoLote }}</p>
              <p class="text-xs text-fg-muted">{{ lote.productoNombre }}</p>
            </td>
            <td class="hidden px-3 py-2.5 font-mono text-xs text-fg-muted sm:table-cell">
              {{ lote.productoCodigo }}
            </td>
            <td class="px-3 py-2.5">
              <p class="text-sm" [class.text-danger]="lote.vencido">
                {{ lote.fechaVencimiento | fechaSgai }}
              </p>
              @if (lote.diasParaVencer !== null && lote.diasParaVencer !== undefined) {
                <p class="text-xs"
                   [class.text-danger]="lote.diasParaVencer < 30"
                   [class.text-fg-muted]="lote.diasParaVencer >= 30">
                  {{ lote.diasParaVencer < 0
                      ? 'vencido hace ' + (-lote.diasParaVencer) + ' d'
                      : 'en ' + lote.diasParaVencer + ' d' }}
                </p>
              }
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ lote.saldoTotal | cantidad }}</td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ lote.proveedorNombre || '—' }}
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(lote.estadoLote)">{{ etiqueta(lote.estadoLote) }}</ui-badge>
              @if (lote.liberadoPor) {
                <p class="mt-0.5 text-[11px] text-fg-subtle">por {{ lote.liberadoPor }}</p>
              }
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verTrazabilidad(lote)">Trazabilidad</button>
              @if (lote.estadoLote === 'CUARENTENA' || lote.estadoLote === 'RETENIDO') {
                <span *hasPermiso="'inventario:lote:liberar'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-success
                                 hover:bg-success-soft" (click)="abrirCambio(lote, 'LIBERADO')">
                    Liberar
                  </button>
                </span>
              }
              @if (lote.estadoLote === 'LIBERADO') {
                <span *hasPermiso="'inventario:lote:retener'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-warning
                                 hover:bg-warning-soft" (click)="abrirCambio(lote, 'RETENIDO')">
                    Retener
                  </button>
                </span>
              }
            </td>
          </ng-template>

          <ng-template #tarjeta let-lote>
            <article class="sgai-card p-3">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="font-mono text-sm font-medium text-fg">{{ lote.codigoLote }}</p>
                  <p class="truncate text-xs text-fg-muted">{{ lote.productoNombre }}</p>
                </div>
                <ui-badge [tono]="tono(lote.estadoLote)">{{ etiqueta(lote.estadoLote) }}</ui-badge>
              </div>
              <dl class="mt-2 grid grid-cols-2 gap-2 text-xs">
                <div><dt class="text-fg-subtle">Vence</dt>
                  <dd [class.text-danger]="lote.vencido" class="text-fg">
                    {{ lote.fechaVencimiento | fechaSgai }}
                  </dd></div>
                <div><dt class="text-fg-subtle">Saldo</dt>
                  <dd class="tabular-nums text-fg">{{ lote.saldoTotal | cantidad }}</dd></div>
              </dl>
              <div class="mt-2 flex justify-end gap-2">
                <ui-button tamano="sm" variante="secundario" (presionado)="verTrazabilidad(lote)">
                  Trazabilidad
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

    <!-- Cambio de estado sanitario -->
    <ui-modal [abierto]="!!loteEnCambio()" tamano="sm"
              [titulo]="'Cambiar estado del lote ' + (loteEnCambio()?.codigoLote ?? '')"
              subtitulo="El cambio queda registrado con su usuario y el motivo en la auditoria."
              (cerrado)="loteEnCambio.set(null)">
      <form [formGroup]="formularioCambio" class="space-y-3">
        <ui-select etiqueta="Nuevo estado" formControlName="estado" [opciones]="estadosDestino()"
                   [requerido]="true" />
        <ui-textarea etiqueta="Motivo" formControlName="motivo" [requerido]="true" [filas]="3"
                     marcador="Certificado de analisis conforme / alerta del fabricante..." />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="loteEnCambio.set(null)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="confirmarCambio()">Confirmar</ui-button>
      </div>
    </ui-modal>

    <!-- Trazabilidad -->
    <ui-modal [abierto]="!!trazabilidad()" tamano="lg"
              [titulo]="'Trazabilidad del lote ' + (trazabilidad()?.lote?.codigoLote ?? '')"
              [subtitulo]="trazabilidad()?.lote?.productoNombre ?? ''"
              (cerrado)="trazabilidad.set(null)">
      @let traza = trazabilidad();
      @if (traza) {
        <section class="mb-4">
          <h3 class="mb-2 text-sm font-semibold text-fg">Donde esta ahora</h3>
          @if (traza.ubicacionesActuales.length) {
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
                  <th class="py-2 pr-3 font-semibold">Almacen</th>
                  <th class="py-2 pr-3 font-semibold">Ubicacion</th>
                  <th class="py-2 pr-3 text-right font-semibold">Cantidad</th>
                  <th class="py-2 text-right font-semibold">Disponible</th>
                </tr>
              </thead>
              <tbody>
                @for (saldo of traza.ubicacionesActuales; track saldo.id) {
                  <tr class="border-b border-border/60 last:border-0">
                    <td class="py-2 pr-3">{{ saldo.almacenNombre }}</td>
                    <td class="py-2 pr-3 font-mono text-xs">{{ saldo.ubicacionCodigo || '—' }}</td>
                    <td class="py-2 pr-3 text-right tabular-nums">{{ saldo.cantidad | cantidad }}</td>
                    <td class="py-2 text-right tabular-nums">{{ saldo.disponible | cantidad }}</td>
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="text-sm text-fg-muted">El lote ya no tiene saldo en ningun almacen.</p>
          }
        </section>

        <section>
          <h3 class="mb-2 text-sm font-semibold text-fg">Historia de movimientos</h3>
          <ol class="space-y-2">
            @for (movimiento of traza.movimientos; track movimiento.movimientoId) {
              <li class="rounded-lg border border-border px-3 py-2">
                <div class="flex flex-wrap items-baseline justify-between gap-2">
                  <span class="font-mono text-sm font-medium text-fg">{{ movimiento.numero }}</span>
                  <span class="text-xs text-fg-muted">{{ movimiento.fecha | fechaSgai }}</span>
                </div>
                <p class="mt-0.5 text-xs text-fg-muted">
                  {{ movimiento.tipoDescripcion }} · {{ movimiento.almacen }}
                  @if (movimiento.areaDestino) { · {{ movimiento.areaDestino }} }
                  @if (movimiento.centroCosto) { · {{ movimiento.centroCosto }} }
                </p>
                <p class="mt-0.5 text-xs">
                  <span class="tabular-nums font-medium text-fg">
                    {{ movimiento.cantidad | cantidad }}
                  </span>
                  <span class="text-fg-subtle"> · registro de {{ movimiento.usuario || '—' }}</span>
                  @if (movimiento.estado === 'ANULADO') {
                    <span class="ml-1 font-medium text-danger">(anulado)</span>
                  }
                </p>
              </li>
            } @empty {
              <li class="text-sm text-fg-muted">El lote no tiene movimientos registrados.</li>
            }
          </ol>
        </section>
      }
      <div pie>
        <ui-button variante="secundario" (presionado)="trazabilidad.set(null)">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class LotesComponent {
  private readonly servicio = inject(LoteConsultaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly estados: Opcion<string>[] = [
    { valor: 'CUARENTENA', etiqueta: 'En cuarentena' },
    { valor: 'LIBERADO', etiqueta: 'Liberado' },
    { valor: 'RETENIDO', etiqueta: 'Retenido' },
    { valor: 'RECHAZADO', etiqueta: 'Rechazado' },
    { valor: 'VENCIDO', etiqueta: 'Vencido' },
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'lote', titulo: 'Lote / producto' },
    { clave: 'codigo', titulo: 'Codigo', ocultarEn: 'movil' },
    { clave: 'vencimiento', titulo: 'Vencimiento', ordenPor: 'fechaVencimiento' },
    { clave: 'saldo', titulo: 'Saldo', alineacion: 'der' },
    { clave: 'proveedor', titulo: 'Proveedor', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Estado sanitario', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Lote>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fechaVencimiento,asc' });

  protected readonly loteEnCambio = signal<Lote | null>(null);
  protected readonly trazabilidad = signal<TrazabilidadLote | null>(null);
  protected readonly guardando = signal(false);

  protected readonly formularioCambio = this.fb.nonNullable.group({
    estado: ['LIBERADO', [Validators.required]],
    motivo: ['', [Validators.required, Validators.minLength(5)]],
  });

  constructor() {
    this.lista.recargar();
  }

  /** Solo se ofrecen las transiciones validas desde el estado actual. */
  protected estadosDestino(): Opcion<string>[] {
    const actual = this.loteEnCambio()?.estadoLote;
    const transiciones: Record<EstadoLote, string[]> = {
      CUARENTENA: ['LIBERADO', 'RECHAZADO'],
      LIBERADO: ['RETENIDO', 'RECHAZADO'],
      RETENIDO: ['LIBERADO', 'RECHAZADO'],
      RECHAZADO: [],
      VENCIDO: [],
    };
    const permitidos = actual ? transiciones[actual] : [];
    return this.estados.filter((estado) => permitidos.includes(String(estado.valor)));
  }

  protected abrirCambio(lote: Lote, destino: EstadoLote): void {
    this.loteEnCambio.set(lote);
    this.formularioCambio.reset({ estado: destino, motivo: '' });
  }

  protected confirmarCambio(): void {
    const lote = this.loteEnCambio();
    if (!lote || this.formularioCambio.invalid) {
      this.formularioCambio.markAllAsTouched();
      return;
    }
    const { estado, motivo } = this.formularioCambio.getRawValue();
    this.guardando.set(true);
    const peticion = estado === 'LIBERADO'
      ? this.servicio.liberar(lote.id, motivo)
      : this.servicio.cambiarEstado(lote.id, { estado: estado as EstadoLote, motivo });
    peticion.subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.loteEnCambio.set(null);
        this.notificacion.exito('Estado del lote actualizado',
          `${actualizado.codigoLote}: ${actualizado.estadoDescripcion}`);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected verTrazabilidad(lote: Lote): void {
    this.servicio.trazabilidad(lote.id).subscribe((traza) => this.trazabilidad.set(traza));
  }
}
