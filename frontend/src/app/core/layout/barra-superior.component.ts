import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { AlmacenContextoService } from '../services/almacen-contexto.service';
import { ThemeService } from '../services/theme.service';
import { ApiService } from '../http/api.service';
import { ResumenAlertas } from '../models/alertas.model';

/**
 * Encabezado: almacen de trabajo, alertas, tema claro/oscuro y menu de usuario.
 * El selector de almacen define el contexto de todas las pantallas operativas.
 */
@Component({
  selector: 'sgai-barra-superior',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <header class="sticky top-0 z-30 flex h-14 items-center gap-2 border-b border-border
                   bg-surface/95 px-3 backdrop-blur sm:px-4">
      <button type="button" class="rounded-lg p-2 text-fg-muted hover:bg-surface-muted lg:hidden"
              aria-label="Abrir menu" (click)="menuSolicitado.emit()">
        <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M4 7h16M4 12h16M4 17h16" stroke-linecap="round" />
        </svg>
      </button>

      <a routerLink="/dashboard" class="flex items-center gap-2 lg:hidden">
        <span class="flex size-8 items-center justify-center rounded-lg bg-brand-600 text-sm
                     font-bold text-white">SG</span>
      </a>

      <!-- Almacen de trabajo -->
      @if (contexto.almacenes().length) {
        <label class="ml-auto flex items-center gap-2 lg:ml-0">
          <span class="sr-only">Almacen de trabajo</span>
          <select class="max-w-[11rem] rounded-lg border border-border bg-surface px-2.5 py-1.5
                         text-sm text-fg sm:max-w-xs"
                  [value]="contexto.seleccionadoId() ?? ''"
                  (change)="cambiarAlmacen($any($event.target).value)">
            @for (almacen of contexto.almacenes(); track almacen.id) {
              <option [value]="almacen.id">{{ almacen.codigo }} · {{ almacen.nombre }}</option>
            }
          </select>
        </label>
      }

      <div class="ml-auto flex items-center gap-1">
        <!-- Alertas -->
        <a routerLink="/alertas" class="relative rounded-lg p-2 text-fg-muted hover:bg-surface-muted"
           [attr.aria-label]="'Alertas activas: ' + (resumen()?.total ?? 0)">
          <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.6" stroke-linecap="round">
            <path d="M12 4a6 6 0 0 0-6 6v4l-2 3h16l-2-3v-4a6 6 0 0 0-6-6Zm-2 16a2 2 0 0 0 4 0" />
          </svg>
          @if ((resumen()?.total ?? 0) > 0) {
            <span class="absolute -right-0.5 -top-0.5 flex min-w-4 items-center justify-center
                         rounded-full bg-danger px-1 text-[10px] font-bold leading-4 text-white">
              {{ resumen()!.total > 99 ? '99+' : resumen()!.total }}
            </span>
          }
        </a>

        <!-- Tema -->
        <button type="button" class="rounded-lg p-2 text-fg-muted hover:bg-surface-muted"
                [attr.aria-label]="tema.tema() === 'dark' ? 'Activar tema claro' : 'Activar tema oscuro'"
                (click)="tema.alternar()">
          @if (tema.tema() === 'dark') {
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5M17.5 17.5 19 19M19 5l-1.5 1.5M6.5 17.5 5 19" />
            </svg>
          } @else {
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.6" stroke-linecap="round">
              <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />
            </svg>
          }
        </button>

        <!-- Usuario -->
        <div class="relative">
          <button type="button"
                  class="flex items-center gap-2 rounded-lg px-1.5 py-1 hover:bg-surface-muted"
                  [attr.aria-expanded]="menuUsuario()"
                  (click)="menuUsuario.set(!menuUsuario())">
            <span class="flex size-8 items-center justify-center rounded-full bg-brand-600
                         text-xs font-semibold text-white">{{ auth.iniciales() }}</span>
            <span class="hidden text-left sm:block">
              <span class="block text-xs font-medium leading-tight text-fg">
                {{ auth.usuario()?.nombreCompleto }}
              </span>
              <span class="block text-[11px] leading-tight text-fg-muted">{{ rolPrincipal() }}</span>
            </span>
          </button>

          @if (menuUsuario()) {
            <div class="absolute right-0 mt-1 w-56 overflow-hidden rounded-xl border border-border
                        bg-surface py-1 shadow-[var(--shadow-pop)]">
              <div class="border-b border-border px-3 py-2">
                <p class="truncate text-sm font-medium text-fg">{{ auth.usuario()?.nombreCompleto }}</p>
                <p class="truncate text-xs text-fg-muted">{{ auth.usuario()?.email }}</p>
              </div>
              <a routerLink="/perfil" class="block px-3 py-2 text-sm text-fg hover:bg-surface-muted"
                 (click)="menuUsuario.set(false)">Mi perfil</a>
              <a routerLink="/perfil/cambiar-password"
                 class="block px-3 py-2 text-sm text-fg hover:bg-surface-muted"
                 (click)="menuUsuario.set(false)">Cambiar contrasena</a>
              <button type="button"
                      class="block w-full px-3 py-2 text-left text-sm text-danger hover:bg-danger-soft"
                      (click)="cerrarSesion()">Cerrar sesion</button>
            </div>
          }
        </div>
      </div>
    </header>
  `,
  host: { class: 'block' },
})
export class BarraSuperiorComponent {
  readonly menuSolicitado = output<void>();

  protected readonly auth = inject(AuthService);
  protected readonly tema = inject(ThemeService);
  protected readonly contexto = inject(AlmacenContextoService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly menuUsuario = signal(false);
  protected readonly resumen = signal<ResumenAlertas | null>(null);
  protected readonly rolPrincipal = computed(() => {
    const roles = this.auth.roles();
    return roles.length ? roles[0].replace(/_/g, ' ') : '';
  });

  constructor() {
    if (this.auth.tienePermiso('alertas:alerta:leer')) {
      this.api.get<ResumenAlertas>('/alertas/resumen').subscribe({
        next: (resumen) => this.resumen.set(resumen),
        error: () => this.resumen.set(null),
      });
    }
  }

  protected cambiarAlmacen(valor: string): void {
    this.contexto.seleccionar(valor ? Number(valor) : null);
  }

  protected cerrarSesion(): void {
    this.menuUsuario.set(false);
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
