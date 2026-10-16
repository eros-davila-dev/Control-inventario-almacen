import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConteoRequest, LineaToma, TomaInventario } from '../../core/models/conteo.model';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { CantidadPipe } from '../../shared/pipes/cantidad.pipe';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { MonedaPipe } from '../../shared/pipes/moneda.pipe';
import {
  UiButtonComponent,
  UiCardComponent,
  UiPageHeaderComponent,
  UiSkeletonComponent,
  UiStatComponent,
  etiquetaEstado,
  tonoDeEstado,
} from '../../shared/ui';
import { TomaInventarioService } from './conteo.service';

/**
 * Hoja de conteo fisico (HU-37, HU-38, HU-39). Mientras la toma esta EN_PROCESO
 * el saldo teorico no se muestra (conteo ciego); al cerrar el conteo aparecen
 * las diferencias, el ERI y, con la aprobacion, se generan los ajustes de kardex.
 */
@Component({
  selector: 'sgai-conteo-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiButtonComponent,
    UiStatComponent, UiSkeletonComponent, CantidadPipe, MonedaPipe, FechaPipe,
    HasPermisoDirective,
  ],
  template: `
    @let toma = datos();
    @if (!toma) {
      <ui-skeleton [filas]="6" />
    } @else {
      <ui-page-header [titulo]="'Toma ' + toma.numero"
                      [descripcion]="toma.alcanceDescripcion + ' · ' + toma.almacenNombre">
        <div acciones class="flex flex-wrap gap-2">
          <ui-button variante="secundario" (presionado)="volver()">Volver</ui-button>
          @if (toma.estado === 'EN_PROCESO') {
            <span *hasPermiso="'conteo:toma:ejecutar'" class="flex gap-2">
              <ui-button variante="secundario" [cargando]="guardando()"
                         [deshabilitado]="!cambios()" (presionado)="guardarConteo(false)">
                Guardar avance
              </ui-button>
              <ui-button [cargando]="cerrando()" (presionado)="cerrarConteo()">
                Cerrar conteo
              </ui-button>
            </span>
          }
          @if (toma.estado === 'CONTADA') {
            <span *hasPermiso="'conteo:toma:aprobar'">
              <ui-button (presionado)="aprobar()">Aprobar y ajustar</ui-button>
            </span>
          }
        </div>
      </ui-page-header>

      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <ui-stat etiqueta="Estado" [valor]="toma.estadoDescripcion"
                 [detalle]="'Programada ' + (toma.fechaProgramada | fechaSgai)"
                 [tono]="tono(toma.estado)" />
        <ui-stat etiqueta="Avance del conteo"
                 [valor]="toma.itemsContados + ' / ' + toma.itemsTotales"
                 [detalle]="avance() + ' % de los items'" tono="marca" />
        <ui-stat etiqueta="Items con diferencia" [valor]="toma.itemsConDiferencia"
                 [detalle]="cegado() ? 'visible al cerrar el conteo'
                   : ((toma.valorDiferencias | moneda) + ' de impacto')"
                 [tono]="toma.itemsConDiferencia > 0 ? 'advertencia' : 'exito'" />
        <ui-stat etiqueta="ERI"
                 [valor]="toma.eri !== null && toma.eri !== undefined
                   ? (toma.eri | cantidad: 2) + ' %' : '—'"
                 detalle="Meta institucional: 98 %"
                 [tono]="toma.eri === null || toma.eri === undefined ? 'neutral'
                   : (toma.eri >= 98 ? 'exito' : 'advertencia')" />
      </div>

      @if (cegado()) {
        <div class="mt-4 rounded-xl border border-info/30 bg-info-soft px-4 py-3 text-sm text-info">
          <strong class="font-semibold">Conteo ciego.</strong>
          El saldo del sistema permanece oculto hasta cerrar el conteo: registre lo que
          fisicamente cuenta en cada ubicacion, sin ajustarlo a ninguna referencia.
        </div>
      }

      <ui-card class="mt-4" [sinRelleno]="true"
               titulo="Hoja de conteo"
               [subtitulo]="cegado()
                 ? 'Escriba la cantidad contada; deje en blanco lo que aun no cuenta.'
                 : 'Resultado del conteo frente al saldo congelado al abrir la toma.'">
        <div class="border-b border-border px-4 py-3">
          <input class="sgai-input max-w-sm" type="search"
                 placeholder="Filtrar por producto, lote o ubicacion"
                 (input)="filtro.set($any($event.target).value)" />
        </div>

        <form [formGroup]="formulario">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[46rem] text-sm">
              <thead class="bg-surface-muted/60">
                <tr class="text-left text-xs uppercase text-fg-muted">
                  <th class="px-3 py-2 font-semibold">Producto</th>
                  <th class="px-3 py-2 font-semibold">Lote / vence</th>
                  <th class="px-3 py-2 font-semibold">Ubicacion</th>
                  @if (!cegado()) {
                    <th class="px-3 py-2 text-right font-semibold">Sistema</th>
                  }
                  <th class="px-3 py-2 text-right font-semibold">1.er conteo</th>
                  <th class="px-3 py-2 text-right font-semibold">2.o conteo</th>
                  @if (!cegado()) {
                    <th class="px-3 py-2 text-right font-semibold">Diferencia</th>
                  }
                  <th class="px-3 py-2 font-semibold">Observacion</th>
                </tr>
              </thead>
              <tbody>
                @for (indice of indicesVisibles(); track indice) {
                  @let grupo = asGrupo(lineas.at(indice));
                  <tr class="border-b border-border/60 last:border-0" [formGroup]="grupo">
                    <td class="px-3 py-2">
                      <p class="text-fg">{{ grupo.value.nombre }}</p>
                      <p class="font-mono text-xs text-fg-muted">
                        {{ grupo.value.codigo }} · {{ grupo.value.unidad }}
                      </p>
                    </td>
                    <td class="px-3 py-2 text-xs">
                      <p class="font-mono text-fg">{{ grupo.value.codigoLote || '—' }}</p>
                      <p class="text-fg-muted">
                        {{ grupo.value.fechaVencimiento
                           ? (grupo.value.fechaVencimiento | fechaSgai) : '' }}
                      </p>
                    </td>
                    <td class="px-3 py-2 font-mono text-xs text-fg-muted">
                      {{ grupo.value.ubicacionCodigo || '—' }}
                    </td>
                    @if (!cegado()) {
                      <td class="px-3 py-2 text-right tabular-nums text-fg-muted">
                        {{ grupo.value.teorica | cantidad }}
                      </td>
                    }
                    <td class="px-3 py-2">
                      @if (editable()) {
                        <input class="sgai-input w-24 py-1.5 text-right text-xs tabular-nums"
                               type="number" step="0.0001" min="0" inputmode="decimal"
                               formControlName="cantidadContada" (input)="marcarCambio()" />
                      } @else {
                        <p class="text-right tabular-nums text-fg">
                          {{ grupo.value.cantidadContada | cantidad }}
                        </p>
                      }
                    </td>
                    <td class="px-3 py-2">
                      @if (editable()) {
                        <input class="sgai-input w-24 py-1.5 text-right text-xs tabular-nums"
                               type="number" step="0.0001" min="0" inputmode="decimal"
                               formControlName="segundoConteo" (input)="marcarCambio()"
                               title="Recuento de verificacion cuando el primero no cuadra" />
                      } @else {
                        <p class="text-right tabular-nums text-fg-muted">
                          {{ grupo.value.segundoConteo !== null
                             && grupo.value.segundoConteo !== undefined
                             ? (grupo.value.segundoConteo | cantidad) : '—' }}
                        </p>
                      }
                    </td>
                    @if (!cegado()) {
                      <td class="px-3 py-2 text-right tabular-nums"
                          [class.text-danger]="grupo.value.diferencia < 0"
                          [class.text-success]="grupo.value.diferencia > 0">
                        {{ grupo.value.diferencia | cantidad }}
                      </td>
                    }
                    <td class="px-3 py-2">
                      @if (editable()) {
                        <input class="sgai-input py-1.5 text-xs" formControlName="observacion"
                               placeholder="Motivo de la diferencia" maxlength="300" />
                      } @else {
                        <span class="text-xs text-fg-muted">{{ grupo.value.observacion || '' }}</span>
                      }
                    </td>
                  </tr>
                }
                @if (!indicesVisibles().length) {
                  <tr>
                    <td [attr.colspan]="cegado() ? 6 : 8"
                        class="px-3 py-8 text-center text-sm text-fg-muted">
                      Ningun item coincide con el filtro.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </form>
      </ui-card>

      @if (toma.estado === 'APROBADA') {
        <p class="mt-3 text-sm text-fg-muted">
          Aprobada por {{ toma.aprobador || '—' }}
          @if (toma.fechaCierre) { el {{ toma.fechaCierre | fechaSgai }} }
          · los ajustes quedaron registrados en el kardex con motivo "conteo fisico".
        </p>
      }
      @if (toma.observacion) {
        <p class="mt-2 rounded-lg bg-surface-muted px-3 py-2 text-sm text-fg-muted">
          {{ toma.observacion }}
        </p>
      }
    }
  `,
})
export class ConteoDetalleComponent {
  /** Identificador de la toma, enlazado desde la ruta. */
  readonly id = input.required<string>();

  private readonly servicio = inject(TomaInventarioService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly tono = tonoDeEstado;
  protected readonly etiqueta = etiquetaEstado;

  protected readonly datos = signal<TomaInventario | null>(null);
  protected readonly guardando = signal(false);
  protected readonly cerrando = signal(false);
  protected readonly cambios = signal(false);
  protected readonly filtro = signal('');

  protected readonly formulario = this.fb.group({ lineas: this.fb.array<FormGroup>([]) });

  protected get lineas(): FormArray<FormGroup> {
    return this.formulario.controls.lineas;
  }

  protected readonly editable = computed(() => this.datos()?.estado === 'EN_PROCESO');
  /** En conteo ciego el servidor no envia la cantidad teorica. */
  protected readonly cegado = computed(() => this.datos()?.estado === 'EN_PROCESO');

  protected readonly avance = computed(() => {
    const toma = this.datos();
    return toma?.itemsTotales ? Math.round((toma.itemsContados / toma.itemsTotales) * 100) : 0;
  });

  /** Filtra sin rehacer el FormArray, para no perder lo ya tipeado. */
  protected readonly indicesVisibles = computed<number[]>(() => {
    const texto = this.filtro().trim().toLowerCase();
    const lineas = this.datos()?.lineas ?? [];
    return lineas
      .map((linea, indice) => ({ linea, indice }))
      .filter(({ linea }) => !texto
        || `${linea.productoCodigo} ${linea.productoNombre} ${linea.codigoLote ?? ''} `
          .concat(linea.ubicacionCodigo ?? '').toLowerCase().includes(texto))
      .map(({ indice }) => indice);
  });

  constructor() {
    this.cargar();
  }

  protected asGrupo(control: unknown): FormGroup {
    return control as FormGroup;
  }

  protected marcarCambio(): void {
    this.cambios.set(true);
  }

  protected volver(): void {
    this.router.navigate(['/conteo/tomas']);
  }

  private cargar(): void {
    this.servicio.obtener(Number(this.id())).subscribe((toma) => this.aplicar(toma));
  }

  private aplicar(toma: TomaInventario): void {
    this.datos.set(toma);
    this.lineas.clear();
    toma.lineas.forEach((linea) => this.lineas.push(this.grupoDeLinea(linea)));
    this.cambios.set(false);
  }

  private grupoDeLinea(linea: LineaToma): FormGroup {
    return this.fb.group({
      lineaId: [linea.id],
      codigo: [linea.productoCodigo],
      nombre: [linea.productoNombre],
      unidad: [linea.unidadMedida],
      codigoLote: [linea.codigoLote ?? ''],
      fechaVencimiento: [linea.fechaVencimiento ?? ''],
      ubicacionCodigo: [linea.ubicacionCodigo ?? ''],
      teorica: [linea.cantidadTeorica ?? null],
      diferencia: [linea.diferencia ?? 0],
      cantidadContada: [linea.cantidadContada ?? null],
      segundoConteo: [linea.segundoConteo ?? null],
      observacion: [linea.observacion ?? ''],
    });
  }

  /** Envia solo lo contado; lo que queda en blanco sigue pendiente. */
  protected guardarConteo(silencioso: boolean): void {
    const peticion = this.lineasParaEnviar();
    if (!peticion.lineas.length) {
      this.notificacion.advertencia('Registre al menos una cantidad contada');
      return;
    }
    this.guardando.set(true);
    this.servicio.contar(Number(this.id()), peticion).subscribe({
      next: (toma) => {
        this.guardando.set(false);
        this.aplicar(toma);
        if (!silencioso) {
          this.notificacion.exito('Conteo guardado',
            `${toma.itemsContados} de ${toma.itemsTotales} items contados`);
        }
      },
      error: () => this.guardando.set(false),
    });
  }

  /**
   * Arma el cuerpo del conteo con lo que el operario escribio. El segundo conteo
   * se envia como una linea aparte para que el servidor lo tome como recuento de
   * verificacion y no como una correccion del primero.
   */
  private lineasParaEnviar(): ConteoRequest {
    const lineas: ConteoRequest['lineas'] = [];
    this.lineas.controls.forEach((grupo) => {
      const valores = grupo.getRawValue();
      const agregar = (cantidad: unknown, esSegundoConteo: boolean) => {
        if (cantidad === null || cantidad === undefined || cantidad === '') {
          return;
        }
        lineas.push({
          lineaId: Number(valores.lineaId),
          cantidadContada: Number(cantidad),
          esSegundoConteo,
          observacion: valores.observacion || null,
        });
      };
      agregar(valores.cantidadContada, false);
      agregar(valores.segundoConteo, true);
    });
    return { lineas };
  }

  protected async cerrarConteo(): Promise<void> {
    const toma = this.datos();
    if (!toma) {
      return;
    }
    const pendientes = toma.itemsTotales - toma.itemsContados;
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Cerrar el conteo de ${toma.numero}`,
      mensaje: pendientes > 0
        ? `Quedan ${pendientes} items sin contar; se tomaran como cero y generaran diferencia. `
          + 'Al cerrar se revelan el saldo del sistema y el ERI.'
        : 'Al cerrar se revelan el saldo del sistema, las diferencias y el ERI. '
          + 'Luego solo queda aprobar para que se generen los ajustes.',
      textoConfirmar: 'Cerrar conteo',
      peligroso: pendientes > 0,
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.cerrando.set(true);
    const cerrar = () => this.servicio.cerrarConteo(Number(this.id())).subscribe({
      next: (cerrada) => {
        this.cerrando.set(false);
        this.aplicar(cerrada);
        this.notificacion.exito('Conteo cerrado',
          `ERI ${cerrada.eri ?? 0} % · ${cerrada.itemsConDiferencia} items con diferencia`);
      },
      error: () => this.cerrando.set(false),
    });
    if (this.cambios()) {
      // Persiste lo tipeado antes de cerrar para no perder el ultimo tramo contado.
      this.servicio.contar(Number(this.id()), this.lineasParaEnviar()).subscribe({
        next: () => cerrar(),
        error: () => this.cerrando.set(false),
      });
    } else {
      cerrar();
    }
  }

  protected async aprobar(): Promise<void> {
    const toma = this.datos();
    if (!toma) {
      return;
    }
    const respuesta = await this.confirmacion.preguntar({
      titulo: `Aprobar ${toma.numero}`,
      mensaje: `Se generaran los ajustes de inventario por ${toma.itemsConDiferencia} items `
        + 'con diferencia. Los movimientos de ajuste no se pueden editar despues. '
        + 'Quien ejecuto el conteo no puede aprobarlo.',
      textoConfirmar: 'Aprobar y generar ajustes',
    });
    if (!respuesta.confirmado) {
      return;
    }
    this.servicio.aprobar(toma.id).subscribe((aprobada) => {
      this.aplicar(aprobada);
      this.notificacion.exito('Toma aprobada', 'Los ajustes quedaron registrados en el kardex');
    });
  }
}
