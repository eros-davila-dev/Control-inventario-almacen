import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Estado vacio con guia de la siguiente accion. */
@Component({
  selector: 'ui-empty',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      <div class="flex size-12 items-center justify-center rounded-full bg-surface-muted
                  text-fg-subtle" aria-hidden="true">
        <svg class="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M3 7.5 12 3l9 4.5v9L12 21l-9-4.5v-9Z" stroke-linejoin="round" />
          <path d="M3 7.5 12 12m0 0 9-4.5M12 12v9" stroke-linejoin="round" />
        </svg>
      </div>
      <p class="text-sm font-medium text-fg">{{ titulo() }}</p>
      @if (descripcion()) {
        <p class="max-w-md text-sm text-fg-muted">{{ descripcion() }}</p>
      }
      <ng-content />
    </div>
  `,
  host: { class: 'block' },
})
export class UiEmptyComponent {
  readonly titulo = input('Sin informacion para mostrar');
  readonly descripcion = input('');
}
