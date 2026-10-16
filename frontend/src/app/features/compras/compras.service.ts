import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  OrdenCompra,
  OrdenCompraRequest,
  Recepcion,
  RecepcionRequest,
  Requerimiento,
  RequerimientoRequest,
  SugerenciaReposicion,
} from '../../core/models/compras.model';

@Injectable({ providedIn: 'root' })
export class RequerimientoService {
  private readonly api = inject(ApiService);

  /** Propuesta de reposicion calculada por el servidor (punto de reorden + consumo). */
  sugerencia(almacenId: number): Observable<SugerenciaReposicion[]> {
    return this.api.get<SugerenciaReposicion[]>(`/requerimientos-compra/sugerencia/${almacenId}`);
  }

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Requerimiento>> {
    return this.api.getPage<Requerimiento>('/requerimientos-compra', parametros);
  }

  obtener(id: number): Observable<Requerimiento> {
    return this.api.get<Requerimiento>(`/requerimientos-compra/${id}`);
  }

  crear(cuerpo: RequerimientoRequest): Observable<Requerimiento> {
    return this.api.post<Requerimiento>('/requerimientos-compra', cuerpo);
  }

  enviar(id: number): Observable<Requerimiento> {
    return this.api.post<Requerimiento>(`/requerimientos-compra/${id}/enviar`);
  }

  aprobar(id: number): Observable<Requerimiento> {
    return this.api.post<Requerimiento>(`/requerimientos-compra/${id}/aprobar`);
  }

  rechazar(id: number, motivo: string): Observable<Requerimiento> {
    return this.api.postConParametros<Requerimiento>(
      `/requerimientos-compra/${id}/rechazar`, { motivo });
  }
}

@Injectable({ providedIn: 'root' })
export class OrdenCompraService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<OrdenCompra>> {
    return this.api.getPage<OrdenCompra>('/ordenes-compra', parametros);
  }

  obtener(id: number): Observable<OrdenCompra> {
    return this.api.get<OrdenCompra>(`/ordenes-compra/${id}`);
  }

  crear(cuerpo: OrdenCompraRequest): Observable<OrdenCompra> {
    return this.api.post<OrdenCompra>('/ordenes-compra', cuerpo);
  }

  aprobar(id: number): Observable<OrdenCompra> {
    return this.api.post<OrdenCompra>(`/ordenes-compra/${id}/aprobar`);
  }

  rechazar(id: number, motivo: string): Observable<OrdenCompra> {
    return this.api.postConParametros<OrdenCompra>(`/ordenes-compra/${id}/rechazar`, { motivo });
  }

  anular(id: number, motivo: string): Observable<OrdenCompra> {
    return this.api.postConParametros<OrdenCompra>(`/ordenes-compra/${id}/anular`, { motivo });
  }

  recepcionesDe(id: number): Observable<Recepcion[]> {
    return this.api.get<Recepcion[]>(`/ordenes-compra/${id}/recepciones`);
  }
}

@Injectable({ providedIn: 'root' })
export class RecepcionService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Recepcion>> {
    return this.api.getPage<Recepcion>('/recepciones', parametros);
  }

  obtener(id: number): Observable<Recepcion> {
    return this.api.get<Recepcion>(`/recepciones/${id}`);
  }

  registrar(cuerpo: RecepcionRequest): Observable<Recepcion> {
    return this.api.post<Recepcion>('/recepciones', cuerpo);
  }
}
