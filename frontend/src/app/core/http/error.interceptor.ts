import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/api.model';
import { NotificacionService } from '../services/notificacion.service';

/** Mensajes por codigo de negocio: el texto del servidor no se muestra en crudo. */
const TITULOS: Record<string, string> = {
  VALIDACION: 'Revise los datos ingresados',
  NO_ENCONTRADO: 'El registro no existe o fue eliminado',
  DUPLICADO: 'El registro ya existe',
  CONFLICTO: 'La operacion entra en conflicto con el estado actual',
  REGLA_NEGOCIO: 'La operacion no cumple una regla del almacen',
  ESTADO_INVALIDO: 'El documento no admite esa operacion en su estado actual',
  CONCURRENCIA: 'Otro usuario modifico el registro: vuelva a intentar',
  CREDENCIALES_INVALIDAS: 'Credenciales invalidas',
  CUENTA_BLOQUEADA: 'Cuenta bloqueada temporalmente',
  CUENTA_INACTIVA: 'La cuenta esta inactiva',
  PERMISO_DENEGADO: 'No cuenta con permisos para esta operacion',
  ALMACEN_NO_AUTORIZADO: 'No tiene asignado el almacen de la operacion',
  SEGREGACION_FUNCIONES: 'Quien registra el documento no puede aprobarlo',
  DEMASIADAS_PETICIONES: 'Demasiados intentos: espere un momento',
  STOCK_INSUFICIENTE: 'Stock insuficiente',
  LOTE_NO_DISPONIBLE: 'El lote no esta disponible para despacho',
  LOTE_VENCIDO: 'El lote esta vencido',
  PRODUCTO_REQUIERE_LOTE: 'El producto exige lote y fecha de vencimiento',
  MOVIMIENTO_ANULADO: 'El movimiento ya fue anulado',
  CANTIDAD_EXCEDE_PENDIENTE: 'La cantidad supera el saldo pendiente',
  CALIDAD_RECHAZADA: 'Control de calidad no conforme',
  REGISTRO_SANITARIO_VENCIDO: 'Registro sanitario vencido',
  VIDA_UTIL_INSUFICIENTE: 'Vida util remanente insuficiente',
  PASSWORD_DEBIL: 'La contrasena no cumple la politica de seguridad',
  PASSWORD_REUTILIZADA: 'No puede reutilizar contrasenas anteriores',
  ERROR_INTERNO: 'Error interno del sistema',
};

/**
 * Traduce los errores de la API a avisos entendibles y centraliza los casos
 * transversales: cambio de contrasena obligatorio y perdida de conexion.
 * Los 401 los maneja el interceptor de renovacion.
 */
export const errorInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const notificacion = inject(NotificacionService);
  const router = inject(Router);

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      if (error.status === 0) {
        notificacion.error('Sin conexion con el servidor',
          'Verifique su red o intente nuevamente en unos segundos.');
        return throwError(() => error);
      }

      const problema = (error.error ?? {}) as ProblemDetail;
      const codigo = problema.codigo ?? '';

      if (codigo === 'PASSWORD_CHANGE_REQUIRED') {
        notificacion.advertencia('Debe cambiar su contrasena antes de continuar');
        void router.navigate(['/perfil/cambiar-password']);
        return throwError(() => error);
      }

      // El login muestra su propio mensaje y la renovacion de sesion es silenciosa.
      if (peticion.url.includes('/auth/login') || peticion.url.includes('/auth/refresh')) {
        return throwError(() => error);
      }

      const titulo = TITULOS[codigo] ?? problema.title ?? 'No se pudo completar la operacion';
      const detalle = codigo === 'VALIDACION' && problema.errores
        ? Object.values(problema.errores).join(' · ')
        : problema.detail;

      notificacion.error(titulo, detalle);
      return throwError(() => error);
    }),
  );
};
