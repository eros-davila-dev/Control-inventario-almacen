import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import { Alerta, ResumenAlertas } from '../../core/models/alertas.model';

/**
 * Centro de alertas. Las alertas las genera el servidor (eventos de stock y
 * tareas programadas de madrugada), de modo que la pantalla solo consulta y
 * gestiona su ciclo de vida: activa -> leida -> resuelta.
 */
@Injectable({ providedIn: 'root' })
export class AlertaService {
  private readonly api = inject(ApiService);

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Alerta>> {
    return this.api.getPage<Alerta>('/alertas', parametros);
  }

  resumen(almacenId?: number | null): Observable<ResumenAlertas> {
    return this.api.get<ResumenAlertas>('/alertas/resumen', { almacenId: almacenId ?? null });
  }

  marcarLeida(id: number): Observable<Alerta> {
    return this.api.patch<Alerta>(`/alertas/${id}/leida`);
  }

  resolver(id: number): Observable<Alerta> {
    return this.api.patch<Alerta>(`/alertas/${id}/resolver`);
  }

  /** Fuerza la reevaluacion de reglas sin esperar a la tarea nocturna. */
  recalcular(almacenId?: number | null): Observable<ResumenAlertas> {
    return this.api.postConParametros<ResumenAlertas>(
      '/alertas/recalcular', { almacenId: almacenId ?? null });
  }
}
