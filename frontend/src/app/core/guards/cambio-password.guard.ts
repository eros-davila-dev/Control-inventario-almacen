import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Mientras el usuario tenga pendiente el cambio de contrasena obligatorio
 * (HU-03) solo puede navegar a su perfil. El backend aplica la misma regla.
 */
export const cambioPasswordGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.debeCambiarPassword()) {
    return true;
  }
  return router.createUrlTree(['/perfil/cambiar-password']);
};
