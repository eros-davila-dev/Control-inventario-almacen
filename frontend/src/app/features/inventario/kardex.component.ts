import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Kardex } from '../../core/models/inventario.model';
import { ProductoResumen } from '../../core/models/catalogo.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { DescargaService } from '../../core/services/descarga.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import { SelectorProductoComponent } from '../../shared/ui/selector-producto.component';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiEmptyComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
} from '../../shared/ui';
import { KardexConsultaService } from './inventario.service';

/**
 * Kardex valorizado por costo promedio ponderado (HU-25).
 * Es de solo lectura: las correcciones se hacen con movimientos inversos.
 */
@Component({
  selector: 'sgai-kardex',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiPaginationComponent,
    UiButtonComponent, UiBadgeComponent, UiEmptyComponent, SelectorProductoComponent,
    CantidadPipe, MonedaPipe, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Kardex valorizado"
                    descripcion="Movimientos, saldo y costo promedio ponderado de un producto.">
      <div acciones>
        @if (producto()) {
          <ui-button variante="secundario" (presionado)="exportar()">Exportar CSV</ui-button>
        }
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="grid gap-3 border-b border-border px-4 py-3 lg:grid-cols-4">
        <div class="lg:col-span-2">
          <sgai-selector-producto etiqueta="Producto"
                                  (seleccionado)="elegirProducto($event)" />
        </div>
        <label class="block">
          <span class="sgai-label">Desde</span>
          <input class="sgai-input" type="date" [value]="desde()"
                 (change)="desde.set($any($event.target).value); consultar()" />
        </label>
        <label class="block">
          <span class="sgai-label">Hasta</span>
          <input class="sgai-input" type="date" [value]="hasta()"
                 (change)="hasta.set($any($event.target).value); consultar()" />
        </label>
      </div>

      @if (producto()) {
        <div class="flex flex-wrap items-center gap-2 border-b border-border bg-surface-muted/60
                    px-4 py-2.5">
          <ui-badge tono="marca" [punto]="false">{{ producto()!.codigo }}</ui-badge>
          <span class="text-sm font-medium text-fg">{{ producto()!.nombre }}</span>
          <span class="text-xs text-fg-muted">
            Almacen: {{ contexto.seleccionado()?.nombre ?? 'todos' }}
          </span>
        </div>
      }

      <div class="px-4 py-3">
        @if (!producto()) {
          <ui-empty titulo="Seleccione un producto"
                    descripcion="El kardex se consulta por producto y, opcionalmente, por almacen y rango de fechas." />
        } @else {
          <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                    tituloVacio="Sin movimientos en el periodo">
            <ng-template #fila let-linea>
              <td class="px-3 py-2 text-xs tabular-nums text-fg-muted">
                {{ linea.fecha | fechaSgai }}
              </td>
              <td class="px-3 py-2 font-mono text-xs">{{ linea.numeroDocumento }}</td>
              <td class="hidden px-3 py-2 text-xs text-fg-muted sm:table-cell">
                {{ linea.tipoDescripcion }}
              </td>
              <td class="hidden px-3 py-2 font-mono text-xs lg:table-cell">
                {{ linea.codigoLote || '—' }}
              </td>
              <td class="px-3 py-2 text-right tabular-nums text-success">
                {{ linea.entrada > 0 ? (linea.entrada | cantidad) : '' }}
              </td>
              <td class="px-3 py-2 text-right tabular-nums text-danger">
                {{ linea.salida > 0 ? (linea.salida | cantidad) : '' }}
              </td>
              <td class="px-3 py-2 text-right font-medium tabular-nums">
                {{ linea.saldo | cantidad }}
              </td>
              <td class="hidden px-3 py-2 text-right tabular-nums lg:table-cell">
                {{ linea.costoUnitario | moneda }}
              </td>
              <td class="px-3 py-2 text-right tabular-nums">{{ linea.costoPromedio | moneda }}</td>
              <td class="hidden px-3 py-2 text-right tabular-nums lg:table-cell">
                {{ linea.valorSaldo | moneda }}
              </td>
            </ng-template>
          </ui-table>

          <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                         [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                         (paginaCambiada)="lista.irAPagina($event)"
                         (tamanoCambiado)="lista.cambiarTamano($event)" />
        }
      </div>
    </ui-card>
  `,
})
export class KardexComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(KardexConsultaService);
  private readonly descarga = inject(DescargaService);
  private readonly notificacion = inject(NotificacionService);

  protected readonly producto = signal<ProductoResumen | null>(null);
  protected readonly desde = signal('');
  protected readonly hasta = signal('');

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'fecha', titulo: 'Fecha' },
    { clave: 'documento', titulo: 'Documento' },
    { clave: 'tipo', titulo: 'Tipo', ocultarEn: 'movil' },
    { clave: 'lote', titulo: 'Lote', ocultarEn: 'tablet' },
    { clave: 'entrada', titulo: 'Entrada', alineacion: 'der' },
    { clave: 'salida', titulo: 'Salida', alineacion: 'der' },
    { clave: 'saldo', titulo: 'Saldo', alineacion: 'der' },
    { clave: 'costoUnitario', titulo: 'Costo unit.', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'costoPromedio', titulo: 'Costo prom.', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valor saldo', alineacion: 'der', ocultarEn: 'tablet' },
  ];

  protected readonly lista = new ListaPaginada<Kardex>(
    (parametros) => this.servicio.consultar(parametros), { size: 50 });

  constructor() {
    // Permite llegar desde el catalogo o el stock con el producto preseleccionado.
    const parametros = new URLSearchParams(window.location.search);
    const productoId = parametros.get('productoId');
    if (productoId) {
      this.lista.filtrar({
        productoId: Number(productoId),
        almacenId: parametros.get('almacenId') ?? this.contexto.seleccionadoId(),
      });
      this.producto.set({
        id: Number(productoId), codigo: '—', nombre: 'Producto seleccionado',
        unidadMedida: '', controlaLote: false, controlaVencimiento: false, critico: false,
      });
    }
  }

  protected elegirProducto(producto: ProductoResumen): void {
    this.producto.set(producto);
    this.consultar();
  }

  protected consultar(): void {
    const producto = this.producto();
    if (!producto) {
      this.notificacion.advertencia('Seleccione un producto');
      return;
    }
    this.lista.filtrar({
      productoId: producto.id,
      almacenId: this.contexto.seleccionadoId(),
      desde: this.desde() || null,
      hasta: this.hasta() || null,
    });
  }

  protected exportar(): void {
    const producto = this.producto();
    if (!producto) {
      return;
    }
    this.descarga.descargarCsv('/reportes/kardex.csv', `kardex-${producto.codigo}.csv`, {
      productoId: producto.id,
      almacenId: this.contexto.seleccionadoId(),
      desde: this.desde() || null,
      hasta: this.hasta() || null,
    });
  }
}
