export type EstadoLote = 'CUARENTENA' | 'LIBERADO' | 'RETENIDO' | 'RECHAZADO' | 'VENCIDO';

export type TipoMovimiento =
  | 'INGRESO_COMPRA'
  | 'INGRESO_DEVOLUCION'
  | 'INGRESO_AJUSTE'
  | 'INGRESO_TRANSFERENCIA'
  | 'SALIDA_DESPACHO'
  | 'SALIDA_MERMA'
  | 'SALIDA_VENCIMIENTO'
  | 'SALIDA_AJUSTE'
  | 'SALIDA_TRANSFERENCIA'
  | 'SALIDA_DEVOLUCION_PROVEEDOR';

export type EstadoMovimiento = 'REGISTRADO' | 'ANULADO';

export type MotivoAjuste =
  | 'CONTEO_FISICO'
  | 'MERMA'
  | 'VENCIMIENTO'
  | 'ROTURA'
  | 'ERROR_REGISTRO'
  | 'DONACION'
  | 'SALDO_INICIAL'
  | 'DEVOLUCION_AREA'
  | 'MERMA_TRANSITO';

export type EstadoReposicion =
  | 'AGOTADO'
  | 'BAJO_MINIMO'
  | 'EN_PUNTO_REORDEN'
  | 'SOBRE_STOCK'
  | 'NORMAL';

export interface Lote {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  codigoLote: string;
  proveedorId?: number;
  proveedorNombre?: string;
  fechaFabricacion?: string;
  fechaVencimiento?: string;
  diasParaVencer?: number;
  estadoLote: EstadoLote;
  estadoDescripcion: string;
  motivoEstado?: string;
  fechaLiberacion?: string;
  liberadoPor?: string;
  costoUnitario: number;
  saldoTotal: number;
  despachable: boolean;
  vencido: boolean;
}

export interface LoteRequest {
  productoId: number;
  codigoLote: string;
  proveedorId?: number | null;
  fechaFabricacion?: string | null;
  fechaVencimiento?: string | null;
  costoUnitario?: number;
  iniciarEnCuarentena?: boolean;
}

export interface CambioEstadoLoteRequest {
  estado: EstadoLote;
  motivo: string;
}

export interface Stock {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  almacenId: number;
  almacenNombre: string;
  loteId?: number;
  codigoLote?: string;
  fechaVencimiento?: string;
  estadoLote?: EstadoLote;
  diasParaVencer?: number;
  ubicacionId?: number;
  ubicacionCodigo?: string;
  cantidad: number;
  reservada: number;
  disponible: number;
  costoPromedio: number;
  valorizado: number;
  despachable: boolean;
}

export interface StockConsolidado {
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  unidadMedida: string;
  almacenId: number;
  almacenNombre: string;
  cantidad: number;
  reservada: number;
  disponible: number;
  stockMinimo: number;
  puntoReorden: number;
  stockMaximo: number;
  costoPromedio: number;
  valorizado: number;
  consumoPromedioDiario: number;
  coberturaDias?: number;
  critico: boolean;
  estadoReposicion: EstadoReposicion;
}

export interface SugerenciaLote {
  loteId?: number;
  codigoLote: string;
  fechaVencimiento?: string;
  diasParaVencer?: number;
  ubicacionId?: number;
  ubicacionCodigo?: string;
  disponible: number;
  cantidadSugerida: number;
  costoUnitario: number;
}

export interface MovimientoLinea {
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
  cantidad: number;
  costoUnitario: number;
  costoTotal: number;
  fefoRespetado: boolean;
  motivoNoFefo?: string;
}

export interface Movimiento {
  id: number;
  numero: string;
  tipoMovimiento: TipoMovimiento;
  tipoDescripcion: string;
  estadoMovimiento: EstadoMovimiento;
  fecha: string;
  almacenOrigenId: number;
  almacenOrigenNombre: string;
  almacenDestinoId?: number;
  almacenDestinoNombre?: string;
  motivoAjuste?: MotivoAjuste;
  documentoTipo?: string;
  documentoNumero?: string;
  proveedorId?: number;
  proveedorNombre?: string;
  centroCosto?: string;
  areaDestino?: string;
  observacion?: string;
  movimientoAnuladoId?: number;
  referenciaTipo?: string;
  referenciaId?: number;
  valorTotal: number;
  registradoPor?: string;
  registradoEn?: string;
  lineas: MovimientoLinea[];
}

export interface LineaIngresoRequest {
  productoId: number;
  codigoLote?: string | null;
  fechaFabricacion?: string | null;
  fechaVencimiento?: string | null;
  loteId?: number | null;
  ubicacionId?: number | null;
  cantidad: number;
  costoUnitario: number;
}

export interface IngresoRequest {
  almacenId: number;
  tipoMovimiento: TipoMovimiento;
  fecha?: string | null;
  documentoTipo?: string | null;
  documentoNumero?: string | null;
  proveedorId?: number | null;
  motivoAjuste?: MotivoAjuste | null;
  observacion?: string | null;
  lineas: LineaIngresoRequest[];
}

export interface LineaSalidaRequest {
  productoId: number;
  loteId?: number | null;
  ubicacionId?: number | null;
  cantidad: number;
  motivoNoFefo?: string | null;
}

export interface SalidaRequest {
  almacenId: number;
  tipoMovimiento: TipoMovimiento;
  fecha?: string | null;
  centroCosto?: string | null;
  areaDestino?: string | null;
  motivoAjuste?: MotivoAjuste | null;
  documentoTipo?: string | null;
  documentoNumero?: string | null;
  observacion?: string | null;
  lineas: LineaSalidaRequest[];
}

export interface AjusteRequest {
  almacenId: number;
  motivoAjuste: MotivoAjuste;
  incrementa: boolean;
  observacion: string;
  lineas: {
    productoId: number;
    loteId?: number | null;
    ubicacionId?: number | null;
    cantidad: number;
    costoUnitario?: number | null;
  }[];
}

export interface Kardex {
  id: number;
  fecha: string;
  numeroDocumento: string;
  tipoMovimiento: TipoMovimiento;
  tipoDescripcion: string;
  codigoLote?: string;
  entrada: number;
  salida: number;
  saldo: number;
  costoUnitario: number;
  costoPromedio: number;
  valorSaldo: number;
  usuario?: string;
}

export type EstadoTransferencia = 'ENVIADA' | 'RECIBIDA' | 'RECIBIDA_PARCIAL' | 'ANULADA';

export interface TransferenciaLinea {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  loteId?: number;
  codigoLote?: string;
  fechaVencimiento?: string;
  cantidadEnviada: number;
  cantidadRecibida: number;
  diferencia: number;
  costoUnitario: number;
  observacion?: string;
}

export interface Transferencia {
  id: number;
  numero: string;
  almacenOrigenId: number;
  almacenOrigenNombre: string;
  almacenDestinoId: number;
  almacenDestinoNombre: string;
  fechaEnvio: string;
  fechaRecepcion?: string;
  estado: EstadoTransferencia;
  estadoDescripcion: string;
  transportista?: string;
  guiaRemision?: string;
  observacion?: string;
  enviadoPor?: string;
  recibidoPor?: string;
  movimientoSalidaId?: number;
  movimientoIngresoId?: number;
  tieneDiferencias: boolean;
  lineas: TransferenciaLinea[];
}

export interface TransferenciaRequest {
  almacenOrigenId: number;
  almacenDestinoId: number;
  fechaEnvio?: string | null;
  transportista?: string | null;
  guiaRemision?: string | null;
  observacion?: string | null;
  lineas: { productoId: number; loteId?: number | null; ubicacionId?: number | null; cantidad: number }[];
}

export interface RecepcionTransferenciaRequest {
  ubicacionDestinoId?: number | null;
  observacion?: string | null;
  lineas: { transferenciaLineaId: number; cantidadRecibida: number; observacion?: string | null }[];
}

export interface TrazabilidadLote {
  lote: Lote;
  ubicacionesActuales: Stock[];
  movimientos: {
    movimientoId: number;
    numero: string;
    tipo: string;
    tipoDescripcion: string;
    fecha: string;
    almacen: string;
    areaDestino?: string;
    centroCosto?: string;
    cantidad: number;
    documento?: string;
    usuario?: string;
    estado: string;
  }[];
}
