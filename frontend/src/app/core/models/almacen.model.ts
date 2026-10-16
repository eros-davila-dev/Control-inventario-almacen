export type TipoAlmacen = 'CENTRAL' | 'SEDE' | 'FARMACIA' | 'CUARENTENA' | 'FRIO' | 'RECHAZADOS';
export type TipoUbicacion = 'PICKING' | 'RESERVA' | 'FRIO' | 'CUARENTENA';

export interface Almacen {
  id: number;
  codigo: string;
  nombre: string;
  sede: string;
  direccion?: string;
  tipoAlmacen: TipoAlmacen;
  tipoAlmacenDescripcion: string;
  responsableId?: number;
  telefono?: string;
  permiteNegativo: boolean;
  despachable: boolean;
  activo: boolean;
}

export interface AlmacenRequest {
  codigo: string;
  nombre: string;
  sede: string;
  direccion?: string | null;
  tipoAlmacen: TipoAlmacen;
  responsableId?: number | null;
  telefono?: string | null;
  permiteNegativo?: boolean;
  activo?: boolean;
}

export interface Ubicacion {
  id: number;
  almacenId: number;
  almacenNombre: string;
  codigo: string;
  descripcion?: string;
  zona?: string;
  rack?: string;
  nivel?: string;
  tipoUbicacion: TipoUbicacion;
  activo: boolean;
}

export interface UbicacionRequest {
  almacenId: number;
  codigo: string;
  descripcion?: string | null;
  zona?: string | null;
  rack?: string | null;
  nivel?: string | null;
  tipoUbicacion?: TipoUbicacion;
  activo?: boolean;
}
