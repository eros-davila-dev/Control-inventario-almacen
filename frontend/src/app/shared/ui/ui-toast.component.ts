import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificacionService, TipoNotificacion } from '../../core/services/notificacion.service';

/** Contenedor global de avisos, anunciado por lectores de pantalla. */
@Component({
  selector: 'ui-toast',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-0 z-[70] flex flex-col items-center gap-2
                px-3 pb-20 sm:inset-x-auto sm:right-4 sm:bottom-4 sm:items-end sm:pb-0"
         role="status" aria-live="polite">
      @for (aviso of servicio.notificaciones(); track aviso.id) {
        <div [class]="clases(aviso.tipo)">
          <span class="mt-0.5 shrink-0" aria-hidden="true">{{ icono(aviso.tipo) }}</span>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold">{{ aviso.titulo }}</p>
            @if (aviso.mensaje) {
              <p class="mt-0.5 text-xs opacity-90">{{ aviso.mensaje }}</p>
            }
          </div>
          <button type="button" class="shrink-0 rounded p-0.5 text-current opacity-60 hover:opacity-100"
                  aria-label="Cerrar aviso" (click)="servicio.cerrar(aviso.id)">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 6l12 12M18 6 6 18" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      }
    </div>
  `,
})
export class UiToastComponent {
  protected readonly servicio = inject(NotificacionService);

  protected clases(tipo: TipoNotificacion): string {
    const base = 'pointer-events-auto flex w-full max-w-sm items-start gap-2.5 rounded-xl border '
      + 'px-3.5 py-3 shadow-[var(--shadow-pop)] animate-in';
    const tonos: Record<TipoNotificacion, string> = {
      exito: 'bg-success-soft text-success border-success/30',
      error: 'bg-danger-soft text-danger border-danger/30',
      advertencia: 'bg-warning-soft text-warning border-warning/30',
      info: 'bg-info-soft text-info border-info/30',
    };
    return `${base} ${tonos[tipo]}`;
  }

  protected icono(tipo: TipoNotificacion): string {
    const iconos: Record<TipoNotificacion, string> = {
      exito: '✓', error: '✕', advertencia: '!', info: 'i',
    };
    return iconos[tipo];
  }
}
