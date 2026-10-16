import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AlcanceToma, TomaInventario, TomaInventarioRequest } from '../../core/models/conteo.model';
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
import { TomaInventarioService } from './conteo.service';

/**
 * Programacion y seguimiento de tomas de inventario (HU-36 a HU-39). El ERI
 * (exactitud del registro de inventario) de cada toma es el indicador que mide
 * el objetivo OE1: pasar del 72 % al 98 % de exactitud.
 */
@Component({
  selector: 'sgai-tomas-inventario',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    CantidadPipe, MonedaPipe, FechaPipe, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Inventario fisico"
                    descripcion="Conteo ciego por almacen, categoria o clase ABC; el sistema calcula el ERI y genera los ajustes al aprobar.">
      <div acciones>
        <ui-button *hasPermiso="'conteo:toma:programar'" (presionado)="abrirProgramacion()">
          Programar toma
        </ui-button>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="border-b border-border px-4 py-3">
        <select class="sgai-input max-w-xs"
                (change)="lista.filtrar({ estado: $any($event.target).value || null })">
          <option value="">Todos los estados</option>
          <option value="PROGRAMADA">Programada</option>
          <option value="EN_PROCESO">En proceso</option>
          <option value="CONTADA">Contada (por aprobar)</option>
          <option value="APROBADA">Aprobada</option>
          <option value="ANULADA">Anulada</option>
        </select>
      </div>

      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)"
                  tituloVacio="Sin tomas de inventario"
                  descripcionVacio="Programe una toma ciclica ABC o un conteo total del almacen.">
          <ng-template #fila let-toma>
            <td class="px-3 py-2.5">
              <p class="font-mono text-sm font-medium text-fg">{{ toma.numero }}</p>
              <p class="text-xs text-fg-muted">
                programada {{ toma.fechaProgramada | fechaSgai }}
              </p>
            </td>
            <td class="px-3 py-2.5">
              <p class="text-sm text-fg">{{ toma.alcanceDescripcion }}</p>
              <p class="text-xs text-fg-muted">{{ toma.almacenNombre }}</p>
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">
              <p class="text-sm text-fg">{{ toma.itemsContados }} / {{ toma.itemsTotales }}</p>
              <div class="ml-auto mt-1 h-1.5 w-20 overflow-hidden rounded-full bg-surface-muted">
                <div class="h-full rounded-full bg-brand-500"
                     [style.width.%]="avance(toma)"></div>
              </div>
            </td>
            <td class="hidden px-3 py-2.5 text-right tabular-nums lg:table-cell">
              <span [class.text-danger]="toma.itemsConDiferencia > 0">
                {{ toma.itemsConDiferencia }}
              </span>
              @if (toma.valorDiferencias) {
                <p class="text-[11px] text-fg-subtle">{{ toma.valorDiferencias | moneda }}</p>
              }
            </td>
            <td class="px-3 py-2.5 text-center">
              @if (toma.eri !== null && toma.eri !== undefined) {
                <span class="text-sm font-semibold tabular-nums"
                      [class.text-success]="toma.eri >= 98"
                      [class.text-warning]="toma.eri >= 90 && toma.eri < 98"
                      [class.text-danger]="toma.eri < 90">
                  {{ toma.eri | cantidad: 2 }} %
                </span>
              } @else {
                <span class="text-fg-subtle">—</span>
              }
            </td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="tono(toma.estado)">{{ etiqueta(toma.estado) }}</ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right whitespace-nowrap">
              <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-brand-700
                             hover:bg-surface-muted dark:text-brand-300"
                      (click)="abrirToma(toma)">
                {{ toma.estado === 'EN_PROCESO' ? 'Contar' : 'Ver' }}
              </button>
              @if (toma.estado === 'PROGRAMADA') {
                <span *hasPermiso="'conteo:toma:ejecutar'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                 text-success hover:bg-success-soft"
                          (click)="iniciar(toma)">Iniciar</button>
                </span>
              }
              @if (toma.estado !== 'APROBADA' && toma.estado !== 'ANULADA') {
                <span *hasPermiso="'conteo:toma:programar'">
                  <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium
                                 text-danger hover:bg-danger-soft"
                          (click)="anular(toma)">Anular</button>
                </span>
              }
            </td>
          </ng-template>

          <ng-template #tarjeta let-toma>
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-mono text-sm font-medium text-fg">{{ toma.numero }}</p>
                <p class="text-xs text-fg-muted">{{ toma.alcanceDescripcion }}</p>
              </div>
              <ui-badge [tono]="tono(toma.estado)">{{ etiqueta(toma.estado) }}</ui-badge>
            </div>
            <p class="mt-2 text-xs text-fg-muted">
              Avance {{ toma.itemsContados }} / {{ toma.itemsTotales }}
              @if (toma.eri !== null && toma.eri !== undefined) {
                · ERI {{ toma.eri | cantidad: 2 }} %
              }
            </p>
            <div class="mt-2 flex gap-2">
              <ui-button tamano="sm" variante="secundario" (presionado)="abrirToma(toma)">
                {{ toma.estado === 'EN_PROCESO' ? 'Contar' : 'Ver' }}
              </ui-button>
              @if (toma.estado === 'PROGRAMADA') {
                <span *hasPermiso="'conteo:toma:ejecutar'">
                  <ui-button tamano="sm" (presionado)="iniciar(toma)">Iniciar</ui-button>
                </span>
              }
            </div>
          </ng-template>
        </ui-table>

        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <ui-modal [abierto]="modalAbierto()" titulo="Programar toma de inventario"
              subtitulo="Al iniciarla, el sistema congela el saldo teorico de cada ubicacion y lote."
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="space-y-3">
        <label class="block">
          <span class="sgai-label">Almacen <span class="text-danger">*</span></span>
          <select class="sgai-input" formControlName="almacenId">
            @for (almacen of contexto.almacenes(); track almacen.id) {
              <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
            }
          </select>
        </label>
        <label class="block">
          <span class="sgai-label">Alcance</span>
          <select class="sgai-input" formControlName="alcance">
            <option value="TOTAL">Total · todo el almacen</option>
            <option value="POR_CATEGORIA">Por categoria</option>
            <option value="CICLICO_ABC">Ciclico por clase ABC</option>
          </select>
        </label>
        @if (alcanceActual() === 'POR_CATEGORIA') {
          <label class="block">
            <span class="sgai-label">Categoria <span class="text-danger">*</span></span>
            <select class="sgai-input" formControlName="categoriaId">
              <option [value]="null">Seleccione...</option>
              @for (categoria of catalogos.categorias(); track categoria.id) {
                <option [value]="categoria.id">{{ categoria.nombre }}</option>
              }
            </select>
          </label>
        }
        @if (alcanceActual() === 'CICLICO_ABC') {
          <label class="block">
            <span class="sgai-label">Clase <span class="text-danger">*</span></span>
            <select class="sgai-input" formControlName="clasificacionAbc">
              <option [value]="null">Seleccione...</option>
              <option value="A">A · alto valor de consumo (conteo mensual)</option>
              <option value="B">B · valor intermedio (trimestral)</option>
              <option value="C">C · bajo valor (semestral)</option>
            </select>
          </label>
        }
        <label class="block">
          <span class="sgai-label">Fecha programada</span>
          <input class="sgai-input" type="date" formControlName="fechaProgramada" />
        </label>
        <label class="block">
          <span class="sgai-label">Observacion</span>
          <input class="sgai-input" formControlName="observacion"
                 placeholder="Conteo ciclico de clase A - octubre" maxlength="255" />
        </label>
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="programar()">Programar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class TomasComponent {
  protected readonly contexto = inject(AlmacenContextoService);
  protected readonly catalogos = inject(CatalogoRefService);
  private readonly servicio = inject(TomaInventarioService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly modalAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly alcanceActual = signal<AlcanceToma>('TOTAL');

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'numero', titulo: 'Toma', ordenPor: 'numero' },
    { clave: 'alcance', titulo: 'Alcance' },
    { clave: 'avance', titulo: 'Avance', alineacion: 'der' },
    { clave: 'diferencias', titulo: 'Diferencias', alineacion: 'der', ocultarEn: 'tablet' },
    { clave: 'eri', titulo: 'ERI', alineacion: 'centro' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<TomaInventario>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'fechaProgramada,desc' });

  protected readonly formulario = this.fb.group({
    almacenId: [null as number | null],
    alcance: ['TOTAL' as AlcanceToma],
    categoriaId: [null as number | null],
    clasificacionAbc: [null as string | null],
    fechaProgramada: [new Date().toISOString().slice(0, 10)],
    observacion: [''],
  });

  constructor() {
    this.catalogos.categoriasActivas().subscribe();
    effect(() => this.lista.filtrar({ almacenId: this.contexto.seleccionadoId() }));
    this.formulario.controls.alcance.valueChanges.subscribe(
      (alcance) => this.alcanceActual.set(alcance ?? 'TOTAL'));
  }

  protected avance(toma: TomaInventario): number {
    return toma.itemsTotales ? Math.round((toma.itemsContados / toma.itemsTotales) * 100) : 0;
  }

  protected abrirProgramacion(): void {
    this.formulario.reset({
      almacenId: this.contexto.seleccionadoId(),
      alcance: 'TOTAL',
      categoriaId: null,
      clasificacionAbc: null,
      fechaProgramada: new Date().toISOString().slice(0, 10),
      observacion: '',
    });
    this.alcanceActual.set('TOTAL');
    this.modalAbierto.set(true);
  }

  protected programar(): void {
    const valores = this.formulario.getRawValue();
    if (!valores.almacenId) {
      this.notificacion.advertencia('Seleccione el almacen a contar');
      return;
    }
    const alcance = valores.alcance ?? 'TOTAL';
    if (alcance === 'POR_CATEGORIA' && !valores.categoriaId) {
      this.notificacion.advertencia('Indique la categoria del conteo');
      return;
    }
    if (alcance === 'CICLICO_ABC' && !valores.clasificacionAbc) {
      this.notificacion.advertencia('Indique la clase ABC del conteo');
      return;
    }
    const cuerpo: TomaInventarioRequest = {
      almacenId: Number(valores.almacenId),
      alcance,
      categoriaId: alcance === 'POR_CATEGORIA' ? Number(valores.categoriaId) : null,
      clasificacionAbc: alcance === 'CICLICO_ABC'
        ? (valores.clasificacionAbc as 'A' | 'B' | 'C') : null,
      fechaProgramada: valores.fechaProgramada || null,
      observacion: valores.observacion || null,
    };
    this.guardando.set(true);
    this.servicio.programar(cuerpo).subscribe({
      next: (toma) => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.notificacion.exito('Toma programada', toma.numero);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected async iniciar(toma: TomaInventario): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Iniciar ${toma.numero}`,
      mensaje: 'Se congelara el saldo teorico de cada producto, lote y ubicacion. Evite registrar '
        + 'movimientos en el almacen mientras dure el conteo.',
      textoConfirmar: 'Iniciar conteo',
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.abrir(toma.id).subscribe((abierta) => {
      this.notificacion.exito('Toma iniciada', `${abierta.itemsTotales} items por contar`);
      this.router.navigate(['/conteo/tomas', abierta.id]);
    });
  }

  protected async anular(toma: TomaInventario): Promise<void> {
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Anular ${toma.numero}`,
      mensaje: 'La toma quedara sin efecto y no generara ajustes. Indique el motivo.',
      peligroso: true,
      pedirMotivo: true,
      textoConfirmar: 'Anular',
    });
    if (!respuesta.confirmado || !respuesta.motivo) {
      return;
    }
    this.servicio.anular(toma.id, respuesta.motivo).subscribe(() => {
      this.notificacion.exito('Toma anulada', toma.numero);
      this.lista.recargar();
    });
  }

  protected abrirToma(toma: TomaInventario): void {
    this.router.navigate(['/conteo/tomas', toma.id]);
  }
}
