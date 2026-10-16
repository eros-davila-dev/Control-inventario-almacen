import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TokenResponse, UsuarioSesion } from '../models/seguridad.model';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

const USUARIO: UsuarioSesion = {
  id: 2,
  username: 'almacenero',
  email: 'almacenero@nipromed.pe',
  nombres: 'Luis',
  apellidos: 'Carrasco',
  nombreCompleto: 'Luis Carrasco',
  activo: true,
  debeCambiarPassword: false,
  roles: ['ALMACENERO'],
  permisos: ['inventario:stock:leer', 'despacho:despacho:registrar'],
  almacenIds: [1, 2],
};

const TOKENS: TokenResponse = {
  accessToken: 'jwt-de-acceso',
  refreshToken: 'token-de-refresco',
  tipo: 'Bearer',
  expiraEnSegundos: 900,
  usuario: USUARIO,
};

describe('AuthService', () => {
  let servicio: AuthService;
  let almacenamiento: TokenStorageService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servicio = TestBed.inject(AuthService);
    almacenamiento = TestBed.inject(TokenStorageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('no esta autenticado antes de iniciar sesion', () => {
    expect(servicio.autenticado()).toBeFalse();
    expect(servicio.tienePermiso('inventario:stock:leer')).toBeFalse();
  });

  it('al iniciar sesion guarda el refresh token y expone permisos y roles', () => {
    servicio.login({ usuario: 'almacenero', password: 'Clave123*' }).subscribe();
    http.expectOne('/api/v1/auth/login').flush(TOKENS);

    expect(servicio.autenticado()).toBeTrue();
    expect(servicio.accessToken()).toBe('jwt-de-acceso');
    expect(servicio.tienePermiso('despacho:despacho:registrar')).toBeTrue();
    expect(servicio.tienePermiso('seguridad:usuario:crear')).toBeFalse();
    expect(servicio.tieneAlgunPermiso(['x', 'inventario:stock:leer'])).toBeTrue();
    expect(servicio.esAdministrador()).toBeFalse();
    expect(servicio.iniciales()).toBe('LC');
    expect(servicio.almacenesAsignados()).toEqual([1, 2]);
  });

  it('el access token no se persiste: solo vive en memoria', () => {
    servicio.login({ usuario: 'almacenero', password: 'Clave123*' }).subscribe();
    http.expectOne('/api/v1/auth/login').flush(TOKENS);

    expect(almacenamiento.leerRefresh()).toBe('token-de-refresco');
    expect(JSON.stringify(localStorage)).not.toContain('jwt-de-acceso');
  });

  it('restaura la sesion con el refresh token guardado (login transitivo)', () => {
    almacenamiento.guardarRefresh('token-de-refresco');
    let restaurada: boolean | undefined;

    servicio.restaurarSesion().subscribe((valor) => (restaurada = valor));
    http.expectOne('/api/v1/auth/refresh').flush(TOKENS);

    expect(restaurada).toBeTrue();
    expect(servicio.autenticado()).toBeTrue();
    expect(servicio.sesionIniciada()).toBeTrue();
  });

  it('sin refresh token no llama al servidor y marca la sesion como resuelta', () => {
    let restaurada: boolean | undefined;

    servicio.restaurarSesion().subscribe((valor) => (restaurada = valor));

    expect(restaurada).toBeFalse();
    expect(servicio.sesionIniciada()).toBeTrue();
  });

  it('si el refresh token ya no sirve limpia la sesion', () => {
    almacenamiento.guardarRefresh('token-vencido');
    let restaurada: boolean | undefined;

    servicio.restaurarSesion().subscribe((valor) => (restaurada = valor));
    http.expectOne('/api/v1/auth/refresh')
      .flush({ codigo: 'TOKEN_INVALIDO' }, { status: 401, statusText: 'Unauthorized' });

    expect(restaurada).toBeFalse();
    expect(servicio.autenticado()).toBeFalse();
    expect(almacenamiento.leerRefresh()).toBeNull();
  });

  it('al cambiar la password cierra la sesion local', () => {
    servicio.login({ usuario: 'almacenero', password: 'Clave123*' }).subscribe();
    http.expectOne('/api/v1/auth/login').flush(TOKENS);

    servicio.cambiarPassword({ passwordActual: 'Clave123*', passwordNueva: 'Nueva456*', confirmacion: 'Nueva456*' })
      .subscribe();
    http.expectOne('/api/v1/perfil/password').flush(null);

    expect(servicio.autenticado()).toBeFalse();
    expect(almacenamiento.leerRefresh()).toBeNull();
  });
});
