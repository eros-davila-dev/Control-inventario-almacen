import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AlmacenContextoService } from '../services/almacen-contexto.service';
import { BarraSuperiorComponent } from './barra-superior.component';
import { MenuLateralComponent } from './menu-lateral.component';
import { NavegacionInferiorComponent } from './navegacion-inferior.component';

/**
 * Estructura de la aplicacion autenticada.
 * Responsive: en escritorio hay menu lateral fijo (colapsable), en tablet se
 * reduce a iconos y en telefono se usa un cajon lateral mas una barra inferior.
 */
@Component({
  selector: 'sgai-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet, RouterLink, BarraSuperiorComponent, MenuLateralComponent,
    NavegacionInferiorComponent,
  ],
  template: `
    <div class="flex min-h-dvh">
      <!-- Menu lateral de escritorio -->
      <aside [class]="clasesAside()">
        <div class="flex h-14 items-center gap-2 border-b border-border px-3">
          <a routerLink="/dashboard" class="flex items-center gap-2 overflow-hidden">
            <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-600
                         text-sm font-bold text-white">SG</span>
            @if (!colapsado()) {
              <span class="min-w-0">
                <span class="block truncate text-sm font-semibold leading-tight text-fg">SGAI-CID</span>
                <span class="block truncate text-[11px] leading-tight text-fg-muted">
                  Almacen e inventarios
                </span>
              </span>
            }
          </a>
          <button type="button"
                  class="ml-auto hidden rounded-lg p-1.5 text-fg-muted hover:bg-surface-muted lg:block"
                  [attr.aria-label]="colapsado() ? 'Expandir menu' : 'Colapsar menu'"
                  (click)="colapsado.set(!colapsado())">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round">
              <path [attr.d]="colapsado() ? 'M9 6l6 6-6 6' : 'M15 6l-6 6 6 6'" />
            </svg>
          </button>
        </div>
        <sgai-menu-lateral [colapsado]="colapsado()" class="flex-1" />
        @if (!colapsado()) {
          <div class="border-t border-border px-4 py-3">
            <p class="text-[11px] leading-snug text-fg-subtle">
              Corporacion Integral de Dialisis S.A.C.
            </p>
          </div>
        }
      </aside>

      <!-- Cajon lateral de movil -->
      @if (cajonAbierto()) {
        <div class="fixed inset-0 z-40 lg:hidden">
          <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
               (click)="cajonAbierto.set(false)" aria-hidden="true"></div>
          <aside class="relative z-10 flex h-full w-72 max-w-[85vw] flex-col border-r border-border
                        bg-surface">
            <div class="flex h-14 items-center justify-between border-b border-border px-4">
              <span class="text-sm font-semibold text-fg">SGAI-CID</span>
              <button type="button" class="rounded-lg p-1.5 text-fg-muted hover:bg-surface-muted"
                      aria-label="Cerrar menu" (click)="cajonAbierto.set(false)">
                <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="1.8" stroke-linecap="round">
                  <path d="M6 6l12 12M18 6 6 18" />
                </svg>
              </button>
            </div>
            <sgai-menu-lateral class="flex-1" (navegado)="cajonAbierto.set(false)" />
          </aside>
        </div>
      }

      <!-- Contenido -->
      <div class="flex min-w-0 flex-1 flex-col">
        <sgai-barra-superior (menuSolicitado)="cajonAbierto.set(true)" />
        <main class="flex-1 px-3 pb-24 pt-4 sm:px-5 sm:pb-8 lg:px-7" id="contenido">
          <router-outlet />
        </main>
      </div>
    </div>
    <sgai-navegacion-inferior />
  `,
  host: { class: 'block' },
})
export class ShellComponent {
  protected readonly colapsado = signal(false);
  protected readonly cajonAbierto = signal(false);
  private readonly contexto = inject(AlmacenContextoService);

  constructor() {
    this.contexto.cargar();
  }

  protected clasesAside(): string {
    return [
      'hidden shrink-0 flex-col border-r border-border bg-surface lg:flex',
      'sticky top-0 h-dvh transition-[width]',
      this.colapsado() ? 'w-[4.5rem]' : 'w-64',
    ].join(' ');
  }
}
