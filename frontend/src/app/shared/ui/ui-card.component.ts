import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Contenedor de superficie con titulo y zona de acciones. */
@Component({
  selector: 'ui-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="sgai-card overflow-hidden">
      @if (titulo() || subtitulo()) {
        <header class="flex flex-wrap items-start justify-between gap-3 border-b border-border
                       px-4 py-3 sm:px-5">
          <div class="min-w-0">
            <h2 class="text-sm font-semibold text-fg sm:text-base">{{ titulo() }}</h2>
            @if (subtitulo()) {
              <p class="mt-0.5 text-xs text-fg-muted sm:text-sm">{{ subtitulo() }}</p>
            }
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <ng-content select="[acciones]" />
          </div>
        </header>
      }
      <div [class]="sinRelleno() ? '' : 'px-4 py-4 sm:px-5'">
        <ng-content />
      </div>
    </section>
  `,
  host: { class: 'block' },
})
export class UiCardComponent {
  readonly titulo = input('');
  readonly subtitulo = input('');
  readonly sinRelleno = input(false);
}
