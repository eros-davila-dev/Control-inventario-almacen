export type TipoProducto =
  | 'INSUMO_DIALISIS'
  | 'MEDICAMENTO'
  | 'MATERIAL_MEDICO'
  | 'REACTIVO'
  | 'EPP'
  | 'REPUESTO_MAQUINA'
  | 'LIMPIEZA'
  | 'OFICINA';

export type CondicionAlmacenamiento =
  | 'AMBIENTE'
  | 'REFRIGERADO_2_8'
  | 'CONGELADO'
  | 'PROTEGIDO_LUZ';

export type ClasificacionAbc = 'A' | 'B' | 'C';

export interface UnidadMedida {
  id: number;
  codigo: string;
  nombre: string;
  simbolo?: string;
  umBaseId?: number;
  umBaseCodigo?: string;
  factorABase: number;
  esBase: boolean;
  activo: boolean;
}

export interface UnidadMedidaRequest {
  codigo: string;
  nombre: string;
  simbolo?: string | null;
  umBaseId?: number | null;
  factorABase: number;
  activo?: boolean;
}

export interface Categoria {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  categoriaPadreId?: number;
  categoriaPadreNombre?: string;
  requiereCadenaFrio: boolean;
  activo: boolean;
}

export interface CategoriaRequest {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  categoriaPadreId?: number | null;
  requiereCadenaFrio?: boolean;
  activo?: boolean;
}

export interface Producto {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  categoriaId: number;
  categoriaNombre: string;
  unidadMedidaId: number;
  unidadMedidaCodigo: string;
  unidadMedidaNombre: string;
  tipoProducto: TipoProducto;
  presentacion?: string;
  marca?: string;
  registroSanitario?: string;
  venceRegistroSanitario?: string;
  registroSanitarioVigente: boolean;
  condicionAlmacenamiento: CondicionAlmacenamiento;
  requiereCadenaFrio: boolean;
  controlaLote: boolean;
  controlaVencimiento: boolean;
  vidaUtilMinimaDias: number;
  clasificacionAbc: ClasificacionAbc;
  stockMinimo: number;
  puntoReorden: number;
  stockMaximo: number;
  diasCobertura: number;
  costoPromedio: number;
  critico: boolean;
  activo: boolean;
}

export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  categoriaId: number;
  unidadMedidaId: number;
  tipoProducto: TipoProducto;
  presentacion?: string | null;
  marca?: string | null;
  registroSanitario?: string | null;
  venceRegistroSanitario?: string | null;
  condicionAlmacenamiento: CondicionAlmacenamiento;
  controlaLote?: boolean;
  controlaVencimiento?: boolean;
  vidaUtilMinimaDias?: number;
  clasificacionAbc?: ClasificacionAbc;
  stockMinimo: number;
  puntoReorden: number;
  stockMaximo: number;
  diasCobertura?: number;
  critico?: boolean;
  activo?: boolean;
}

export interface ProductoResumen {
  id: number;
  codigo: string;
  nombre: string;
  unidadMedida: string;
  controlaLote: boolean;
  controlaVencimiento: boolean;
  critico: boolean;
}

export interface Proveedor {
  id: number;
  ruc: string;
  razonSocial: string;
  nombreComercial?: string;
  contacto?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  condicionPago?: string;
  leadTimeDias: number;
  activo: boolean;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  nombreComercial?: string | null;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  condicionPago?: string | null;
  leadTimeDias?: number;
  activo?: boolean;
}
