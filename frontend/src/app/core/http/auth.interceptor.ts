import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

/** Adjunta el access token a las llamadas a la API, salvo a los endpoints publicos. */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const esPublico = peticion.url.includes('/auth/login') || peticion.url.includes('/auth/refresh');
  const token = auth.accessToken();

  if (!token || esPublico || !peticion.url.startsWith('/api')) {
    return siguiente(peticion);
  }
  return siguiente(peticion.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  }));
};
