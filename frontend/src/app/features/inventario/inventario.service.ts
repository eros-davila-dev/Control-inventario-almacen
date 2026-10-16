import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  AjusteRequest,
  CambioEstadoLoteRequest,
  IngresoRequest,
  Kardex,
  Lote,
  LoteRequest,
  Movimiento,
  RecepcionTransferenciaRequest,
  SalidaRequest,
  Stock,
  StockConsolidado,
  SugerenciaLote,
  Transferencia,
  TransferenciaRequest,
  TrazabilidadLote,
} from '../../core/models/inventario.model';

@Injectable({ providedIn: 'root' })
export class StockConsultaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Stock>> {
    return this.api.getPage<Stock>('/stock', parametros);
  }

  consolidado(parametros: Record<string, unknown>): Observable<StockConsolidado[]> {
    return this.api.get<StockConsolidado[]>('/stock/consolidado', parametros);
  }

  porProductoYAlmacen(productoId: number, almacenId: number): Observable<Stock[]> {
    return this.api.get<Stock[]>(`/stock/producto/${productoId}/almacen/${almacenId}`);
  }

  /** Sugerencia FEFO del servidor: nunca se calcula en el cliente. */
  sugerenciaFefo(productoId: number, almacenId: number, cantidad: number):
    Observable<SugerenciaLote[]> {
    return this.api.get<SugerenciaLote[]>('/stock/sugerencia-fefo',
      { productoId, almacenId, cantidad });
  }
}

@Injectable({ providedIn: 'root' })
export class LoteConsultaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Lote>> {
    return this.api.getPage<Lote>('/lotes', parametros);
  }

  crear(cuerpo: LoteRequest): Observable<Lote> {
    return this.api.post<Lote>('/lotes', cuerpo);
  }

  liberar(id: number, motivo: string): Observable<Lote> {
    return this.api.patch<Lote>(`/lotes/${id}/liberar`, { estado: 'LIBERADO', motivo });
  }

  cambiarEstado(id: number, cuerpo: CambioEstadoLoteRequest): Observable<Lote> {
    return this.api.patch<Lote>(`/lotes/${id}/estado`, cuerpo);
  }

  trazabilidad(id: number): Observable<TrazabilidadLote> {
    return this.api.get<TrazabilidadLote>(`/lotes/${id}/trazabilidad`);
  }

  liberadosDe(productoId: number): Observable<Lote[]> {
    return this.api.get<Lote[]>(`/lotes/producto/${productoId}/liberados`);
  }
}

@Injectable({ providedIn: 'root' })
export class MovimientoConsultaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Movimiento>> {
    return this.api.getPage<Movimiento>('/movimientos', parametros);
  }

  obtener(id: number): Observable<Movimiento> {
    return this.api.get<Movimiento>(`/movimientos/${id}`);
  }

  /**
   * Registra un ingreso. La clave de idempotencia evita duplicar el documento
   * si el usuario reintenta por una conexion inestable.
   */
  registrarIngreso(cuerpo: IngresoRequest, idempotencyKey: string): Observable<Movimiento> {
    return this.api.post<Movimiento>('/movimientos/ingresos', cuerpo,
      { cabeceras: { 'Idempotency-Key': idempotencyKey } });
  }

  registrarSalida(cuerpo: SalidaRequest, idempotencyKey: string): Observable<Movimiento> {
    return this.api.post<Movimiento>('/movimientos/salidas', cuerpo,
      { cabeceras: { 'Idempotency-Key': idempotencyKey } });
  }

  registrarAjuste(cuerpo: AjusteRequest): Observable<Movimiento> {
    return this.api.post<Movimiento>('/movimientos/ajustes', cuerpo);
  }

  anular(id: number, motivo: string): Observable<Movimiento> {
    return this.api.post<Movimiento>(`/movimientos/${id}/anular`, { motivo });
  }
}

@Injectable({ providedIn: 'root' })
export class KardexConsultaService {
  private readonly api = inject(ApiService);

  consultar(parametros: ParametrosPagina): Observable<PageResponse<Kardex>> {
    return this.api.getPage<Kardex>('/kardex', parametros);
  }
}

@Injectable({ providedIn: 'root' })
export class TransferenciaConsultaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Transferencia>> {
    return this.api.getPage<Transferencia>('/transferencias', parametros);
  }

  obtener(id: number): Observable<Transferencia> {
    return this.api.get<Transferencia>(`/transferencias/${id}`);
  }

  enviar(cuerpo: TransferenciaRequest): Observable<Transferencia> {
    return this.api.post<Transferencia>('/transferencias', cuerpo);
  }

  recibir(id: number, cuerpo: RecepcionTransferenciaRequest): Observable<Transferencia> {
    return this.api.post<Transferencia>(`/transferencias/${id}/recibir`, cuerpo);
  }
}
