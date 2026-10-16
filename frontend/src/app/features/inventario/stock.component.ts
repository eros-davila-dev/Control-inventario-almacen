import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Stock, StockConsolidado } from '../../core/models/inventario.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { DescargaService } from '../../core/services/descarga.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import { CatalogoRefService } from '../../shared/services/catalogo-ref.service';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiTableComponent,
  UiTabsComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { StockConsultaService } from './inventario.service';

/**
 * Consulta de saldos (HU-26). Dos vistas: consolidada por producto, con semaforo
 * de reposicion y cobertura, y detalle por lote, vencimiento y ubicacion.
 */
@Component({
  selector: 'sgai-stock',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiTabsComponent,
    UiBadgeComponent, UiButtonComponent, UiModalComponent, CantidadPipe, MonedaPipe, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Saldos de inventario"
                    [descripcion]="'Situacion de ' + (contexto.seleccionado()?.nombre ?? 'la red de almacenes')">
      <div acciones>
        <ui-button variante="secundario" (presionado)="exportar()">Exportar CSV</ui-button>
      </div>
    </ui-page-header>

    <ui-tabs [pestanas]="pestanas()" [(activa)]="vista" />

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 sm:grid-cols-2 lg:grid-cols-4">
        <input class="sgai-input lg:col-span-2" type="search"
               placeholder="Buscar producto por codigo o nombre..."
               [value]="texto()" (input)="texto.set($any($event.target).value)" />
        <select class="sgai-input" [value]="categoriaId() ?? ''"
                (change)="categoriaId.set($any($event.target).value || null)">
          <option value="">Todas las categorias</option>
          @for (categoria of catalogo.categorias(); track categoria.id) {
            <option [value]="categoria.id">{{ categoria.nombre }}</option>
          }
        </select>
        @if (vista() === 'consolidado') {
          <label class="flex items-center gap-2 text-sm text-fg">
            <input type="checkbox" class="size-4 accent-brand-600"
                   [checked]="soloReposicion()"
                   (change)="soloReposicion.set($any($event.target).checked)" />
            Solo los que requieren reposicion
          </label>
        }
      </div>

      <div class="px-4 py-3">
        @if (vista() === 'consolidado') {
          <ui-table [columnas]="columnasConsolidado" [datos]="consolidadoFiltrado()"
                    [cargando]="cargando()"
                    tituloVacio="Sin saldos"
                    descripcionVacio="No hay productos con saldo en el alcance seleccionado.">
            <ng-template #fila let-saldo>
              <td class="px-3 py-2.5">
                <p class="font-medium text-fg">{{ saldo.productoNombre }}</p>
                <p class="font-mono text-xs text-fg-muted">
                  {{ saldo.productoCodigo }} · {{ saldo.unidadMedida }}
                  @if (saldo.critico) { · critico }
                </p>
              </td>
              <td class="hidden px-3 py-2.5 text-fg-muted sm:table-cell">{{ saldo.almacenNombre }}</td>
              <td class="px-3 py-2.5 text-right tabular-nums font-medium">
                {{ saldo.disponible | cantidad }}
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums text-fg-muted lg:table-cell">
                {{ saldo.stockMinimo | cantidad }} / {{ saldo.puntoReorden | cantidad }}
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                @if (saldo.coberturaDias !== null && saldo.coberturaDias !== undefined) {
                  <span [class.text-danger]="saldo.coberturaDias < 15"
                        [class.font-medium]="saldo.coberturaDias < 15">
                    {{ saldo.coberturaDias | cantidad: 1 }} d
                  </span>
                } @else {
                  <span class="text-fg-subtle">—</span>
                }
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                {{ saldo.valorizado | moneda }}
              </td>
              <td class="px-3 py-2.5 text-center">
                <ui-badge [tono]="tono(saldo.estadoReposicion)">
                  {{ etiqueta(saldo.estadoReposicion) }}
                </ui-badge>
              </td>
              <td class="px-3 py-2.5 text-right">
                <button type="button"
                        class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                               hover:bg-surface-muted dark:text-brand-300"
                        (click)="verLotes(saldo)">Ver lotes</button>
                <a [routerLink]="['/inventario/kardex']"
                   [queryParams]="{ productoId: saldo.productoId, almacenId: saldo.almacenId }"
                   class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                          hover:bg-surface-muted hover:text-fg">Kardex</a>
              </td>
            </ng-template>

            <ng-template #tarjeta let-saldo>
              <article class="sgai-card p-3">
                <div class="flex items-start justify-between gap-2">
                  <div class="min-w-0">
                    <p class="truncate text-sm font-medium text-fg">{{ saldo.productoNombre }}</p>
                    <p class="font-mono text-xs text-fg-muted">{{ saldo.productoCodigo }}</p>
                  </div>
                  <ui-badge [tono]="tono(saldo.estadoReposicion)">
                    {{ etiqueta(saldo.estadoReposicion) }}
                  </ui-badge>
                </div>
                <dl class="mt-2 grid grid-cols-3 gap-2 text-xs">
                  <div><dt class="text-fg-subtle">Disponible</dt>
                    <dd class="tabular-nums font-medium text-fg">{{ saldo.disponible | cantidad }}</dd></div>
                  <div><dt class="text-fg-subtle">Minimo</dt>
                    <dd class="tabular-nums text-fg">{{ saldo.stockMinimo | cantidad }}</dd></div>
                  <div><dt class="text-fg-subtle">Cobertura</dt>
                    <dd class="tabular-nums text-fg">
                      {{ saldo.coberturaDias !== null ? (saldo.coberturaDias | cantidad: 1) + ' d' : '—' }}
                    </dd></div>
                </dl>
                <div class="mt-2 flex justify-end">
                  <ui-button tamano="sm" variante="secundario" (presionado)="verLotes(saldo)">
                    Ver lotes
                  </ui-button>
                </div>
              </article>
            </ng-template>
          </ui-table>
        } @else {
          <ui-table [columnas]="columnasDetalle" [datos]="detalle()" [cargando]="cargando()"
                    tituloVacio="Sin saldos por lote">
            <ng-template #fila let-saldo>
              <td class="px-3 py-2.5">
                <p class="font-medium text-fg">{{ saldo.productoNombre }}</p>
                <p class="font-mono text-xs text-fg-muted">{{ saldo.productoCodigo }}</p>
              </td>
              <td class="px-3 py-2.5">
                @if (saldo.codigoLote) {
                  <p class="font-mono text-sm text-fg">{{ saldo.codigoLote }}</p>
                  <p class="text-xs"
                     [class.text-danger]="(saldo.diasParaVencer ?? 999) < 30"
                     [class.text-fg-muted]="(saldo.diasParaVencer ?? 999) >= 30">
                    {{ saldo.fechaVencimiento | fechaSgai }}
                    @if (saldo.diasParaVencer !== null && saldo.diasParaVencer !== undefined) {
                      ({{ saldo.diasParaVencer }} d)
                    }
                  </p>
                } @else {
                  <span class="text-xs text-fg-subtle">Sin control de lote</span>
                }
              </td>
              <td class="hidden px-3 py-2.5 text-center sm:table-cell">
                @if (saldo.estadoLote) {
                  <ui-badge [tono]="tono(saldo.estadoLote)">{{ etiqueta(saldo.estadoLote) }}</ui-badge>
                }
              </td>
              <td class="hidden px-3 py-2.5 font-mono text-xs text-fg-muted lg:table-cell">
                {{ saldo.ubicacionCodigo || '—' }}
              </td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ saldo.cantidad | cantidad }}</td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums text-fg-muted lg:table-cell">
                {{ saldo.reservada | cantidad }}
              </td>
              <td class="px-3 py-2.5 text-right tabular-nums font-medium">
                {{ saldo.disponible | cantidad }}
              </td>
              <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                {{ saldo.valorizado | moneda }}
              </td>
            </ng-template>
          </ui-table>
        }
      </div>
    </ui-card>

    <!-- Detalle por lote de un producto -->
    <ui-modal [abierto]="!!productoDetalle()" tamano="lg"
              [titulo]="productoDetalle()?.productoNombre ?? ''"
              subtitulo="Saldo por lote, vencimiento y ubicacion"
              (cerrado)="productoDetalle.set(null)">
      <ui-table [columnas]="columnasLotes" [datos]="lotesProducto()" [cargando]="cargandoLotes()">
        <ng-template #fila let-saldo>
          <td class="px-3 py-2 font-mono text-sm">{{ saldo.codigoLote || 'SIN LOTE' }}</td>
          <td class="px-3 py-2 text-sm">
            {{ saldo.fechaVencimiento | fechaSgai }}
            @if (saldo.diasParaVencer !== null && saldo.diasParaVencer !== undefined) {
              <span class="text-xs" [class.text-danger]="saldo.diasParaVencer < 30">
                ({{ saldo.diasParaVencer }} d)
              </span>
            }
          </td>
          <td class="px-3 py-2 text-center">
            @if (saldo.estadoLote) {
              <ui-badge [tono]="tono(saldo.estadoLote)">{{ etiqueta(saldo.estadoLote) }}</ui-badge>
            }
          </td>
          <td class="px-3 py-2 font-mono text-xs text-fg-muted">{{ saldo.ubicacionCodigo || '—' }}</td>
          <td class="px-3 py-2 text-right tabular-nums">{{ saldo.cantidad | cantidad }}</td>
          <td class="px-3 py-2 text-right tabular-nums font-medium">{{ saldo.disponible | cantidad }}</td>
        </ng-template>
      </ui-table>
      <div pie>
        <ui-button variante="secundario" (presionado)="productoDetalle.set(null)">Cerrar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class StockComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  protected readonly catalogo = inject(CatalogoRefService);
  private readonly servicio = inject(StockConsultaService);
  private readonly descarga = inject(DescargaService);

  protected readonly vista = signal<'consolidado' | 'detalle'>('consolidado');
  protected readonly texto = signal('');
  protected readonly categoriaId = signal<string | null>(null);
  protected readonly soloReposicion = signal(false);
  protected readonly cargando = signal(false);
  protected readonly consolidado = signal<StockConsolidado[]>([]);
  protected readonly detalle = signal<Stock[]>([]);
  protected readonly productoDetalle = signal<StockConsolidado | null>(null);
  protected readonly lotesProducto = signal<Stock[]>([]);
  protected readonly cargandoLotes = signal(false);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly columnasConsolidado: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'almacen', titulo: 'Almacen', ocultarEn: 'movil' },
    { clave: 'disponible', titulo: 'Disponible', alineacion: 'der' },
    { clave: 'parametros', titulo: 'Min / reorden', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'cobertura', titulo: 'Cobertura', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'valor', titulo: 'Valorizado', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Reposicion', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly columnasDetalle: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'lote', titulo: 'Lote / vencimiento' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro', ocultarEn: 'movil' },
    { clave: 'ubicacion', titulo: 'Ubicacion', ocultarEn: 'tablet' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'reservada', titulo: 'Reservada', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'disponible', titulo: 'Disponible', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valorizado', alineacion: 'der', ocultarEn: 'tablet' },
  ];

  protected readonly columnasLotes: ColumnaTabla[] = [
    { clave: 'lote', titulo: 'Lote' },
    { clave: 'vencimiento', titulo: 'Vence' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'ubicacion', titulo: 'Ubicacion' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'disponible', titulo: 'Disponible', alineacion: 'der' },
  ];

  protected readonly pestanas = computed(() => [
    { clave: 'consolidado', titulo: 'Consolidado por producto', contador: this.consolidado().length },
    { clave: 'detalle', titulo: 'Detalle por lote', contador: this.detalle().length },
  ]);

  protected readonly consolidadoFiltrado = computed(() => {
    const termino = this.texto().trim().toLowerCase();
    return this.consolidado().filter((saldo) => !termino
      || saldo.productoNombre.toLowerCase().includes(termino)
      || saldo.productoCodigo.toLowerCase().includes(termino));
  });

  constructor() {
    this.catalogo.categoriasActivas().subscribe();
    effect(() => {
      // Se recarga al cambiar de almacen, vista o filtros del servidor.
      const almacenId = this.contexto.seleccionadoId();
      const categoriaId = this.categoriaId();
      const soloReposicion = this.soloReposicion();
      const vista = this.vista();
      this.cargando.set(true);

      if (vista === 'consolidado') {
        this.servicio.consolidado({ almacenId, categoriaId, soloReposicion }).subscribe({
          next: (datos) => {
            this.consolidado.set(datos);
            this.cargando.set(false);
          },
          error: () => this.cargando.set(false),
        });
      } else {
        this.servicio.buscar({
          almacenId, categoriaId, texto: this.texto() || null, soloConSaldo: true, size: 200,
        }).subscribe({
          next: (pagina) => {
            this.detalle.set(pagina.contenido);
            this.cargando.set(false);
          },
          error: () => this.cargando.set(false),
        });
      }
    });
  }

  protected verLotes(saldo: StockConsolidado): void {
    this.productoDetalle.set(saldo);
    this.cargandoLotes.set(true);
    this.servicio.porProductoYAlmacen(saldo.productoId, saldo.almacenId).subscribe({
      next: (lotes) => {
        this.lotesProducto.set(lotes);
        this.cargandoLotes.set(false);
      },
      error: () => this.cargandoLotes.set(false),
    });
  }

  protected exportar(): void {
    this.descarga.descargarCsv('/reportes/stock.csv', 'saldos-inventario.csv',
      { almacenId: this.contexto.seleccionadoId() });
  }
}
