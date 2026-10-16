import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
  inject,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { authInterceptor } from './core/http/auth.interceptor';
import { cargaInterceptor } from './core/http/carga.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';
import { refreshInterceptor } from './core/http/refresh.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Sin Zone.js: la deteccion de cambios se apoya en signals (Angular 20).
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    provideHttpClient(
      withFetch(),
      // El orden importa: carga -> token -> renovacion -> traduccion de errores.
      withInterceptors([cargaInterceptor, authInterceptor, refreshInterceptor, errorInterceptor]),
    ),
    // Renovacion silenciosa al abrir la aplicacion: si hay refresh token valido
    // el usuario entra directo sin volver a escribir sus credenciales.
    provideAppInitializer(() => inject(AuthService).restaurarSesion()),
  ],
};
