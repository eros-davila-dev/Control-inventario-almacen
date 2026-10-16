export interface UsuarioSesion {
  id: number;
  username: string;
  email: string;
  nombres: string;
  apellidos: string;
  nombreCompleto: string;
  dni?: string;
  cargo?: string;
  telefono?: string;
  activo: boolean;
  debeCambiarPassword: boolean;
  ultimoAcceso?: string;
  roles: string[];
  permisos: string[];
  almacenIds: number[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tipo: string;
  expiraEnSegundos: number;
  usuario: UsuarioSesion;
}

export interface LoginRequest {
  usuario: string;
  password: string;
}

export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
  confirmacion: string;
}

export interface UsuarioRequest {
  username: string;
  email: string;
  nombres: string;
  apellidos: string;
  dni?: string | null;
  cargo?: string | null;
  telefono?: string | null;
  activo?: boolean;
  rolIds: number[];
  almacenIds?: number[];
}

export interface PasswordTemporal {
  usuarioId: number;
  username: string;
  passwordTemporal: string;
}

export interface Permiso {
  id: number;
  codigo: string;
  modulo: string;
  descripcion: string;
}

export interface Rol {
  id: number;
  nombre: string;
  descripcion?: string;
  esSistema: boolean;
  activo: boolean;
  usuariosAsignados: number;
  permisos: Permiso[];
}

export interface RolRequest {
  nombre: string;
  descripcion?: string | null;
  activo?: boolean;
  permisoIds: number[];
}

export interface RegistroAuditoria {
  id: number;
  usuario: string;
  accion: string;
  entidad: string;
  entidadId?: string;
  detalle?: string;
  ip?: string;
  userAgent?: string;
  exitoso: boolean;
  fecha: string;
}
