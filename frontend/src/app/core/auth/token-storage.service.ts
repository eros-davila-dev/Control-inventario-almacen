import { Injectable } from '@angular/core';
import { UsuarioSesion } from '../models/seguridad.model';

/**
 * Persistencia de la sesion en el cliente. El access token vive solo en memoria
 * (AuthService); aqui se guarda unicamente el refresh token y una copia del
 * perfil para poder pintar la interfaz antes de la primera renovacion.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private static readonly CLAVE_REFRESH = 'sgai.refresh';
  private static readonly CLAVE_USUARIO = 'sgai.usuario';

  guardarRefresh(token: string): void {
    this.escribir(TokenStorageService.CLAVE_REFRESH, token);
  }

  leerRefresh(): string | null {
    return this.leer(TokenStorageService.CLAVE_REFRESH);
  }

  guardarUsuario(usuario: UsuarioSesion): void {
    this.escribir(TokenStorageService.CLAVE_USUARIO, JSON.stringify(usuario));
  }

  leerUsuario(): UsuarioSesion | null {
    const bruto = this.leer(TokenStorageService.CLAVE_USUARIO);
    if (!bruto) {
      return null;
    }
    try {
      return JSON.parse(bruto) as UsuarioSesion;
    } catch {
      return null;
    }
  }

  limpiar(): void {
    this.borrar(TokenStorageService.CLAVE_REFRESH);
    this.borrar(TokenStorageService.CLAVE_USUARIO);
  }

  // El almacenamiento puede no estar disponible (modo privado, politicas del
  // navegador): la aplicacion debe seguir funcionando igualmente.
  private escribir(clave: string, valor: string): void {
    try {
      localStorage.setItem(clave, valor);
    } catch {
      /* sin persistencia: la sesion durara solo lo que dure la pestana */
    }
  }

  private leer(clave: string): string | null {
    try {
      return localStorage.getItem(clave);
    } catch {
      return null;
    }
  }

  private borrar(clave: string): void {
    try {
      localStorage.removeItem(clave);
    } catch {
      /* nada que limpiar */
    }
  }
}
