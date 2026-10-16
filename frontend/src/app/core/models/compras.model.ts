export type EstadoRequerimiento =
  | 'BORRADOR' | 'ENVIADO' | 'APROBADO' | 'RECHAZADO' | 'ATENDIDO' | 'ANULADO';

export type EstadoOrdenCompra =
  | 'BORRADOR' | 'APROBADA' | 'RECHAZADA' | 'RECIBIDA_PARCIAL' | 'RECIBIDA' | 'ANULADA';

export type ResultadoCalidad = 'CONFORME' | 'OBSERVADO' | 'RECHAZADO';

export interface SugerenciaReposicion {
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  critico: boolean;
  disponible: number;
  stockMinimo: number;
  puntoReorden: number;
  stockMaximo: number;
  consumoPromedioDiario: number;
  coberturaDias?: number;
  cantidadSugerida: number;
  estadoReposicion: string;
}

export interface Requerimiento {
  id: number;
  numero: string;
  almacenId: number;
  almacenNombre: string;
  fecha: string;
  estado: EstadoRequerimiento;
  estadoDescripcion: string;
  justificacion?: string;
  solicitante?: string;
  aprobador?: string;
  fechaAprobacion?: string;
  motivoRechazo?: string;
  lineas: {
    id: number;
    productoId: number;
    productoCodigo: string;
    productoNombre: string;
    unidadMedida: string;
    cantidadSolicitada: number;
    cantidadSugerida: number;
    disponibleActual: number;
    observacion?: string;
  }[];
}

export interface RequerimientoRequest {
  almacenId: number;
  justificacion?: string | null;
  lineas: { productoId: number; cantidadSolicitada: number; observacion?: string | null }[];
}

export interface OrdenCompraLinea {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  controlaLote: boolean;
  controlaVencimiento: boolean;
  requiereCadenaFrio: boolean;
  vidaUtilMinimaDias: number;
  cantidad: number;
  cantidadRecibida: number;
  pendiente: number;
  precioUnitario: number;
  importe: number;
  observacion?: string;
}

export interface OrdenCompra {
  id: number;
  numero: string;
  proveedorId: number;
  proveedorRuc: string;
  proveedorRazonSocial: string;
  requerimientoId?: number;
  requerimientoNumero?: string;
  almacenDestinoId: number;
  almacenDestinoNombre: string;
  fechaEmision: string;
  fechaEsperada?: string;
  moneda: string;
  condicionPago?: string;
  subtotal: number;
  igv: number;
  total: number;
  estado: EstadoOrdenCompra;
  estadoDescripcion: string;
  creadoPor?: string;
  aprobador?: string;
  fechaAprobacion?: string;
  observacion?: string;
  lineas: OrdenCompraLinea[];
}

export interface OrdenCompraRequest {
  proveedorId: number;
  requerimientoId?: number | null;
  almacenDestinoId: number;
  fechaEmision?: string | null;
  fechaEsperada?: string | null;
  moneda?: string;
  condicionPago?: string | null;
  observacion?: string | null;
  lineas: {
    productoId: number;
    cantidad: number;
    precioUnitario: number;
    observacion?: string | null;
  }[];
}

export interface Recepcion {
  id: number;
  numero: string;
  ordenId: number;
  ordenNumero: string;
  almacenId: number;
  almacenNombre: string;
  fecha: string;
  guiaRemision?: string;
  factura?: string;
  transportista?: string;
  temperaturaLlegada?: number;
  empaqueIntegro: boolean;
  resultadoCalidad: ResultadoCalidad;
  resultadoDescripcion: string;
  observacion?: string;
  movimientoId?: number;
  recibidoPor?: string;
  lineas: {
    id: number;
    productoId: number;
    productoCodigo: string;
    productoNombre: string;
    codigoLote?: string;
    fechaVencimiento?: string;
    loteId?: number;
    cantidad: number;
    precioUnitario: number;
    conforme: boolean;
    observacion?: string;
  }[];
}

export interface RecepcionRequest {
  ordenId: number;
  fecha?: string | null;
  guiaRemision?: string | null;
  factura?: string | null;
  transportista?: string | null;
  temperaturaLlegada?: number | null;
  empaqueIntegro?: boolean;
  resultadoCalidad: ResultadoCalidad;
  observacion?: string | null;
  ubicacionId?: number | null;
  lineas: {
    ordenLineaId: number;
    codigoLote?: string | null;
    fechaFabricacion?: string | null;
    fechaVencimiento?: string | null;
    cantidad: number;
    conforme?: boolean;
    observacion?: string | null;
  }[];
}
