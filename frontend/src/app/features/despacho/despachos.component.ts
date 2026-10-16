import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { Despacho } from '../../core/models/despacho.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
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
} from '../../shared/ui';
import { DespachoService } from './despacho.service';

/**
 * Despachos efectuados (HU-33, HU-34). Permite verificar que lote se entrego a
 * cada area y detectar desviaciones de la politica FEFO.
 */
@Component({
  selector: 'sgai-despachos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiPaginationComponent,
    UiButtonComponent, UiBadgeComponent, UiModalComponent, CantidadPipe, MonedaPipe, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Despachos"
                    descripcion="Entregas a las areas asistenciales con el lote consumido y su valorizacion." />

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2">
        <label class="block">
          <span class="sgai-label">Desde</span>
          <input class="sgai-input" type="date"
                 (change)="lista.filtrar({ desde: $any($event.target).value || null })" />
        </label>
        <label class="block">
          <span class="sgai-label">Hasta</span>
          <input class="sgai-input" type="date"
                 (change)="lista.filtrar({ hasta: $any($event.target).value || null })" />
        </label>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  tituloVacio="Sin despachos en el periodo">
          <ng-template #fila let-despacho>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ despacho.numero }}</p>
              <p class="text-xs text-fg-muted">{{ despacho.fecha | fechaSgai }}</p>
            </td>
            <td class="px-3 py-2.5 font-mono text-xs text-fg-muted">
              {{ despacho.requisicionNumero }}
            </td>
            <td class="px-3 py-2.5 text-sm">
              <p class="text-fg">{{ despacho.area || '—' }}</p>
              <p class="text-xs text-fg-muted">{{ despacho.centroCosto || '—' }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              {{ despacho.entregadoA || '—' }}
              <span class="block">despacho de {{ despacho.despachadoPor || '—' }}</span>
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ despacho.lineas.length }}</td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ despacho.valorTotal | moneda }}</td>
            <td class="px-3 py-2.5 text-center">
              @if (tieneDesviacion(despacho)) {
                <ui-badge tono="advertencia">Desviacion FEFO</ui-badge>
              } @else {
                <ui-badge tono="exito">FEFO</ui-badge>
              }
            </td>
            <td class="px-3 py-2.5 text-right">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="verDetalle(despacho)">Detalle</button>
            </td>
          </ng-template>
        </ui-table>

        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <ui-modal [abierto]="!!detalle()" tamano="lg"
              [titulo]="'Despacho ' + (detalle()?.numero ?? '')"
              [subtitulo]="(detalle()?.area ?? '') + ' · requisicion ' + (detalle()?.requisicionNumero ?? '')"
              (cerrado)="detalle.set(null)">
      @let despacho = detalle();
      @if (despacho) {
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs uppercase text-fg-muted">
              <th class="py-2 pr-3 font-semibold">Producto</th>
              <th class="py-2 pr-3 font-semibold">Lote</th>
              <th class="py-2 pr-3 text-right font-semibold">Cantidad</th>
              <th class="py-2 pr-3 text-right font-semibold">Costo unitario</th>
              <th class="py-2 text-center font-semibold">FEFO</th>
            </tr>
          </thead>
          <tbody>
            @for (linea of despacho.lineas; track linea.id) {
              <tr class="border-b border-border/60 last:border-0">
                <td class="py-2 pr-3">
                  <p class="text-fg">{{ linea.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">
                    {{ linea.productoCodigo }} · {{ linea.unidadMedida }}
                  </p>
                </td>
                <td class="py-2 pr-3 font-mono text-xs">
                  {{ linea.codigoLote || '—' }}
                  @if (linea.fechaVencimiento) {
                    <span class="block text-fg-muted">
                      vence {{ linea.fechaVencimiento | fechaSgai }}
                    </span>
                  }
                </td>
                <td class="py-2 pr-3 text-right tabular-nums">{{ linea.cantidad | cantidad }}</td>
                <td class="py-2 pr-3 text-right tabular-nums">{{ linea.costoUnitario | moneda }}</td>
                <td class="py-2 text-center">
                  @if (linea.fefoRespetado) {
                    <ui-badge tono="exito" [punto]="false">Si</ui-badge>
                  } @else {
                    <ui-badge tono="advertencia" [punto]="false">No</ui-badge>
                    <p class="mt-0.5 max-w-[14rem] text-[11px] text-fg-muted">
                      {{ linea.motivoNoFefo }}
                    </p>
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
export class DespachosComponent {
  private readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(DespachoService);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Documento' },
    { clave: 'requisicion', titulo: 'Requisicion' },
    { clave: 'area', titulo: 'Area / centro de costo' },
    { clave: 'entrega', titulo: 'Entrega', ocultarEn: 'tablet' },
    { clave: 'items', titulo: 'Items', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valor', alineacion: 'der' },
    { clave: 'fefo', titulo: 'Politica', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Despacho>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fecha,desc' });

  protected readonly detalle = signal<Despacho | null>(null);

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
    });
  }

  protected tieneDesviacion(despacho: Despacho): boolean {
    return despacho.lineas.some((linea) => !linea.fefoRespetado);
  }

  protected verDetalle(despacho: Despacho): void {
    this.servicio.obtener(despacho.id).subscribe((completo) => this.detalle.set(completo));
  }
}
