import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { GrupoNavegacion, ICONOS, NAVEGACION } from './nav.config';

/**
 * Menu lateral. Solo muestra las entradas cuyos permisos tiene el usuario, de
 * modo que un almacenero no ve opciones de administracion que no podria usar.
 */
@Component({
  selector: 'sgai-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="sgai-scroll flex h-full flex-col gap-5 overflow-y-auto px-3 py-4"
         aria-label="Menu principal">
      @for (grupo of gruposVisibles(); track grupo.titulo) {
        <div>
          @if (!colapsado()) {
            <p class="mb-1.5 px-2 text-[11px] font-semibold uppercase tracking-wider text-fg-subtle">
              {{ grupo.titulo }}
            </p>
          }
          <ul class="space-y-0.5">
            @for (item of grupo.items; track item.ruta) {
              <li>
                <a [routerLink]="item.ruta"
                   routerLinkActive="bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-200"
                   [routerLinkActiveOptions]="{ exact: item.ruta === '/almacenes' }"
                   [attr.title]="colapsado() ? item.etiqueta : null"
                   class="flex items-center gap-2.5 rounded-lg px-2 py-2 text-sm font-medium
                          text-fg-muted transition-colors hover:bg-surface-muted hover:text-fg"
                   (click)="navegado.emit()">
                  <svg class="size-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
                       aria-hidden="true">
                    <path [attr.d]="icono(item.icono)" />
                  </svg>
                  @if (!colapsado()) {
                    <span class="truncate">{{ item.etiqueta }}</span>
                  }
                </a>
              </li>
            }
          </ul>
        </div>
      }
    </nav>
  `,
  host: { class: 'block h-full' },
})
export class MenuLateralComponent {
  readonly colapsado = input(false);
  readonly navegado = output<void>();

  private readonly auth = inject(AuthService);

  protected readonly gruposVisibles = computed<GrupoNavegacion[]>(() =>
    NAVEGACION
      .map((grupo) => ({
        ...grupo,
        items: grupo.items.filter((item) => this.auth.tieneAlgunPermiso(item.permisos ?? [])),
      }))
      .filter((grupo) => grupo.items.length > 0));

  protected icono(clave: string): string {
    return ICONOS[clave] ?? '';
  }
}
