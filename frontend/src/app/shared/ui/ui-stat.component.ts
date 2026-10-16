import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TonoBadge } from './ui-badge.component';

/** Tarjeta de indicador del tablero (HU-43). */
@Component({
  selector: 'ui-stat',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="sgai-card flex items-start gap-3 p-4 transition-shadow hover:shadow-[var(--shadow-pop)]">
      <div [class]="clasesIcono()" aria-hidden="true">
        <ng-content select="[icono]" />
      </div>
      <div class="min-w-0 flex-1">
        <p class="truncate text-xs font-medium uppercase tracking-wide text-fg-muted">
          {{ etiqueta() }}
        </p>
        <p class="mt-1 text-2xl font-semibold tabular-nums text-fg">{{ valor() }}</p>
        @if (detalle()) {
          <p class="mt-0.5 truncate text-xs text-fg-subtle">{{ detalle() }}</p>
        }
      </div>
    </article>
  `,
  host: { class: 'block' },
})
export class UiStatComponent {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly detalle = input('');
  readonly tono = input<TonoBadge>('marca');

  private static readonly TONOS: Record<TonoBadge, string> = {
    neutral: 'bg-surface-muted text-fg-muted',
    marca: 'bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-200',
    exito: 'bg-success-soft text-success',
    advertencia: 'bg-warning-soft text-warning',
    peligro: 'bg-danger-soft text-danger',
    info: 'bg-info-soft text-info',
  };

  protected readonly clasesIcono = computed(() =>
    `flex size-10 shrink-0 items-center justify-center rounded-xl ${UiStatComponent.TONOS[this.tono()]}`);
}
