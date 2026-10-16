import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { NotificacionService } from '../services/notificacion.service';

/**
 * Autorizacion de ruta por permiso. La UI oculta lo que el usuario no puede
 * hacer, pero la decision real sigue siendo del servidor (defensa en profundidad).
 * Se declara como `data: { permisos: ['modulo:recurso:accion'] }`.
 */
export const permisoGuard: CanActivateFn = (ruta) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notificacion = inject(NotificacionService);

  const requeridos = (ruta.data?.['permisos'] as string[] | undefined) ?? [];
  if (auth.tieneAlgunPermiso(requeridos)) {
    return true;
  }
  notificacion.advertencia('No cuenta con permisos para esa pantalla');
  return router.createUrlTree(['/dashboard']);
};
