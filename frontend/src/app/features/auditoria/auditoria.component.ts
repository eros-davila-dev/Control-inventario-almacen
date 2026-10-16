import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RegistroAuditoria } from '../../core/models/seguridad.model';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiCardComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
} from '../../shared/ui';
import { AuditoriaService } from '../seguridad/seguridad.service';

/**
 * Pista de auditoria (HU-06). Es de solo lectura e inmutable: permite responder
 * quien registro, aprobo o anulo cada documento del almacen.
 */
@Component({
  selector: 'sgai-auditoria',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiPaginationComponent,
    UiBadgeComponent, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Pista de auditoria"
                    descripcion="Registro inmutable de las operaciones del sistema, con usuario, IP y fecha." />

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-4">
        <input class="sgai-input" type="search" placeholder="Usuario..."
               (input)="lista.filtrar({ usuario: $any($event.target).value || null })" />
        <select class="sgai-input" (change)="lista.filtrar({ entidad: $any($event.target).value || null })">
          <option value="">Todas las entidades</option>
          @for (entidad of entidades; track entidad) {
            <option [value]="entidad">{{ entidad }}</option>
          }
        </select>
        <label class="block">
          <span class="sr-only">Desde</span>
          <input class="sgai-input" type="date"
                 (change)="filtrarFecha('desde', $any($event.target).value)" />
        </label>
        <label class="block">
          <span class="sr-only">Hasta</span>
          <input class="sgai-input" type="date"
                 (change)="filtrarFecha('hasta', $any($event.target).value)" />
        </label>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()">
          <ng-template #fila let-registro>
            <td class="px-3 py-2.5 text-xs tabular-nums text-fg-muted">
              {{ registro.fecha | fechaSgai: 'hora' }}
            </td>
            <td class="px-3 py-2.5 text-sm font-medium text-fg">{{ registro.usuario }}</td>
            <td class="px-3 py-2.5">
              <ui-badge [tono]="registro.exitoso ? 'neutral' : 'peligro'" [punto]="false">
                {{ registro.accion }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-sm text-fg-muted">
              {{ registro.entidad }}
              @if (registro.entidadId) {
                <span class="text-fg-subtle">#{{ registro.entidadId }}</span>
              }
            </td>
            <td class="hidden max-w-md px-3 py-2.5 text-xs text-fg-muted lg:table-cell">
              <span class="line-clamp-2">{{ registro.detalle || '—' }}</span>
            </td>
            <td class="hidden px-3 py-2.5 text-xs text-fg-subtle lg:table-cell">
              {{ registro.ip || '—' }}
            </td>
          </ng-template>
        </ui-table>
        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>
  `,
})
export class AuditoriaComponent {
  private readonly servicio = inject(AuditoriaService);

  protected readonly entidades = [
    'usuario', 'rol', 'producto', 'lote', 'movimiento', 'transferencia',
    'requerimiento_compra', 'orden_compra', 'recepcion', 'requisicion', 'despacho',
    'toma_inventario',
  ];

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'fecha', titulo: 'Fecha y hora', ancho: '11rem' },
    { clave: 'usuario', titulo: 'Usuario' },
    { clave: 'accion', titulo: 'Accion' },
    { clave: 'entidad', titulo: 'Entidad' },
    { clave: 'detalle', titulo: 'Detalle', ocultarEn: 'tablet' },
    { clave: 'ip', titulo: 'IP', ocultarEn: 'tablet' },
  ];

  protected readonly lista = new ListaPaginada<RegistroAuditoria>(
    (parametros) => this.servicio.buscar(parametros), { size: 25 });

  private readonly rango = signal<{ desde?: string; hasta?: string }>({});

  constructor() {
    this.lista.recargar();
  }

  /** La API espera marcas de tiempo ISO con zona. */
  protected filtrarFecha(limite: 'desde' | 'hasta', valor: string): void {
    this.rango.update((actual) => ({ ...actual, [limite]: valor || undefined }));
    const { desde, hasta } = this.rango();
    this.lista.filtrar({
      desde: desde ? `${desde}T00:00:00Z` : null,
      hasta: hasta ? `${hasta}T23:59:59Z` : null,
    });
  }
}
