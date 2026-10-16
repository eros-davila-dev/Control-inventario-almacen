import { ChangeDetectionStrategy, Component, ContentChild, TemplateRef, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { UiEmptyComponent } from './ui-empty.component';
import { UiSkeletonComponent } from './ui-skeleton.component';

export interface ColumnaTabla {
  clave: string;
  titulo: string;
  alineacion?: 'izq' | 'centro' | 'der';
  /** Campo por el que ordena el servidor; si falta, la columna no es ordenable. */
  ordenPor?: string;
  /** Oculta la columna en pantallas pequenas para priorizar lo esencial. */
  ocultarEn?: 'movil' | 'tablet';
  ancho?: string;
}

/**
 * Tabla reutilizable de la aplicacion: encabezados declarativos, ordenamiento
 * delegado al servidor, estado de carga y vacio, y vista de tarjetas en movil
 * (la plantilla de tarjeta es opcional; si no se define se usa la de fila).
 */
@Component({
  selector: 'ui-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet, UiEmptyComponent, UiSkeletonComponent],
  template: `
    @if (cargando()) {
      <ui-skeleton [filas]="6" />
    } @else if (!datos().length) {
      <ui-empty [titulo]="tituloVacio()" [descripcion]="descripcionVacio()" />
    } @else {
      <!-- Escritorio y tablet: tabla completa con scroll horizontal controlado -->
      <div class="sgai-scroll -mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0"
           [class.hidden]="!!plantillaTarjeta">
        <table class="w-full border-collapse text-sm">
          <thead>
            <tr class="border-b border-border text-left">
              @for (columna of columnas(); track columna.clave) {
                <th scope="col"
                    [style.width]="columna.ancho"
                    [class]="clasesEncabezado(columna)">
                  @if (columna.ordenPor) {
                    <button type="button"
                            class="inline-flex items-center gap-1 transition-colors hover:text-fg"
                            (click)="cambiarOrden(columna.ordenPor)">
                      {{ columna.titulo }}
                      <span class="text-[10px]" aria-hidden="true">{{ iconoOrden(columna.ordenPor) }}</span>
                    </button>
                  } @else {
                    {{ columna.titulo }}
                  }
                </th>
              }
            </tr>
          </thead>
          <tbody>
            @for (fila of datos(); track indiceDe($index, fila)) {
              <tr class="border-b border-border/60 transition-colors last:border-0
                         hover:bg-surface-muted/60">
                <ng-container [ngTemplateOutlet]="plantillaFila"
                              [ngTemplateOutletContext]="{ $implicit: fila, indice: $index }" />
              </tr>
            }
          </tbody>
        </table>
      </div>

      @if (plantillaTarjeta) {
        <!-- Movil: cada registro se presenta como tarjeta apilada -->
        <div class="grid gap-2 lg:hidden">
          @for (fila of datos(); track indiceDe($index, fila)) {
            <ng-container [ngTemplateOutlet]="plantillaTarjeta"
                          [ngTemplateOutletContext]="{ $implicit: fila, indice: $index }" />
          }
        </div>
        <div class="sgai-scroll hidden overflow-x-auto lg:block">
          <table class="w-full border-collapse text-sm">
            <thead>
              <tr class="border-b border-border text-left">
                @for (columna of columnas(); track columna.clave) {
                  <th scope="col" [style.width]="columna.ancho" [class]="clasesEncabezado(columna)">
                    @if (columna.ordenPor) {
                      <button type="button"
                              class="inline-flex items-center gap-1 transition-colors hover:text-fg"
                              (click)="cambiarOrden(columna.ordenPor)">
                        {{ columna.titulo }}
                        <span class="text-[10px]" aria-hidden="true">{{ iconoOrden(columna.ordenPor) }}</span>
                      </button>
                    } @else {
                      {{ columna.titulo }}
                    }
                  </th>
                }
              </tr>
            </thead>
            <tbody>
              @for (fila of datos(); track indiceDe($index, fila)) {
                <tr class="border-b border-border/60 transition-colors last:border-0
                           hover:bg-surface-muted/60">
                  <ng-container [ngTemplateOutlet]="plantillaFila"
                                [ngTemplateOutletContext]="{ $implicit: fila, indice: $index }" />
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    }
  `,
  host: { class: 'block' },
})
export class UiTableComponent<T> {
  readonly columnas = input.required<ColumnaTabla[]>();
  readonly datos = input.required<readonly T[]>();
  readonly cargando = input(false);
  readonly tituloVacio = input('Sin resultados');
  readonly descripcionVacio = input('Ajuste los filtros o registre informacion para verla aqui.');
  /** Orden actual en formato Spring Data: "campo,asc". */
  readonly orden = input('');
  readonly ordenCambiado = output<string>();

  @ContentChild('fila', { static: true }) plantillaFila!: TemplateRef<unknown>;
  @ContentChild('tarjeta', { static: true }) plantillaTarjeta?: TemplateRef<unknown>;

  protected indiceDe(indice: number, fila: T): unknown {
    return (fila as { id?: unknown })?.id ?? indice;
  }

  protected clasesEncabezado(columna: ColumnaTabla): string {
    return [
      'px-3 py-2.5 text-xs font-semibold uppercase tracking-wide text-fg-muted whitespace-nowrap',
      columna.alineacion === 'der' ? 'text-right' : '',
      columna.alineacion === 'centro' ? 'text-center' : '',
      columna.ocultarEn === 'movil' ? 'hidden sm:table-cell' : '',
      columna.ocultarEn === 'tablet' ? 'hidden lg:table-cell' : '',
    ].join(' ');
  }

  protected cambiarOrden(campo: string): void {
    const [campoActual, direccion] = this.orden().split(',');
    const nueva = campoActual === campo && direccion === 'asc' ? 'desc' : 'asc';
    this.ordenCambiado.emit(`${campo},${nueva}`);
  }

  protected iconoOrden(campo: string): string {
    const [campoActual, direccion] = this.orden().split(',');
    if (campoActual !== campo) {
      return '↕';
    }
    return direccion === 'asc' ? '↑' : '↓';
  }
}
