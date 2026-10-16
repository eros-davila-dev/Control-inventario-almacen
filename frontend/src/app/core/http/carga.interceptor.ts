import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { CargaService } from '../services/carga.service';

/** Alimenta la barra de progreso global con las peticiones en vuelo. */
export const cargaInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const carga = inject(CargaService);
  carga.iniciar();
  return siguiente(peticion).pipe(finalize(() => carga.terminar()));
};
