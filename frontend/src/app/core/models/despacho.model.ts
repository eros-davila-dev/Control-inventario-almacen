export type EstadoRequisicion =
  | 'BORRADOR' | 'ENVIADA' | 'EN_ATENCION' | 'ATENDIDA_PARCIAL' | 'ATENDIDA' | 'ANULADA';

export type Prioridad = 'NORMAL' | 'URGENTE';

export interface RequisicionLinea {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  controlaLote: boolean;
  cantidadSolicitada: number;
  cantidadAtendida: number;
  pendiente: number;
  observacion?: string;
}

export interface Requisicion {
  id: number;
  numero: string;
  almacenSolicitanteId: number;
  almacenSolicitanteNombre: string;
  almacenAtencionId: number;
  almacenAtencionNombre: string;
  centroCosto: string;
  area: string;
  prioridad: Prioridad;
  fecha: string;
  fechaRequerida?: string;
  estado: EstadoRequisicion;
  estadoDescripcion: string;
  solicitante?: string;
  observacion?: string;
  lineas: RequisicionLinea[];
}

export interface RequisicionRequest {
  almacenSolicitanteId: number;
  almacenAtencionId: number;
  centroCosto: string;
  area: string;
  prioridad?: Prioridad;
  fechaRequerida?: string | null;
  observacion?: string | null;
  lineas: { productoId: number; cantidadSolicitada: number; observacion?: string | null }[];
}

export interface Despacho {
  id: number;
  numero: string;
  requisicionId: number;
  requisicionNumero: string;
  almacenId: number;
  almacenNombre: string;
  area?: string;
  centroCosto?: string;
  fecha: string;
  movimientoId?: number;
  entregadoA?: string;
  despachadoPor?: string;
  observacion?: string;
  valorTotal: number;
  lineas: {
    id: number;
    requisicionLineaId: number;
    productoId: number;
    productoCodigo: string;
    productoNombre: string;
    unidadMedida: string;
    loteId?: number;
    codigoLote?: string;
    fechaVencimiento?: string;
    cantidad: number;
    costoUnitario: number;
    fefoRespetado: boolean;
    motivoNoFefo?: string;
  }[];
}

export interface DespachoRequest {
  requisicionId: number;
  fecha?: string | null;
  entregadoA?: string | null;
  observacion?: string | null;
  ubicacionId?: number | null;
  lineas: {
    requisicionLineaId: number;
    loteId?: number | null;
    cantidad: number;
    motivoNoFefo?: string | null;
  }[];
}
