import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Encabezado de pantalla: titulo, contexto y acciones principales. */
@Component({
  selector: 'ui-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-xl font-semibold tracking-tight text-fg sm:text-2xl">{{ titulo() }}</h1>
        @if (descripcion()) {
          <p class="mt-1 max-w-3xl text-sm text-fg-muted">{{ descripcion() }}</p>
        }
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ng-content select="[acciones]" />
      </div>
    </header>
  `,
  host: { class: 'block' },
})
export class UiPageHeaderComponent {
  readonly titulo = input.required<string>();
  readonly descripcion = input('');
}
