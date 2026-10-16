import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marcador de carga para tablas y tarjetas. */
@Component({
  selector: 'ui-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @for (fila of filasArray(); track $index) {
      <div class="mb-2 h-9 animate-pulse rounded-lg bg-surface-muted"></div>
    }
  `,
  host: { class: 'block', 'aria-busy': 'true', 'aria-live': 'polite' },
})
export class UiSkeletonComponent {
  readonly filas = input(5);
  protected filasArray(): number[] {
    return Array.from({ length: this.filas() });
  }
}
