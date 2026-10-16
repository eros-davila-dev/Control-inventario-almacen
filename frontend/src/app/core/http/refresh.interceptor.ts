import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Renovacion transparente de la sesion (login transitivo, HU-01).
 * Ante un 401 se renueva el token UNA sola vez y las peticiones que estaban en
 * vuelo se encolan y se reintentan con el token nuevo; si la renovacion falla,
 * se cierra la sesion y se vuelve al login conservando la ruta de retorno.
 */
let renovando = false;
const tokenRenovado = new BehaviorSubject<string | null>(null);

export const refreshInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const esPublico = peticion.url.includes('/auth/login') || peticion.url.includes('/auth/refresh');

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || esPublico) {
        return throwError(() => error);
      }

      if (renovando) {
        return tokenRenovado.pipe(
          filter((token): token is string => token !== null),
          take(1),
          switchMap((token) => siguiente(peticion.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
          }))),
        );
      }

      renovando = true;
      tokenRenovado.next(null);

      return auth.refrescar().pipe(
        switchMap((respuesta) => {
          renovando = false;
          tokenRenovado.next(respuesta.accessToken);
          return siguiente(peticion.clone({
            setHeaders: { Authorization: `Bearer ${respuesta.accessToken}` },
          }));
        }),
        catchError((errorRenovacion: unknown) => {
          renovando = false;
          auth.limpiarSesion();
          void router.navigate(['/login'], {
            queryParams: { retorno: router.url },
          });
          return throwError(() => errorRenovacion);
        }),
      );
    }),
  );
};
