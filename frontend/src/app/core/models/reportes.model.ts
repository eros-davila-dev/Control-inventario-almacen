export interface ValorizacionCategoria {
  categoriaId: number;
  categoriaNombre: string;
  productos: number;
  cantidad: number;
  valor: number;
}

export interface ConsumoMensual {
  anio: number;
  mes: number;
  area: string;
  centroCosto: string;
  cantidad: number;
  valor: number;
}

export interface RotacionProducto {
  productoId: number;
  codigo: string;
  nombre: string;
  cantidadConsumida: number;
  valorConsumido: number;
  ultimaSalida?: string;
}

export interface LoteVencimiento {
  loteId: number;
  codigoLote: string;
  fechaVencimiento: string;
  estadoLote: string;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  almacenId: number;
  almacenNombre: string;
  cantidad: number;
  valorExpuesto: number;
}

export interface BajaRotacion {
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  almacenId: number;
  almacenNombre: string;
  cantidad: number;
  valorizado: number;
  ultimoMovimiento?: string;
  diasSinMovimiento?: number;
}

export interface Dashboard {
  valorInventario: number;
  itemsConSaldo: number;
  itemsBajoMinimo: number;
  itemsAgotados: number;
  lotesPorVencer30: number;
  lotesPorVencer60: number;
  lotesPorVencer90: number;
  lotesVencidos: number;
  valorExpuestoVencimiento: number;
  requisicionesPendientes: number;
  ordenesPorRecibir: number;
  transferenciasEnTransito: number;
  tomasEnProceso: number;
  movimientosHoy: number;
  alertasCriticas: number;
  desviacionesFefo30Dias: number;
  valorizacionPorCategoria: ValorizacionCategoria[];
  consumoMensual: ConsumoMensual[];
  topRotacion: RotacionProducto[];
}
