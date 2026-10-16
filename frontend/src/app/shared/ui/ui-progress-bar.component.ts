import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CargaService } from '../../core/services/carga.service';

/** Barra de progreso global de las peticiones en vuelo. */
@Component({
  selector: 'ui-progress-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (carga.cargando()) {
      <div class="fixed inset-x-0 top-0 z-[60] h-0.5 overflow-hidden bg-brand-100
                  dark:bg-brand-950" role="progressbar" aria-label="Cargando">
        <div class="h-full w-1/3 animate-[barra_1.1s_ease-in-out_infinite] bg-brand-600"></div>
      </div>
    }
  `,
  styles: [`
    @keyframes barra {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(300%); }
    }
  `],
})
export class UiProgressBarComponent {
  protected readonly carga = inject(CargaService);
}
