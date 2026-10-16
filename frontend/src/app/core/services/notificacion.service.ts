import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'exito' | 'error' | 'advertencia' | 'info';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje?: string;
  duracionMs: number;
}

/**
 * Centro de avisos de la aplicacion (toasts). Los componentes no manipulan el
 * DOM: publican aqui y el contenedor global los renderiza con aria-live.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private secuencia = 0;
  private readonly _notificaciones = signal<Notificacion[]>([]);
  readonly notificaciones = this._notificaciones.asReadonly();

  exito(titulo: string, mensaje?: string): void {
    this.publicar('exito', titulo, mensaje, 4000);
  }

  error(titulo: string, mensaje?: string): void {
    this.publicar('error', titulo, mensaje, 8000);
  }

  advertencia(titulo: string, mensaje?: string): void {
    this.publicar('advertencia', titulo, mensaje, 6000);
  }

  info(titulo: string, mensaje?: string): void {
    this.publicar('info', titulo, mensaje, 4000);
  }

  cerrar(id: number): void {
    this._notificaciones.update((lista) => lista.filter((n) => n.id !== id));
  }

  private publicar(tipo: TipoNotificacion, titulo: string, mensaje: string | undefined,
                   duracionMs: number): void {
    const id = ++this.secuencia;
    this._notificaciones.update((lista) => [...lista, { id, tipo, titulo, mensaje, duracionMs }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }
}
