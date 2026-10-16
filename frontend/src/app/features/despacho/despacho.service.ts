import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  Despacho,
  DespachoRequest,
  Requisicion,
  RequisicionRequest,
} from '../../core/models/despacho.model';

@Injectable({ providedIn: 'root' })
export class RequisicionService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Requisicion>> {
    return this.api.getPage<Requisicion>('/requisiciones', parametros);
  }

  obtener(id: number): Observable<Requisicion> {
    return this.api.get<Requisicion>(`/requisiciones/${id}`);
  }

  crear(cuerpo: RequisicionRequest): Observable<Requisicion> {
    return this.api.post<Requisicion>('/requisiciones', cuerpo);
  }

  enviar(id: number): Observable<Requisicion> {
    return this.api.post<Requisicion>(`/requisiciones/${id}/enviar`);
  }

  marcarEnAtencion(id: number): Observable<Requisicion> {
    return this.api.post<Requisicion>(`/requisiciones/${id}/en-atencion`);
  }

  anular(id: number, motivo: string): Observable<Requisicion> {
    return this.api.postConParametros<Requisicion>(`/requisiciones/${id}/anular`, { motivo });
  }

  despachosDe(id: number): Observable<Despacho[]> {
    return this.api.get<Despacho[]>(`/requisiciones/${id}/despachos`);
  }
}

@Injectable({ providedIn: 'root' })
export class DespachoService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Despacho>> {
    return this.api.getPage<Despacho>('/despachos', parametros);
  }

  obtener(id: number): Observable<Despacho> {
    return this.api.get<Despacho>(`/despachos/${id}`);
  }

  atender(cuerpo: DespachoRequest): Observable<Despacho> {
    return this.api.post<Despacho>('/despachos', cuerpo);
  }
}
