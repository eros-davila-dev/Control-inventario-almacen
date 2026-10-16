import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { Dashboard } from '../../core/models/reportes.model';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { UiBadgeComponent } from '../../shared/ui/ui-badge.component';
import { UiCardComponent } from '../../shared/ui/ui-card.component';
import { UiPageHeaderComponent } from '../../shared/ui/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/ui/ui-skeleton.component';
import { UiStatComponent } from '../../shared/ui/ui-stat.component';
import { DashboardService } from './dashboard.service';

/** Tablero de control del almacen (HU-43). */
@Component({
  selector: 'sgai-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiPageHeaderComponent, UiStatComponent, UiCardComponent, UiBadgeComponent,
    UiSkeletonComponent, CantidadPipe, MonedaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Tablero de control"
                    [descripcion]="descripcion()" />

    @if (cargando()) {
      <ui-skeleton [filas]="8" />
    } @else if (datos(); as d) {
      <!-- Indicadores principales -->
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <ui-stat etiqueta="Valor del inventario" [valor]="d.valorInventario | moneda"
                 [detalle]="d.itemsConSaldo + ' item(s) con saldo'" tono="marca">
          <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.6" stroke-linecap="round">
            <path d="M3 7.5 12 3l9 4.5v9L12 21l-9-4.5v-9Z" />
          </svg>
        </ui-stat>

        <ui-stat etiqueta="Requieren reposicion" [valor]="d.itemsBajoMinimo"
                 [detalle]="d.itemsAgotados + ' agotado(s)'"
                 [tono]="d.itemsAgotados > 0 ? 'peligro' : (d.itemsBajoMinimo > 0 ? 'advertencia' : 'exito')">
          <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.6" stroke-linecap="round">
            <path d="M12 4v10m0 4h.01M4 20h16" />
          </svg>
        </ui-stat>

        <ui-stat etiqueta="Por vencer (90 dias)"
                 [valor]="d.lotesPorVencer30 + d.lotesPorVencer60 + d.lotesPorVencer90"
                 [detalle]="(d.valorExpuestoVencimiento | moneda) + ' expuestos'"
                 [tono]="d.lotesPorVencer30 > 0 ? 'advertencia' : 'info'">
          <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.6" stroke-linecap="round">
            <circle cx="12" cy="12" r="8" />
            <path d="M12 8v4l3 2" />
          </svg>
        </ui-stat>

        <ui-stat etiqueta="Lotes vencidos con saldo" [valor]="d.lotesVencidos"
                 detalle="Requieren baja o devolucion"
                 [tono]="d.lotesVencidos > 0 ? 'peligro' : 'exito'">
          <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.6" stroke-linecap="round">
            <path d="M6 6l12 12M18 6 6 18" />
          </svg>
        </ui-stat>
      </div>

      <!-- Pendientes operativos -->
      <div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <a routerLink="/despacho/requisiciones" class="block">
          <ui-stat etiqueta="Requisiciones pendientes" [valor]="d.requisicionesPendientes"
                   detalle="Enviadas o en atencion" tono="info">
            <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <path d="M9 5h6M7 7v12h10V7M9 11h6m-6 4h4" />
            </svg>
          </ui-stat>
        </a>
        <a routerLink="/compras/ordenes" class="block">
          <ui-stat etiqueta="OC por recibir" [valor]="d.ordenesPorRecibir"
                   detalle="Aprobadas o parciales" tono="info">
            <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <path d="M12 3v10m0 0 4-4m-4 4-4-4M4 17h16v4H4Z" />
            </svg>
          </ui-stat>
        </a>
        <a routerLink="/inventario/transferencias" class="block">
          <ui-stat etiqueta="En transito entre sedes" [valor]="d.transferenciasEnTransito"
                   detalle="Pendientes de confirmar" tono="advertencia">
            <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <path d="M7 8h10l-3-3m3 3-3 3M17 16H7l3-3m-3 3 3 3" />
            </svg>
          </ui-stat>
        </a>
        <a routerLink="/inventario/movimientos" class="block">
          <ui-stat etiqueta="Movimientos de hoy" [valor]="d.movimientosHoy"
                   [detalle]="d.desviacionesFefo30Dias + ' desviacion(es) FEFO en 30 dias'"
                   [tono]="d.desviacionesFefo30Dias > 0 ? 'advertencia' : 'exito'">
            <svg icono class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <path d="M3 7h13l-3-3m3 3-3 3M21 17H8l3-3m-3 3 3 3" />
            </svg>
          </ui-stat>
        </a>
      </div>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <!-- Consumo por mes -->
        <ui-card titulo="Consumo despachado por mes"
                 subtitulo="Valor de las salidas a las areas asistenciales">
          @if (consumoPorMes().length) {
            <div class="flex h-48 items-end gap-1.5">
              @for (mes of consumoPorMes(); track mes.etiqueta) {
                <div class="flex min-w-0 flex-1 flex-col items-center gap-1">
                  <span class="text-[10px] tabular-nums text-fg-subtle">{{ mes.resumen }}</span>
                  <div class="w-full rounded-t bg-brand-500/80 transition-all hover:bg-brand-600"
                       [style.height.%]="mes.porcentaje"
                       [attr.title]="mes.etiqueta + ': ' + (mes.valor | moneda)"></div>
                  <span class="truncate text-[10px] text-fg-muted">{{ mes.etiqueta }}</span>
                </div>
              }
            </div>
          } @else {
            <p class="py-8 text-center text-sm text-fg-muted">
              Aun no hay despachos registrados en el periodo.
            </p>
          }
        </ui-card>

        <!-- Valorizacion por categoria -->
        <ui-card titulo="Valorizacion por categoria"
                 subtitulo="Distribucion del valor del inventario">
          @if (d.valorizacionPorCategoria.length) {
            <ul class="space-y-3">
              @for (categoria of valorizacion(); track categoria.categoriaId) {
                <li>
                  <div class="flex items-baseline justify-between gap-2 text-sm">
                    <span class="truncate font-medium text-fg">{{ categoria.categoriaNombre }}</span>
                    <span class="shrink-0 tabular-nums text-fg-muted">
                      {{ categoria.valor | moneda }}
                    </span>
                  </div>
                  <div class="mt-1 h-2 overflow-hidden rounded-full bg-surface-muted">
                    <div class="h-full rounded-full bg-brand-500"
                         [style.width.%]="categoria.porcentaje"></div>
                  </div>
                </li>
              }
            </ul>
          } @else {
            <p class="py-8 text-center text-sm text-fg-muted">Sin saldos valorizados.</p>
          }
        </ui-card>
      </div>

      <!-- Top rotacion -->
      <div class="mt-4">
        <ui-card titulo="Productos de mayor rotacion (90 dias)"
                 subtitulo="Consumo acumulado por despachos a las areas">
          @if (d.topRotacion.length) {
            <div class="sgai-scroll -mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-border text-left text-xs uppercase tracking-wide
                             text-fg-muted">
                    <th class="py-2 pr-3 font-semibold">Producto</th>
                    <th class="py-2 pr-3 font-semibold">Codigo</th>
                    <th class="py-2 pr-3 text-right font-semibold">Consumo</th>
                    <th class="py-2 text-right font-semibold">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  @for (producto of d.topRotacion; track producto.productoId) {
                    <tr class="border-b border-border/60 last:border-0">
                      <td class="py-2 pr-3">{{ producto.nombre }}</td>
                      <td class="py-2 pr-3 font-mono text-xs text-fg-muted">{{ producto.codigo }}</td>
                      <td class="py-2 pr-3 text-right tabular-nums">
                        {{ producto.cantidadConsumida | cantidad }}
                      </td>
                      <td class="py-2 text-right tabular-nums">
                        {{ producto.valorConsumido | moneda }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="py-8 text-center text-sm text-fg-muted">
              Sin consumo registrado en los ultimos 90 dias.
            </p>
          }
        </ui-card>
      </div>

      <!-- Accesos rapidos por permiso -->
      <div class="mt-4 flex flex-wrap gap-2">
        <span *hasPermiso="'inventario:movimiento:registrar'">
          <a routerLink="/inventario/movimientos" class="inline-flex">
            <ui-badge tono="marca">Registrar movimiento</ui-badge>
          </a>
        </span>
        <span *hasPermiso="'despacho:requisicion:crear'">
          <a routerLink="/despacho/requisiciones" class="inline-flex">
            <ui-badge tono="marca">Nueva requisicion</ui-badge>
          </a>
        </span>
        <span *hasPermiso="'compras:requerimiento:crear'">
          <a routerLink="/compras/requerimientos" class="inline-flex">
            <ui-badge tono="marca">Sugerencia de reposicion</ui-badge>
          </a>
        </span>
        <span *hasPermiso="'conteo:toma:programar'">
          <a routerLink="/conteo/tomas" class="inline-flex">
            <ui-badge tono="marca">Programar conteo</ui-badge>
          </a>
        </span>
      </div>
    }
  `,
})
export class DashboardComponent {
  private readonly servicio = inject(DashboardService);
  private readonly contexto = inject(AlmacenContextoService);

  protected readonly datos = signal<Dashboard | null>(null);
  protected readonly cargando = signal(true);

  protected readonly descripcion = computed(() => {
    const almacen = this.contexto.seleccionado();
    return almacen
      ? `Situacion del almacen ${almacen.nombre} (${almacen.sede}).`
      : 'Situacion consolidada de la red de almacenes.';
  });

  /** Ultimos 12 meses de consumo, agregados por mes para el grafico. */
  protected readonly consumoPorMes = computed(() => {
    const consumo = this.datos()?.consumoMensual ?? [];
    const porMes = new Map<string, number>();
    for (const fila of consumo) {
      const clave = `${fila.anio}-${String(fila.mes).padStart(2, '0')}`;
      porMes.set(clave, (porMes.get(clave) ?? 0) + Number(fila.valor ?? 0));
    }
    const filas = [...porMes.entries()].sort(([a], [b]) => a.localeCompare(b)).slice(-12);
    const maximo = Math.max(1, ...filas.map(([, valor]) => valor));
    const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];
    return filas.map(([clave, valor]) => ({
      etiqueta: `${meses[Number(clave.slice(5)) - 1]} ${clave.slice(2, 4)}`,
      valor,
      // Etiqueta compacta: 12.4k en lugar de 12 400
      resumen: valor >= 1000 ? `${(valor / 1000).toFixed(1)}k` : String(Math.round(valor)),
      porcentaje: Math.max(4, Math.round((valor / maximo) * 100)),
    }));
  });

  protected readonly valorizacion = computed(() => {
    const filas = this.datos()?.valorizacionPorCategoria ?? [];
    const maximo = Math.max(1, ...filas.map((f) => Number(f.valor ?? 0)));
    return filas.map((fila) => ({
      ...fila,
      porcentaje: Math.max(2, Math.round((Number(fila.valor ?? 0) / maximo) * 100)),
    }));
  });

  constructor() {
    // Al cambiar el almacen de trabajo se recargan los indicadores.
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.cargando.set(true);
      this.servicio.indicadores(almacenId).subscribe({
        next: (datos) => {
          this.datos.set(datos);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
    });
  }
}
