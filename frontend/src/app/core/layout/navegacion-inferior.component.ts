import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { ICONOS, NAVEGACION } from './nav.config';

/** Navegacion inferior para telefonos: accesos directos a lo que mas se usa en piso. */
@Component({
  selector: 'sgai-navegacion-inferior',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface/95 backdrop-blur
                pb-[env(safe-area-inset-bottom)] lg:hidden"
         aria-label="Accesos rapidos">
      <ul class="flex items-stretch justify-around">
        @for (item of itemsMovil(); track item.ruta) {
          <li class="flex-1">
            <a [routerLink]="item.ruta"
               routerLinkActive="text-brand-600 dark:text-brand-300"
               class="flex flex-col items-center gap-1 px-1 py-2.5 text-[11px] font-medium
                      text-fg-muted transition-colors">
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
                   aria-hidden="true">
                <path [attr.d]="icono(item.icono)" />
              </svg>
              <span class="truncate">{{ item.etiqueta }}</span>
            </a>
          </li>
        }
        <li class="flex-1">
          <a routerLink="/perfil"
             routerLinkActive="text-brand-600 dark:text-brand-300"
             class="flex flex-col items-center gap-1 px-1 py-2.5 text-[11px] font-medium
                    text-fg-muted transition-colors">
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
                 aria-hidden="true">
              <path [attr.d]="icono('perfil')" />
            </svg>
            <span>Perfil</span>
          </a>
        </li>
      </ul>
    </nav>
  `,
})
export class NavegacionInferiorComponent {
  private readonly auth = inject(AuthService);

  protected readonly itemsMovil = computed(() =>
    NAVEGACION
      .flatMap((grupo) => grupo.items)
      .filter((item) => item.enMovil)
      .filter((item) => this.auth.tieneAlgunPermiso(item.permisos ?? []))
      .slice(0, 4));

  protected icono(clave: string): string {
    return ICONOS[clave] ?? '';
  }
}
