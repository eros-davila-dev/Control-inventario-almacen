/** Envoltura de paginacion que devuelve la API (PageResponse del backend). */
export interface PageResponse<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

/** Error de negocio en formato ProblemDetail (RFC 7807) con codigo estable. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  codigo?: CodigoError;
  ruta?: string;
  traceId?: string;
  errores?: Record<string, string>;
  [clave: string]: unknown;
}

export type CodigoError =
  | 'VALIDACION'
  | 'NO_ENCONTRADO'
  | 'DUPLICADO'
  | 'CONFLICTO'
  | 'REGLA_NEGOCIO'
  | 'ESTADO_INVALIDO'
  | 'CONCURRENCIA'
  | 'CREDENCIALES_INVALIDAS'
  | 'CUENTA_BLOQUEADA'
  | 'CUENTA_INACTIVA'
  | 'TOKEN_INVALIDO'
  | 'PASSWORD_CHANGE_REQUIRED'
  | 'PASSWORD_DEBIL'
  | 'PASSWORD_REUTILIZADA'
  | 'PERMISO_DENEGADO'
  | 'ALMACEN_NO_AUTORIZADO'
  | 'SEGREGACION_FUNCIONES'
  | 'DEMASIADAS_PETICIONES'
  | 'STOCK_INSUFICIENTE'
  | 'LOTE_NO_DISPONIBLE'
  | 'LOTE_VENCIDO'
  | 'PRODUCTO_REQUIERE_LOTE'
  | 'MOVIMIENTO_ANULADO'
  | 'CANTIDAD_EXCEDE_PENDIENTE'
  | 'CALIDAD_RECHAZADA'
  | 'REGISTRO_SANITARIO_VENCIDO'
  | 'VIDA_UTIL_INSUFICIENTE'
  | 'ERROR_INTERNO';

/** Parametros de consulta de una tabla paginada. */
export interface ParametrosPagina {
  page?: number;
  size?: number;
  sort?: string;
  [filtro: string]: string | number | boolean | undefined | null;
}

/** Opcion generica para selectores. */
export interface Opcion<T = string> {
  valor: T;
  etiqueta: string;
  descripcion?: string;
}

export const PAGINA_INICIAL: ParametrosPagina = { page: 0, size: 20 };
