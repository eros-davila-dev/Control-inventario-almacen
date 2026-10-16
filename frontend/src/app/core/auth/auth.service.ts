import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { ApiService } from '../http/api.service';
import {
  CambiarPasswordRequest,
  LoginRequest,
  TokenResponse,
  UsuarioSesion,
} from '../models/seguridad.model';
import { TokenStorageService } from './token-storage.service';

/**
 * Estado de la sesion. El access token se mantiene en memoria (signal) y la
 * renovacion es transparente: mientras el refresh token sea valido el usuario
 * no vuelve a ver la pantalla de login (login transitivo, HU-01).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly storage = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _usuario = signal<UsuarioSesion | null>(this.storage.leerUsuario());
  private readonly _sesionIniciada = signal(false);

  readonly usuario = this._usuario.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();
  readonly sesionIniciada = this._sesionIniciada.asReadonly();

  readonly autenticado = computed(() => this._accessToken() !== null);
  readonly debeCambiarPassword = computed(() => this._usuario()?.debeCambiarPassword === true);
  readonly permisos = computed(() => new Set(this._usuario()?.permisos ?? []));
  readonly roles = computed(() => this._usuario()?.roles ?? []);
  readonly esAdministrador = computed(() => this.roles().includes('ADMIN'));
  readonly almacenesAsignados = computed(() => this._usuario()?.almacenIds ?? []);
  readonly iniciales = computed(() => {
    const usuario = this._usuario();
    if (!usuario) {
      return '--';
    }
    return `${usuario.nombres.charAt(0)}${usuario.apellidos.charAt(0)}`.toUpperCase();
  });

  tienePermiso(codigo: string): boolean {
    return this.permisos().has(codigo);
  }

  tieneAlgunPermiso(codigos: string[]): boolean {
    if (!codigos.length) {
      return true;
    }
    const permisos = this.permisos();
    return codigos.some((codigo) => permisos.has(codigo));
  }

  login(credenciales: LoginRequest): Observable<TokenResponse> {
    return this.api.post<TokenResponse>('/auth/login', credenciales).pipe(
      tap((respuesta) => this.aplicarSesion(respuesta)),
    );
  }

  /** Renovacion del access token; devuelve error si la sesion ya no es valida. */
  refrescar(): Observable<TokenResponse> {
    const refreshToken = this.storage.leerRefresh();
    if (!refreshToken) {
      return throwError(() => new Error('sin refresh token'));
    }
    return this.api.post<TokenResponse>('/auth/refresh', { refreshToken }).pipe(
      tap((respuesta) => this.aplicarSesion(respuesta)),
    );
  }

  /**
   * Restaura la sesion al recargar la pagina: si hay refresh token valido se
   * obtiene un access token nuevo sin pedir credenciales al usuario.
   */
  restaurarSesion(): Observable<boolean> {
    if (!this.storage.leerRefresh()) {
      this._sesionIniciada.set(true);
      return of(false);
    }
    return this.refrescar().pipe(
      map(() => true),
      catchError(() => {
        this.limpiarSesion();
        return of(false);
      }),
      tap(() => this._sesionIniciada.set(true)),
    );
  }

  logout(redirigir = true): void {
    const refreshToken = this.storage.leerRefresh();
    const terminar = () => {
      this.limpiarSesion();
      if (redirigir) {
        void this.router.navigate(['/login']);
      }
    };
    if (!refreshToken) {
      terminar();
      return;
    }
    this.api.post('/auth/logout', { refreshToken }).pipe(catchError(() => of(null)))
      .subscribe(() => terminar());
  }

  cambiarPassword(request: CambiarPasswordRequest): Observable<void> {
    return this.api.post<void>('/perfil/password', request).pipe(
      tap(() => {
        // Al cambiar la clave el servidor revoca las sesiones: se exige login.
        this.limpiarSesion();
      }),
    );
  }

  cargarPerfil(): Observable<UsuarioSesion> {
    return this.api.get<UsuarioSesion>('/perfil').pipe(
      tap((usuario) => {
        this._usuario.set(usuario);
        this.storage.guardarUsuario(usuario);
      }),
    );
  }

  aplicarSesion(respuesta: TokenResponse): void {
    this._accessToken.set(respuesta.accessToken);
    this._usuario.set(respuesta.usuario);
    this.storage.guardarRefresh(respuesta.refreshToken);
    this.storage.guardarUsuario(respuesta.usuario);
    this._sesionIniciada.set(true);
  }

  limpiarSesion(): void {
    this._accessToken.set(null);
    this._usuario.set(null);
    this.storage.limpiar();
  }
}
