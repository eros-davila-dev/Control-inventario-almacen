import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/** Paginacion de tablas: siempre del lado del servidor (RNF-02). */
@Component({
  selector: 'ui-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalElementos() > 0) {
      <nav class="flex flex-wrap items-center justify-between gap-3 border-t border-border px-1 pt-3"
           aria-label="Paginacion">
        <p class="text-xs text-fg-muted">
          Mostrando {{ desde() }}-{{ hasta() }} de {{ totalElementos() }} registro(s)
        </p>
        <div class="flex items-center gap-1">
          <button type="button" [class]="clasesBoton()" [disabled]="pagina() === 0"
                  (click)="irA(pagina() - 1)" aria-label="Pagina anterior">Anterior</button>
          <span class="px-2 text-xs font-medium text-fg">
            {{ pagina() + 1 }} / {{ totalPaginas() || 1 }}
          </span>
          <button type="button" [class]="clasesBoton()"
                  [disabled]="pagina() + 1 >= totalPaginas()"
                  (click)="irA(pagina() + 1)" aria-label="Pagina siguiente">Siguiente</button>
          <select class="ml-2 rounded-lg border border-border bg-surface px-2 py-1 text-xs text-fg"
                  [value]="tamano()" (change)="cambiarTamano($event)"
                  aria-label="Registros por pagina">
            @for (opcion of tamanos; track opcion) {
              <option [value]="opcion">{{ opcion }} / pagina</option>
            }
          </select>
        </div>
      </nav>
    }
  `,
  host: { class: 'block' },
})
export class UiPaginationComponent {
  readonly pagina = input(0);
  readonly tamano = input(20);
  readonly totalElementos = input(0);
  readonly totalPaginas = input(0);
  readonly paginaCambiada = output<number>();
  readonly tamanoCambiado = output<number>();

  protected readonly tamanos = [10, 20, 50, 100];

  protected readonly desde = computed(() =>
    this.totalElementos() === 0 ? 0 : this.pagina() * this.tamano() + 1);
  protected readonly hasta = computed(() =>
    Math.min((this.pagina() + 1) * this.tamano(), this.totalElementos()));

  protected clasesBoton(): string {
    return 'rounded-lg border border-border bg-surface px-2.5 py-1 text-xs font-medium text-fg '
      + 'transition-colors hover:bg-surface-muted disabled:opacity-40 disabled:hover:bg-surface';
  }

  protected irA(pagina: number): void {
    this.paginaCambiada.emit(Math.max(0, pagina));
  }

  protected cambiarTamano(evento: Event): void {
    this.tamanoCambiado.emit(Number((evento.target as HTMLSelectElement).value));
  }
}
