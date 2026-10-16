import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  ConteoRequest,
  TomaInventario,
  TomaInventarioRequest,
} from '../../core/models/conteo.model';

/**
 * Toma de inventario fisico. El servidor congela el saldo teorico al abrir la
 * toma y lo oculta mientras se cuenta (conteo ciego), de modo que el conteo no
 * se "cuadre" contra el sistema.
 */
@Injectable({ providedIn: 'root' })
export class TomaInventarioService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<TomaInventario>> {
    return this.api.getPage<TomaInventario>('/tomas-inventario', parametros);
  }

  obtener(id: number): Observable<TomaInventario> {
    return this.api.get<TomaInventario>(`/tomas-inventario/${id}`);
  }

  programar(cuerpo: TomaInventarioRequest): Observable<TomaInventario> {
    return this.api.post<TomaInventario>('/tomas-inventario', cuerpo);
  }

  abrir(id: number): Observable<TomaInventario> {
    return this.api.post<TomaInventario>(`/tomas-inventario/${id}/abrir`);
  }

  contar(id: number, cuerpo: ConteoRequest): Observable<TomaInventario> {
    return this.api.post<TomaInventario>(`/tomas-inventario/${id}/conteos`, cuerpo);
  }

  cerrarConteo(id: number): Observable<TomaInventario> {
    return this.api.post<TomaInventario>(`/tomas-inventario/${id}/cerrar-conteo`);
  }

  aprobar(id: number): Observable<TomaInventario> {
    return this.api.post<TomaInventario>(`/tomas-inventario/${id}/aprobar`);
  }

  anular(id: number, motivo: string): Observable<TomaInventario> {
    return this.api.postConParametros<TomaInventario>(`/tomas-inventario/${id}/anular`, { motivo });
  }
}
