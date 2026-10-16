import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Exige sesion activa; conserva la ruta solicitada para volver tras el login. */
export const autenticadoGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.autenticado()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { retorno: estado.url } });
};
