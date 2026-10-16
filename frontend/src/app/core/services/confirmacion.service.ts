import { Injectable, signal } from '@angular/core';

export interface SolicitudConfirmacion {
  titulo: string;
  mensaje: string;
  textoConfirmar?: string;
  textoCancelar?: string;
  peligroso?: boolean;
  /** Cuando se indica, se exige al usuario escribir un motivo. */
  pedirMotivo?: boolean;
  etiquetaMotivo?: string;
}

interface EstadoConfirmacion extends SolicitudConfirmacion {
  resolver: (valor: { confirmado: boolean; motivo?: string }) => void;
}

/**
 * Dialogo de confirmacion reutilizable. Evita que cada pantalla implemente su
 * propio modal para operaciones sensibles (anular, eliminar, aprobar).
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  private readonly _pendiente = signal<EstadoConfirmacion | null>(null);
  readonly pendiente = this._pendiente.asReadonly();

  preguntar(solicitud: SolicitudConfirmacion): Promise<{ confirmado: boolean; motivo?: string }> {
    return new Promise((resolve) => {
      this._pendiente.set({ ...solicitud, resolver: resolve });
    });
  }

  responder(confirmado: boolean, motivo?: string): void {
    const pendiente = this._pendiente();
    this._pendiente.set(null);
    pendiente?.resolver({ confirmado, motivo });
  }
}
