import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { StockConsolidado } from '../../core/models/inventario.model';
import {
  BajaRotacion,
  ConsumoMensual,
  LoteVencimiento,
  RotacionProducto,
  ValorizacionCategoria,
} from '../../core/models/reportes.model';

/**
 * Reportes de gestion. El calculo vive en el servidor (una sola fuente de
 * verdad para los indicadores del proyecto) y la exportacion a CSV se descarga
 * con separador ";" y decimales con coma, listo para Excel en es-PE.
 */
@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly api = inject(ApiService);

  vencimientos(almacenId: number | null, dias: number): Observable<LoteVencimiento[]> {
    return this.api.get<LoteVencimiento[]>('/reportes/vencimientos', { almacenId, dias });
  }

  valorizacion(almacenId: number | null): Observable<ValorizacionCategoria[]> {
    return this.api.get<ValorizacionCategoria[]>('/reportes/valorizacion', { almacenId });
  }

  consumo(almacenId: number | null, meses: number): Observable<ConsumoMensual[]> {
    return this.api.get<ConsumoMensual[]>('/reportes/consumo', { almacenId, meses });
  }

  rotacion(almacenId: number | null, dias: number): Observable<RotacionProducto[]> {
    return this.api.get<RotacionProducto[]>('/reportes/rotacion', { almacenId, dias });
  }

  bajaRotacion(almacenId: number | null): Observable<BajaRotacion[]> {
    return this.api.get<BajaRotacion[]>('/reportes/baja-rotacion', { almacenId });
  }

  reposicion(almacenId: number | null): Observable<StockConsolidado[]> {
    return this.api.get<StockConsolidado[]>('/reportes/reposicion', { almacenId });
  }
}
