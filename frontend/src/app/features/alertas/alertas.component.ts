import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Alerta, ResumenAlertas, SeveridadAlerta } from '../../core/models/alertas.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import {
  TonoBadge,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiEmptyComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiSkeletonComponent,
  UiStatComponent,
} from '../../shared/ui';
import { AlertaService } from './alertas.service';

/** Tonos y accesos directos por tipo de alerta: cada alerta debe llevar a la accion. */
const ACCION_POR_TIPO: Record<string, { ruta: string; texto: string }> = {
  STOCK_AGOTADO: { ruta: '/compras/requerimientos', texto: 'Generar reposicion' },
  STOCK_BAJO: { ruta: '/compras/requerimientos', texto: 'Generar reposicion' },
  SOBRE_STOCK: { ruta: '/inventario/stock', texto: 'Ver stock' },
  POR_VENCER: { ruta: '/inventario/lotes', texto: 'Priorizar salida (FEFO)' },
  VENCIDO: { ruta: '/inventario/lotes', texto: 'Retirar y dar de baja' },
  LOTE_RETENIDO: { ruta: '/inventario/lotes', texto: 'Revisar lote' },
  SIN_MOVIMIENTO: { ruta: '/inventario/stock', texto: 'Evaluar rotacion' },
  REGISTRO_SANITARIO: { ruta: '/catalogo/productos', texto: 'Actualizar registro' },
};

/**
 * Centro de alertas (HU-40 a HU-43). Resuelve el problema de la gestion
 * reactiva: vencimientos, quiebres y registros sanitarios se avisan antes de
 * que generen perdida, con el acceso directo a la pantalla donde se corrigen.
 */
@Component({
  selector: 'sgai-alertas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiPageHeaderComponent, UiCardComponent, UiStatComponent, UiBadgeComponent,
    UiButtonComponent, UiEmptyComponent, UiSkeletonComponent, UiPaginationComponent,
    CantidadPipe, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Centro de alertas"
                    descripcion="Vencimientos, quiebres de stock, lotes retenidos y registros sanitarios por vencer.">
      <div acciones>
        <ui-button variante="secundario" [cargando]="recalculando()" (presionado)="recalcular()">
          Recalcular ahora
        </ui-button>
      </div>
    </ui-page-header>

    @let r = resumen();
    @if (r) {
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <ui-stat etiqueta="Alertas abiertas" [valor]="r.total"
                 [detalle]="r.criticas + ' criticas · ' + r.altas + ' altas'"
                 [tono]="r.criticas > 0 ? 'peligro' : (r.total > 0 ? 'advertencia' : 'exito')" />
        <ui-stat etiqueta="Por vencer" [valor]="r.porVencer"
                 detalle="Lotes dentro de la ventana de alerta" tono="advertencia" />
        <ui-stat etiqueta="Vencidos" [valor]="r.vencidos"
                 detalle="Retirar del almacen y dar de baja"
                 [tono]="r.vencidos > 0 ? 'peligro' : 'exito'" />
        <ui-stat etiqueta="Stock bajo o agotado" [valor]="r.stockBajo"
                 detalle="Productos bajo su punto de reorden"
                 [tono]="r.stockBajo > 0 ? 'advertencia' : 'exito'" />
      </div>
    } @else {
      <ui-skeleton [filas]="2" />
    }

    <ui-card class="mt-4" [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-3">
        <label class="block">
          <span class="sgai-label">Tipo</span>
          <select class="sgai-input"
                  (change)="lista.filtrar({ tipo: $any($event.target).value || null })">
            <option value="">Todos los tipos</option>
            <option value="STOCK_AGOTADO">Stock agotado</option>
            <option value="STOCK_BAJO">Stock bajo</option>
            <option value="SOBRE_STOCK">Sobre stock</option>
            <option value="POR_VENCER">Por vencer</option>
            <option value="VENCIDO">Vencido</option>
            <option value="LOTE_RETENIDO">Lote retenido</option>
            <option value="SIN_MOVIMIENTO">Sin movimiento</option>
            <option value="REGISTRO_SANITARIO">Registro sanitario</option>
          </select>
        </label>
        <label class="block">
          <span class="sgai-label">Severidad</span>
          <select class="sgai-input"
                  (change)="lista.filtrar({ severidad: $any($event.target).value || null })">
            <option value="">Todas</option>
            <option value="CRITICA">Critica</option>
            <option value="ALTA">Alta</option>
            <option value="MEDIA">Media</option>
            <option value="BAJA">Baja</option>
          </select>
        </label>
        <label class="block">
          <span class="sgai-label">Estado</span>
          <select class="sgai-input" [value]="estadoActual()"
                  (change)="cambiarEstado($any($event.target).value)">
            <option value="ACTIVA">Activas</option>
            <option value="LEIDA">Leidas</option>
            <option value="RESUELTA">Resueltas</option>
            <option value="">Todas</option>
          </select>
        </label>
      </div>

      <div class="px-4 py-3">
        @if (lista.cargando() && !lista.datos().length) {
          <ui-skeleton [filas]="5" />
        } @else if (!lista.datos().length) {
          <ui-empty titulo="Sin alertas"
                    descripcion="No hay alertas con estos filtros. El sistema reevalua las reglas cada madrugada." />
        } @else {
          <ul class="space-y-2">
            @for (alerta of lista.datos(); track alerta.id) {
              <li class="rounded-xl border px-4 py-3"
                  [class]="borde(alerta.severidad)">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <ui-badge [tono]="tonoSeveridad(alerta.severidad)">
                        {{ alerta.severidad }}
                      </ui-badge>
                      <span class="text-xs font-medium uppercase tracking-wide text-fg-muted">
                        {{ alerta.tipoDescripcion }}
                      </span>
                      @if (alerta.estado !== 'ACTIVA') {
                        <ui-badge tono="neutral">{{ alerta.estado }}</ui-badge>
                      }
                    </div>
                    <p class="mt-1 text-sm text-fg">{{ alerta.mensaje }}</p>
                    <p class="mt-0.5 text-xs text-fg-muted">
                      {{ alerta.fechaGeneracion | fechaSgai: 'hora' }}
                      @if (alerta.almacenNombre) { · {{ alerta.almacenNombre }} }
                      @if (alerta.codigoLote) { · lote {{ alerta.codigoLote }} }
                      @if (alerta.valorReferencia !== null && alerta.valorReferencia !== undefined) {
                        · referencia {{ alerta.valorReferencia | cantidad }}
                      }
                    </p>
                  </div>

                  <div class="flex shrink-0 flex-wrap items-center gap-2">
                    @let accion = accionDe(alerta.tipoAlerta);
                    @if (accion) {
                      <a [routerLink]="accion.ruta"
                         class="rounded-lg border border-border px-2.5 py-1 text-xs font-medium
                                text-brand-700 hover:bg-surface-muted dark:text-brand-300">
                        {{ accion.texto }}
                      </a>
                    }
                    @if (alerta.estado === 'ACTIVA') {
                      <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                     text-fg-muted hover:bg-surface-muted"
                              (click)="marcarLeida(alerta)">Marcar leida</button>
                    }
                    @if (alerta.estado !== 'RESUELTA') {
                      <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                     text-success hover:bg-success-soft"
                              (click)="resolver(alerta)">Resolver</button>
                    }
                  </div>
                </div>
              </li>
            }
          </ul>

          <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                         [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                         (paginaCambiada)="lista.irAPagina($event)"
                         (tamanoCambiado)="lista.cambiarTamano($event)" />
        }
      </div>
    </ui-card>
  `,
})
export class AlertasComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(AlertaService);
  private readonly notificacion = inject(NotificacionService);

  protected readonly resumen = signal<ResumenAlertas | null>(null);
  protected readonly recalculando = signal(false);
  protected readonly estadoActual = signal('ACTIVA');

  protected readonly lista = new ListaPaginada<Alerta>(
    (parametros) => this.servicio.buscar(parametros),
    { sort: 'fechaGeneracion,desc', estado: 'ACTIVA' });

  constructor() {
    effect(() => {
      const almacenId = this.contexto.seleccionadoId();
      this.lista.filtrar({ almacenId });
      this.servicio.resumen(almacenId).subscribe((resumen) => this.resumen.set(resumen));
    });
  }

  protected tonoSeveridad(severidad: SeveridadAlerta): TonoBadge {
    switch (severidad) {
      case 'CRITICA':
        return 'peligro';
      case 'ALTA':
        return 'advertencia';
      case 'MEDIA':
        return 'info';
      default:
        return 'neutral';
    }
  }

  protected borde(severidad: SeveridadAlerta): string {
    switch (severidad) {
      case 'CRITICA':
        return 'border-danger/40 bg-danger-soft/40';
      case 'ALTA':
        return 'border-warning/40 bg-warning-soft/30';
      default:
        return 'border-border bg-surface';
    }
  }

  protected accionDe(tipo: string): { ruta: string; texto: string } | null {
    return ACCION_POR_TIPO[tipo] ?? null;
  }

  protected cambiarEstado(estado: string): void {
    this.estadoActual.set(estado);
    this.lista.filtrar({ estado: estado || null });
  }

  protected marcarLeida(alerta: Alerta): void {
    this.servicio.marcarLeida(alerta.id).subscribe(() => this.refrescar());
  }

  protected resolver(alerta: Alerta): void {
    this.servicio.resolver(alerta.id).subscribe(() => {
      this.notificacion.exito('Alerta resuelta');
      this.refrescar();
    });
  }

  protected recalcular(): void {
    this.recalculando.set(true);
    this.servicio.recalcular(this.contexto.seleccionadoId()).subscribe({
      next: (resumen) => {
        this.recalculando.set(false);
        this.resumen.set(resumen);
        this.lista.recargar();
        this.notificacion.exito('Alertas recalculadas', `${resumen.total} alertas abiertas`);
      },
      error: () => this.recalculando.set(false),
    });
  }

  private refrescar(): void {
    this.lista.recargar();
    this.servicio.resumen(this.contexto.seleccionadoId())
      .subscribe((resumen) => this.resumen.set(resumen));
  }
}
