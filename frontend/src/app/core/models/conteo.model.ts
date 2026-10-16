import { ClasificacionAbc } from './catalogo.model';

export type AlcanceToma = 'TOTAL' | 'POR_CATEGORIA' | 'CICLICO_ABC';
export type EstadoToma = 'PROGRAMADA' | 'EN_PROCESO' | 'CONTADA' | 'APROBADA' | 'ANULADA';

export interface LineaToma {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  loteId?: number;
  codigoLote?: string;
  fechaVencimiento?: string;
  ubicacionId?: number;
  ubicacionCodigo?: string;
  cantidadTeorica?: number;
  cantidadContada?: number;
  segundoConteo?: number;
  diferencia?: number;
  valorDiferencia?: number;
  contadoPor?: string;
  observacion?: string;
}

export interface TomaInventario {
  id: number;
  numero: string;
  almacenId: number;
  almacenNombre: string;
  alcance: AlcanceToma;
  alcanceDescripcion: string;
  categoriaId?: number;
  clasificacionAbc?: ClasificacionAbc;
  fechaProgramada: string;
  fechaInicio?: string;
  fechaCierre?: string;
  estado: EstadoToma;
  estadoDescripcion: string;
  responsable?: string;
  aprobador?: string;
  eri?: number;
  observacion?: string;
  itemsTotales: number;
  itemsContados: number;
  itemsConDiferencia: number;
  valorDiferencias: number;
  lineas: LineaToma[];
}

export interface TomaInventarioRequest {
  almacenId: number;
  alcance: AlcanceToma;
  categoriaId?: number | null;
  clasificacionAbc?: ClasificacionAbc | null;
  fechaProgramada?: string | null;
  observacion?: string | null;
}

export interface ConteoRequest {
  lineas: {
    lineaId: number;
    cantidadContada: number;
    esSegundoConteo?: boolean;
    observacion?: string | null;
  }[];
}
