import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { ProductoResumen } from '../../core/models/catalogo.model';
import { CatalogoRefService } from '../services/catalogo-ref.service';

/**
 * Buscador de productos reutilizado por ingresos, salidas, requisiciones,
 * ordenes de compra y conteos: evita que cada pantalla implemente su propio
 * autocompletado y garantiza que siempre se vea el codigo, la unidad y si el
 * producto exige lote.
 */
@Component({
  selector: 'sgai-selector-producto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <label class="block">
        @if (etiqueta()) {
          <span class="sgai-label">{{ etiqueta() }}</span>
        }
        <input type="search" class="sgai-input" autocomplete="off"
               [value]="texto()"
               [placeholder]="marcador()"
               (input)="buscar($any($event.target).value)"
               (focus)="abierto.set(true)"
               (blur)="cerrarDiferido()" />
      </label>

      @if (abierto() && coincidencias().length) {
        <ul class="sgai-scroll absolute z-30 mt-1 max-h-72 w-full overflow-y-auto rounded-xl
                   border border-border bg-surface py-1 shadow-[var(--shadow-pop)]"
            role="listbox">
          @for (producto of coincidencias(); track producto.id) {
            <li>
              <button type="button" role="option" [attr.aria-selected]="false"
                      class="flex w-full items-start gap-2 px-3 py-2 text-left transition-colors
                             hover:bg-surface-muted"
                      (mousedown)="seleccionar(producto)">
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-medium text-fg">{{ producto.nombre }}</span>
                  <span class="block text-xs text-fg-muted">
                    {{ producto.codigo }} · {{ producto.unidadMedida }}
                    @if (producto.controlaLote) { · exige lote }
                    @if (producto.critico) { · critico }
                  </span>
                </span>
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
  host: { class: 'block' },
})
export class SelectorProductoComponent {
  readonly etiqueta = input('Producto');
  readonly marcador = input('Buscar por codigo, nombre o marca...');
  /** Identificadores que ya estan en el documento y no deben repetirse. */
  readonly excluidos = input<readonly number[]>([]);
  readonly seleccionado = output<ProductoResumen>();

  private readonly catalogo = inject(CatalogoRefService);
  protected readonly texto = signal('');
  protected readonly abierto = signal(false);

  protected readonly coincidencias = computed(() => {
    const termino = this.texto().trim().toLowerCase();
    const excluidos = new Set(this.excluidos());
    return this.catalogo.productos()
      .filter((p) => !excluidos.has(p.id))
      .filter((p) => !termino
        || p.nombre.toLowerCase().includes(termino)
        || p.codigo.toLowerCase().includes(termino))
      .slice(0, 12);
  });

  constructor() {
    this.catalogo.productosResumen().subscribe();
  }

  protected buscar(valor: string): void {
    this.texto.set(valor);
    this.abierto.set(true);
  }

  protected seleccionar(producto: ProductoResumen): void {
    this.seleccionado.emit(producto);
    this.texto.set('');
    this.abierto.set(false);
  }

  /** Se cierra con retardo para no perder el clic en la lista. */
  protected cerrarDiferido(): void {
    setTimeout(() => this.abierto.set(false), 150);
  }
}
