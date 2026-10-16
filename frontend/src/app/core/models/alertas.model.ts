export type TipoAlerta =
  | 'STOCK_AGOTADO'
  | 'STOCK_BAJO'
  | 'SOBRE_STOCK'
  | 'POR_VENCER'
  | 'VENCIDO'
  | 'LOTE_RETENIDO'
  | 'SIN_MOVIMIENTO'
  | 'REGISTRO_SANITARIO';

export type SeveridadAlerta = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
export type EstadoAlerta = 'ACTIVA' | 'LEIDA' | 'RESUELTA';

export interface Alerta {
  id: number;
  tipoAlerta: TipoAlerta;
  tipoDescripcion: string;
  severidad: SeveridadAlerta;
  estado: EstadoAlerta;
  productoId?: number;
  productoNombre?: string;
  almacenId?: number;
  almacenNombre?: string;
  loteId?: number;
  codigoLote?: string;
  mensaje: string;
  valorReferencia?: number;
  fechaGeneracion: string;
  fechaResolucion?: string;
}

export interface ResumenAlertas {
  total: number;
  criticas: number;
  altas: number;
  medias: number;
  bajas: number;
  stockBajo: number;
  porVencer: number;
  vencidos: number;
}
