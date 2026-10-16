import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Oculta un bloque si el usuario no tiene el permiso indicado.
 * Es una ayuda de interfaz, no un control de seguridad: la autorizacion real
 * siempre la decide el backend (defensa en profundidad, docs/05-SEGURIDAD.md).
 *
 * Uso: `<div *hasPermiso="'inventario:movimiento:registrar'">...</div>`
 *      `<div *hasPermiso="['compras:orden:crear', 'compras:orden:aprobar']">...</div>`
 */
@Directive({
  selector: '[hasPermiso]',
})
export class HasPermisoDirective {
  readonly hasPermiso = input.required<string | string[]>();

  private readonly auth = inject(AuthService);
  private readonly plantilla = inject(TemplateRef<unknown>);
  private readonly contenedor = inject(ViewContainerRef);
  private visible = false;

  constructor() {
    effect(() => {
      const requeridos = this.hasPermiso();
      const permitido = this.auth.tieneAlgunPermiso(
        Array.isArray(requeridos) ? requeridos : [requeridos]);

      if (permitido && !this.visible) {
        this.contenedor.createEmbeddedView(this.plantilla);
        this.visible = true;
      } else if (!permitido && this.visible) {
        this.contenedor.clear();
        this.visible = false;
      }
    });
  }
}
