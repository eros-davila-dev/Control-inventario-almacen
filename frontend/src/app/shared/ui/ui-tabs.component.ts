import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface Pestana {
  clave: string;
  titulo: string;
  contador?: number;
}

/** Pestanas controladas por signal (model), sin dependencias externas. */
@Component({
  selector: 'ui-tabs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sgai-scroll -mx-1 mb-4 flex gap-1 overflow-x-auto px-1" role="tablist">
      @for (pestana of pestanas(); track pestana.clave) {
        <button type="button" role="tab"
                [attr.aria-selected]="activa() === pestana.clave"
                [class]="clases(pestana.clave)"
                (click)="activa.set(pestana.clave)">
          {{ pestana.titulo }}
          @if (pestana.contador !== undefined) {
            <span class="ml-1.5 rounded-full bg-current/15 px-1.5 text-xs tabular-nums">
              {{ pestana.contador }}
            </span>
          }
        </button>
      }
    </div>
  `,
  host: { class: 'block' },
})
export class UiTabsComponent {
  readonly pestanas = input.required<Pestana[]>();
  readonly activa = model.required<string>();

  protected clases(clave: string): string {
    const base = 'whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-colors';
    return this.activa() === clave
      ? `${base} bg-brand-600 text-white shadow-sm`
      : `${base} text-fg-muted hover:bg-surface-muted hover:text-fg`;
  }
}
