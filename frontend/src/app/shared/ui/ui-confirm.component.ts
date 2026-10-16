import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { UiButtonComponent } from './ui-button.component';
import { UiModalComponent } from './ui-modal.component';

/**
 * Dialogo global de confirmacion. Para operaciones sensibles del almacen
 * (anular un movimiento, retener un lote) puede exigir un motivo que queda
 * en la pista de auditoria.
 */
@Component({
  selector: 'ui-confirm',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiModalComponent, UiButtonComponent],
  template: `
    @let solicitud = servicio.pendiente();
    @if (solicitud) {
      <ui-modal [abierto]="true" [titulo]="solicitud.titulo" tamano="sm"
                [cerrarAlClicarFuera]="false" (cerrado)="responder(false)">
        <p class="text-sm text-fg-muted">{{ solicitud.mensaje }}</p>
        @if (solicitud.pedirMotivo) {
          <label class="mt-4 block">
            <span class="sgai-label">
              {{ solicitud.etiquetaMotivo || 'Motivo' }}
              <span class="text-danger" aria-hidden="true">*</span>
            </span>
            <textarea class="sgai-input" rows="3" maxlength="500" [value]="motivo()"
                      (input)="motivo.set($any($event.target).value)"
                      placeholder="Indique el motivo de la operacion"></textarea>
            @if (intentado() && !motivoValido()) {
              <span class="mt-1 block text-xs font-medium text-danger">
                El motivo es obligatorio y queda registrado en la auditoria.
              </span>
            }
          </label>
        }
        <div pie class="flex gap-2">
          <ui-button variante="secundario" (presionado)="responder(false)">
            {{ solicitud.textoCancelar || 'Cancelar' }}
          </ui-button>
          <ui-button [variante]="solicitud.peligroso ? 'peligro' : 'primario'"
                     (presionado)="responder(true)">
            {{ solicitud.textoConfirmar || 'Confirmar' }}
          </ui-button>
        </div>
      </ui-modal>
    }
  `,
})
export class UiConfirmComponent {
  protected readonly servicio = inject(ConfirmacionService);
  protected readonly motivo = signal('');
  protected readonly intentado = signal(false);

  protected motivoValido(): boolean {
    return this.motivo().trim().length >= 3;
  }

  protected responder(confirmado: boolean): void {
    const solicitud = this.servicio.pendiente();
    if (confirmado && solicitud?.pedirMotivo && !this.motivoValido()) {
      this.intentado.set(true);
      return;
    }
    const motivo = this.motivo().trim() || undefined;
    this.motivo.set('');
    this.intentado.set(false);
    this.servicio.responder(confirmado, motivo);
  }
}
