import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { StockConsolidado } from '../../core/models/inventario.model';
import {
  BajaRotacion,
  ConsumoMensual,
  LoteVencimiento,
  RotacionProducto,
  ValorizacionCategoria,
} from '../../core/models/reportes.model';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { DescargaService } from '../../core/services/descarga.service';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiPageHeaderComponent,
  UiTableComponent,
  UiTabsComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { ReporteService } from './reportes.service';

type Vista = 'vencimientos' | 'valorizacion' | 'consumo' | 'rotacion' | 'inmovilizado' | 'reposicion';

const MESES = ['', 'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Set', 'Oct', 'Nov', 'Dic'];

/**
 * Reportes de gestion (HU-44, HU-45). Cada reporte responde a un objetivo del
 * proyecto: vencimientos al 3,1 % de merma, reposicion a los quiebres, consumo
 * por area al costeo y el inmovilizado al capital detenido en el almacen.
 */
@Component({
  selector: 'sgai-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiPageHeaderComponent, UiCardComponent, UiTableComponent, UiTabsComponent, UiButtonComponent,
    UiBadgeComponent, CantidadPipe, MonedaPipe, FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Reportes de gestion"
                    [descripcion]="'Informacion consolidada de ' + (contexto.seleccionado()?.nombre ?? 'todos los almacenes autorizados') + '.'">
      <div acciones>
        @if (exportable()) {
          <ui-button variante="secundario" (presionado)="exportar()">Exportar CSV</ui-button>
        }
      </div>
    </ui-page-header>

    <ui-tabs [pestanas]="pestanas" [(activa)]="vista" />

    @switch (vista()) {
      @case ('vencimientos') {
        <ui-card titulo="Lotes por vencer y vencidos"
                 subtitulo="Valor expuesto por lote segun la ventana de dias; base del indicador de merma."
                 [sinRelleno]="true">
          <div class="flex flex-wrap items-end gap-3 border-b border-border px-4 py-3">
            <label class="block">
              <span class="sgai-label">Ventana (dias)</span>
              <select class="sgai-input w-32" [value]="dias()"
                      (change)="cambiarDias($any($event.target).value)">
                <option value="30">30 dias</option>
                <option value="60">60 dias</option>
                <option value="90">90 dias</option>
                <option value="180">180 dias</option>
              </select>
            </label>
            <p class="pb-2 text-sm text-fg-muted">
              Valor expuesto total:
              <strong class="tabular-nums text-danger">
                {{ valorExpuesto() | moneda }}
              </strong>
              en {{ vencimientos().length }} lotes
            </p>
          </div>
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasVencimientos" [datos]="vencimientos()"
                      [cargando]="cargando()" tituloVacio="Sin lotes en riesgo"
                      descripcionVacio="Ningun lote vence dentro de la ventana seleccionada.">
              <ng-template #fila let-lote>
                <td class="px-3 py-2.5">
                  <p class="text-sm text-fg">{{ lote.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ lote.productoCodigo }}</p>
                </td>
                <td class="px-3 py-2.5 font-mono text-xs text-fg">{{ lote.codigoLote }}</td>
                <td class="px-3 py-2.5 text-sm">
                  <span [class.text-danger]="diasPara(lote.fechaVencimiento) <= 0"
                        [class.text-warning]="diasPara(lote.fechaVencimiento) > 0
                          && diasPara(lote.fechaVencimiento) <= 30">
                    {{ lote.fechaVencimiento | fechaSgai }}
                  </span>
                  <p class="text-[11px] text-fg-subtle">
                    {{ diasPara(lote.fechaVencimiento) <= 0
                       ? 'vencido' : diasPara(lote.fechaVencimiento) + ' dias' }}
                  </p>
                </td>
                <td class="hidden px-3 py-2.5 text-sm text-fg-muted lg:table-cell">
                  {{ lote.almacenNombre }}
                </td>
                <td class="px-3 py-2.5 text-center">
                  <ui-badge [tono]="tono(lote.estadoLote)">{{ etiqueta(lote.estadoLote) }}</ui-badge>
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ lote.cantidad | cantidad }}</td>
                <td class="px-3 py-2.5 text-right font-medium tabular-nums text-danger">
                  {{ lote.valorExpuesto | moneda }}
                </td>
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }

      @case ('valorizacion') {
        <ui-card titulo="Valorizacion del inventario por categoria"
                 subtitulo="Costo promedio ponderado por categoria, para el cierre contable mensual."
                 [sinRelleno]="true">
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasValorizacion" [datos]="valorizacion()"
                      [cargando]="cargando()" tituloVacio="Sin saldos valorizados">
              <ng-template #fila let-grupo>
                <td class="px-3 py-2.5 text-sm text-fg">{{ grupo.categoriaNombre }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums text-fg-muted">
                  {{ grupo.productos }}
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ grupo.cantidad | cantidad }}</td>
                <td class="px-3 py-2.5 text-right font-medium tabular-nums text-fg">
                  {{ grupo.valor | moneda }}
                </td>
                <td class="px-3 py-2.5">
                  <div class="h-2 overflow-hidden rounded-full bg-surface-muted">
                    <div class="h-full rounded-full bg-brand-500"
                         [style.width.%]="participacion(grupo.valor)"></div>
                  </div>
                  <p class="mt-0.5 text-[11px] tabular-nums text-fg-subtle">
                    {{ participacion(grupo.valor) }} %
                  </p>
                </td>
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }

      @case ('consumo') {
        <ui-card titulo="Consumo por area y centro de costo"
                 subtitulo="Lo despachado a cada area, valorizado al costo de salida."
                 [sinRelleno]="true">
          <div class="border-b border-border px-4 py-3">
            <label class="block max-w-xs">
              <span class="sgai-label">Periodo</span>
              <select class="sgai-input" [value]="meses()"
                      (change)="cambiarMeses($any($event.target).value)">
                <option value="3">Ultimos 3 meses</option>
                <option value="6">Ultimos 6 meses</option>
                <option value="12">Ultimos 12 meses</option>
              </select>
            </label>
          </div>
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasConsumo" [datos]="consumo()" [cargando]="cargando()"
                      tituloVacio="Sin consumo registrado">
              <ng-template #fila let-linea>
                <td class="px-3 py-2.5 text-sm text-fg">{{ periodo(linea) }}</td>
                <td class="px-3 py-2.5 text-sm text-fg">{{ linea.area }}</td>
                <td class="hidden px-3 py-2.5 font-mono text-xs text-fg-muted sm:table-cell">
                  {{ linea.centroCosto || '—' }}
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ linea.cantidad | cantidad }}</td>
                <td class="px-3 py-2.5 text-right font-medium tabular-nums text-fg">
                  {{ linea.valor | moneda }}
                </td>
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }

      @case ('rotacion') {
        <ui-card titulo="Rotacion de productos"
                 subtitulo="Consumo acumulado del periodo: orienta la clasificacion ABC y los parametros de reposicion."
                 [sinRelleno]="true">
          <div class="border-b border-border px-4 py-3">
            <label class="block max-w-xs">
              <span class="sgai-label">Periodo</span>
              <select class="sgai-input" [value]="dias()"
                      (change)="cambiarDias($any($event.target).value)">
                <option value="30">30 dias</option>
                <option value="90">90 dias</option>
                <option value="180">180 dias</option>
                <option value="365">12 meses</option>
              </select>
            </label>
          </div>
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasRotacion" [datos]="rotacion()" [cargando]="cargando()"
                      tituloVacio="Sin salidas en el periodo">
              <ng-template #fila let-producto>
                <td class="px-3 py-2.5">
                  <p class="text-sm text-fg">{{ producto.nombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ producto.codigo }}</p>
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">
                  {{ producto.cantidadConsumida | cantidad }}
                </td>
                <td class="px-3 py-2.5 text-right font-medium tabular-nums text-fg">
                  {{ producto.valorConsumido | moneda }}
                </td>
                <td class="hidden px-3 py-2.5 text-sm text-fg-muted sm:table-cell">
                  {{ producto.ultimaSalida ? (producto.ultimaSalida | fechaSgai) : '—' }}
                </td>
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }

      @case ('inmovilizado') {
        <ui-card titulo="Inventario inmovilizado"
                 subtitulo="Saldo sin movimiento: capital detenido y riesgo de obsolescencia."
                 [sinRelleno]="true">
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasInmovilizado" [datos]="inmovilizado()"
                      [cargando]="cargando()" tituloVacio="Sin saldos inmovilizados">
              <ng-template #fila let-item>
                <td class="px-3 py-2.5">
                  <p class="text-sm text-fg">{{ item.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">{{ item.productoCodigo }}</p>
                </td>
                <td class="hidden px-3 py-2.5 text-sm text-fg-muted lg:table-cell">
                  {{ item.almacenNombre }}
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ item.cantidad | cantidad }}</td>
                <td class="px-3 py-2.5 text-right font-medium tabular-nums text-fg">
                  {{ item.valorizado | moneda }}
                </td>
                <td class="px-3 py-2.5 text-right text-sm">
                  @if (item.diasSinMovimiento !== null && item.diasSinMovimiento !== undefined) {
                    <span class="tabular-nums"
                          [class.text-danger]="item.diasSinMovimiento >= 180"
                          [class.text-warning]="item.diasSinMovimiento >= 90
                            && item.diasSinMovimiento < 180">
                      {{ item.diasSinMovimiento }} d
                    </span>
                  } @else {
                    <span class="text-fg-subtle">sin salidas</span>
                  }
                  <p class="text-[11px] text-fg-subtle">
                    {{ item.ultimoMovimiento ? (item.ultimoMovimiento | fechaSgai) : '' }}
                  </p>
                </td>
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }

      @case ('reposicion') {
        <ui-card titulo="Productos que requieren reposicion"
                 subtitulo="Saldo por debajo del punto de reorden, con la cobertura en dias segun consumo."
                 [sinRelleno]="true">
          <div class="px-4 py-3">
            <ui-table [columnas]="columnasReposicion" [datos]="reposicion()"
                      [cargando]="cargando()" tituloVacio="Nada por reponer"
                      descripcionVacio="Todos los productos estan sobre su punto de reorden.">
              <ng-template #fila let-item>
                <td class="px-3 py-2.5">
                  <p class="text-sm text-fg">{{ item.productoNombre }}</p>
                  <p class="font-mono text-xs text-fg-muted">
                    {{ item.productoCodigo }} · {{ item.unidadMedida }}
                    @if (item.critico) { · critico }
                  </p>
                </td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ item.disponible | cantidad }}</td>
                <td class="hidden px-3 py-2.5 text-right tabular-nums text-fg-muted sm:table-cell">
                  {{ item.puntoReorden | cantidad }}
                </td>
                <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
                  {{ item.consumoPromedioDiario | cantidad: 2 }}
                </td>
                <td class="px-3 py-2.5 text-right">
                  @if (item.coberturaDias !== null && item.coberturaDias !== undefined) {
                    <span class="tabular-nums" [class.text-danger]="item.coberturaDias < 15">
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
              </ng-template>
            </ui-table>
          </div>
        </ui-card>
      }
    }
  `,
})
export class ReportesComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly servicio = inject(ReporteService);
  private readonly descarga = inject(DescargaService);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly vista = signal<Vista>('vencimientos');
  protected readonly dias = signal(90);
  protected readonly meses = signal(6);
  protected readonly cargando = signal(false);

  protected readonly vencimientos = signal<LoteVencimiento[]>([]);
  protected readonly valorizacion = signal<ValorizacionCategoria[]>([]);
  protected readonly consumo = signal<ConsumoMensual[]>([]);
  protected readonly rotacion = signal<RotacionProducto[]>([]);
  protected readonly inmovilizado = signal<BajaRotacion[]>([]);
  protected readonly reposicion = signal<StockConsolidado[]>([]);

  protected readonly pestanas = [
    { clave: 'vencimientos', titulo: 'Vencimientos' },
    { clave: 'valorizacion', titulo: 'Valorizacion' },
    { clave: 'consumo', titulo: 'Consumo por area' },
    { clave: 'rotacion', titulo: 'Rotacion' },
    { clave: 'inmovilizado', titulo: 'Inmovilizado' },
    { clave: 'reposicion', titulo: 'Reposicion' },
  ];

  protected readonly columnasVencimientos: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'lote', titulo: 'Lote' },
    { clave: 'vence', titulo: 'Vence' },
    { clave: 'almacen', titulo: 'Almacen', ocultarEn: 'tablet' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valor expuesto', alineacion: 'der' },
  ];

  protected readonly columnasValorizacion: ColumnaTabla[] = [
    { clave: 'categoria', titulo: 'Categoria' },
    { clave: 'productos', titulo: 'Productos', alineacion: 'der' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valorizado', alineacion: 'der' },
    { clave: 'participacion', titulo: 'Participacion', ancho: '12rem' },
  ];

  protected readonly columnasConsumo: ColumnaTabla[] = [
    { clave: 'periodo', titulo: 'Periodo' },
    { clave: 'area', titulo: 'Area' },
    { clave: 'centro', titulo: 'Centro de costo', ocultarEn: 'movil' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valor', alineacion: 'der' },
  ];

  protected readonly columnasRotacion: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'cantidad', titulo: 'Consumido', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valor consumido', alineacion: 'der' },
    { clave: 'ultima', titulo: 'Ultima salida', ocultarEn: 'movil' },
  ];

  protected readonly columnasInmovilizado: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'almacen', titulo: 'Almacen', ocultarEn: 'tablet' },
    { clave: 'cantidad', titulo: 'Cantidad', alineacion: 'der' },
    { clave: 'valor', titulo: 'Valorizado', alineacion: 'der' },
    { clave: 'dias', titulo: 'Sin movimiento', alineacion: 'der' },
  ];

  protected readonly columnasReposicion: ColumnaTabla[] = [
    { clave: 'producto', titulo: 'Producto' },
    { clave: 'disponible', titulo: 'Disponible', alineacion: 'der' },
    { clave: 'reorden', titulo: 'Punto de reorden', alineacion: 'der', ocultarEn: 'movil' },
    { clave: 'consumo', titulo: 'Consumo diario', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'cobertura', titulo: 'Cobertura', alineacion: 'der' },
    { clave: 'estado', titulo: 'Situacion', alineacion: 'centro' },
  ];

  protected readonly valorExpuesto = computed(() =>
    this.vencimientos().reduce((total, lote) => total + lote.valorExpuesto, 0));

  private readonly valorTotal = computed(() =>
    this.valorizacion().reduce((total, grupo) => total + grupo.valor, 0));

  /** Solo los reportes con exportacion CSV en el servidor ofrecen el boton. */
  protected readonly exportable = computed(() =>
    ['vencimientos', 'consumo'].includes(this.vista()));

  constructor() {
    effect(() => {
      // Depende del almacen, de la vista y de los parametros de periodo.
      const almacenId = this.contexto.seleccionadoId();
      const vista = this.vista();
      const dias = this.dias();
      const meses = this.meses();
      this.cargar(vista, almacenId, dias, meses);
    });
  }

  protected cambiarDias(valor: string): void {
    this.dias.set(Number(valor));
  }

  protected cambiarMeses(valor: string): void {
    this.meses.set(Number(valor));
  }

  protected periodo(linea: ConsumoMensual): string {
    return `${MESES[linea.mes] ?? linea.mes} ${linea.anio}`;
  }

  protected diasPara(fecha: string): number {
    return Math.floor((new Date(fecha).getTime() - Date.now()) / 86_400_000);
  }

  protected participacion(valor: number): number {
    const total = this.valorTotal();
    return total ? Math.round((valor / total) * 1000) / 10 : 0;
  }

  protected exportar(): void {
    const almacenId = this.contexto.seleccionadoId();
    const sufijo = new Date().toISOString().slice(0, 10);
    if (this.vista() === 'vencimientos') {
      this.descarga.descargarCsv('/reportes/vencimientos.csv',
        `vencimientos-${sufijo}.csv`, { almacenId, dias: this.dias() });
      return;
    }
    this.descarga.descargarCsv('/reportes/consumo.csv',
      `consumo-${sufijo}.csv`, { almacenId, meses: this.meses() });
  }

  private cargar(vista: Vista, almacenId: number | null, dias: number, meses: number): void {
    this.cargando.set(true);
    const fin = { complete: () => this.cargando.set(false) };
    switch (vista) {
      case 'vencimientos':
        this.servicio.vencimientos(almacenId, dias).subscribe({
          next: (datos) => this.vencimientos.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
      case 'valorizacion':
        this.servicio.valorizacion(almacenId).subscribe({
          next: (datos) => this.valorizacion.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
      case 'consumo':
        this.servicio.consumo(almacenId, meses).subscribe({
          next: (datos) => this.consumo.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
      case 'rotacion':
        this.servicio.rotacion(almacenId, dias).subscribe({
          next: (datos) => this.rotacion.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
      case 'inmovilizado':
        this.servicio.bajaRotacion(almacenId).subscribe({
          next: (datos) => this.inmovilizado.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
      case 'reposicion':
        this.servicio.reposicion(almacenId).subscribe({
          next: (datos) => this.reposicion.set(datos), ...fin,
          error: () => this.cargando.set(false),
        });
        break;
    }
  }
}
