import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiConfirmComponent } from './shared/ui/ui-confirm.component';
import { UiProgressBarComponent } from './shared/ui/ui-progress-bar.component';
import { UiToastComponent } from './shared/ui/ui-toast.component';

/** Raiz de la aplicacion: enrutador y servicios visuales globales. */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, UiToastComponent, UiConfirmComponent, UiProgressBarComponent],
  template: `
    <a href="#contenido"
       class="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-[80]
              focus:rounded-lg focus:bg-brand-600 focus:px-3 focus:py-2 focus:text-sm
              focus:text-white">
      Saltar al contenido
    </a>
    <ui-progress-bar />
    <router-outlet />
    <ui-toast />
    <ui-confirm />
  `,
})
export class App {}
