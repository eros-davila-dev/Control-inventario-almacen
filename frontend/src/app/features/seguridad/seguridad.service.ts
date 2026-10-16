import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  PasswordTemporal,
  Permiso,
  RegistroAuditoria,
  Rol,
  RolRequest,
  UsuarioRequest,
  UsuarioSesion,
} from '../../core/models/seguridad.model';

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<UsuarioSesion>> {
    return this.api.getPage<UsuarioSesion>('/usuarios', parametros);
  }

  crear(cuerpo: UsuarioRequest): Observable<PasswordTemporal> {
    return this.api.post<PasswordTemporal>('/usuarios', cuerpo);
  }

  actualizar(id: number, cuerpo: UsuarioRequest): Observable<UsuarioSesion> {
    return this.api.put<UsuarioSesion>(`/usuarios/${id}`, cuerpo);
  }

  cambiarEstado(id: number, activo: boolean): Observable<UsuarioSesion> {
    return this.api.patch<UsuarioSesion>(`/usuarios/${id}/estado`, {}, { activo });
  }

  resetearPassword(id: number): Observable<PasswordTemporal> {
    return this.api.post<PasswordTemporal>(`/usuarios/${id}/reset-password`);
  }

  desbloquear(id: number): Observable<UsuarioSesion> {
    return this.api.post<UsuarioSesion>(`/usuarios/${id}/desbloquear`);
  }
}

@Injectable({ providedIn: 'root' })
export class RolAdminService {
  private readonly api = inject(ApiService);

  listar(): Observable<Rol[]> {
    return this.api.get<Rol[]>('/roles');
  }

  crear(cuerpo: RolRequest): Observable<Rol> {
    return this.api.post<Rol>('/roles', cuerpo);
  }

  actualizar(id: number, cuerpo: RolRequest): Observable<Rol> {
    return this.api.put<Rol>(`/roles/${id}`, cuerpo);
  }

  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`/roles/${id}`);
  }

  permisosPorModulo(): Observable<Record<string, Permiso[]>> {
    return this.api.get<Record<string, Permiso[]>>('/permisos');
  }
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<RegistroAuditoria>> {
    return this.api.getPage<RegistroAuditoria>('/auditoria', parametros);
  }
}
