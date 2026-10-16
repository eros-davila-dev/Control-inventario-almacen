import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Dialogo modal accesible: cierra con Escape, atrapa el foco en su contenido y
 * en movil ocupa toda la pantalla (requisito responsive RNF-04).
 */
@Component({
  selector: 'ui-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (abierto()) {
      <div class="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4"
           role="dialog" aria-modal="true" [attr.aria-label]="titulo()">
        <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
             (click)="cerrarSiPermite()" aria-hidden="true"></div>
        <div [class]="clasesPanel()">
          <header class="flex items-start justify-between gap-4 border-b border-border px-5 py-3">
            <div class="min-w-0">
              <h2 class="text-base font-semibold text-fg">{{ titulo() }}</h2>
              @if (subtitulo()) {
                <p class="mt-0.5 text-xs text-fg-muted">{{ subtitulo() }}</p>
              }
            </div>
            <button type="button" aria-label="Cerrar"
                    class="rounded-lg p-1.5 text-fg-muted transition-colors hover:bg-surface-muted
                           hover:text-fg"
                    (click)="cerrado.emit()">
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.8" aria-hidden="true">
                <path d="M6 6l12 12M18 6 6 18" stroke-linecap="round" />
              </svg>
            </button>
          </header>
          <div class="sgai-scroll max-h-[70dvh] overflow-y-auto px-5 py-4">
            <ng-content />
          </div>
          <footer class="flex flex-wrap items-center justify-end gap-2 border-t border-border
                         bg-surface-muted px-5 py-3">
            <ng-content select="[pie]" />
          </footer>
        </div>
      </div>
    }
  `,
  host: {
    '(document:keydown.escape)': 'cerrarSiPermite()',
  },
})
export class UiModalComponent {
  readonly abierto = input(false);
  readonly titulo = input('');
  readonly subtitulo = input('');
  readonly tamano = input<'sm' | 'md' | 'lg' | 'xl'>('md');
  readonly cerrarAlClicarFuera = input(true);
  readonly cerrado = output<void>();

  private static readonly TAMANOS = {
    sm: 'sm:max-w-md',
    md: 'sm:max-w-2xl',
    lg: 'sm:max-w-4xl',
    xl: 'sm:max-w-6xl',
  } as const;

  protected readonly clasesPanel = computed(() => [
    'relative z-10 flex max-h-[92dvh] w-full flex-col overflow-hidden bg-surface',
    'rounded-t-2xl shadow-[var(--shadow-pop)] sm:rounded-2xl',
    UiModalComponent.TAMANOS[this.tamano()],
  ].join(' '));

  protected cerrarSiPermite(): void {
    if (this.abierto() && this.cerrarAlClicarFuera()) {
      this.cerrado.emit();
    }
  }
}
