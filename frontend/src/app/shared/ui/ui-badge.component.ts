import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type TonoBadge = 'neutral' | 'marca' | 'exito' | 'advertencia' | 'peligro' | 'info';

/**
 * Etiqueta de estado. Todos los estados del sistema (lote, movimiento, orden,
 * requisicion, alerta) se pintan con este componente y el mapa de tonos de
 * `tono-estado.ts`, de modo que un mismo estado se ve igual en toda la app.
 */
@Component({
  selector: 'ui-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="clases()">
      @if (punto()) {
        <span class="size-1.5 rounded-full bg-current opacity-80" aria-hidden="true"></span>
      }
      <ng-content />
    </span>
  `,
  host: { class: 'contents' },
})
export class UiBadgeComponent {
  readonly tono = input<TonoBadge>('neutral');
  readonly punto = input(true);

  private static readonly TONOS: Record<TonoBadge, string> = {
    neutral: 'bg-surface-muted text-fg-muted border-border',
    marca: 'bg-brand-50 text-brand-700 border-brand-200 dark:bg-brand-950 dark:text-brand-200 dark:border-brand-800',
    exito: 'bg-success-soft text-success border-success/30',
    advertencia: 'bg-warning-soft text-warning border-warning/30',
    peligro: 'bg-danger-soft text-danger border-danger/30',
    info: 'bg-info-soft text-info border-info/30',
  };

  protected readonly clases = computed(() => [
    'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5',
    'text-xs font-medium whitespace-nowrap',
    UiBadgeComponent.TONOS[this.tono()],
  ].join(' '));
}
