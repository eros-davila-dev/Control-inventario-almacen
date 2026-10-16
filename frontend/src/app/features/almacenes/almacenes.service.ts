import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import { Almacen, AlmacenRequest, Ubicacion, UbicacionRequest } from '../../core/models/almacen.model';
import { BaseCrudService } from '../../shared/services/base-crud.service';

@Injectable({ providedIn: 'root' })
export class AlmacenService extends BaseCrudService<Almacen, AlmacenRequest> {
  protected readonly ruta = '/almacenes';

  activos(): Observable<Almacen[]> {
    return this.api.get<Almacen[]>(`${this.ruta}/activos`);
  }

  autorizados(): Observable<Almacen[]> {
    return this.api.get<Almacen[]>(`${this.ruta}/autorizados`);
  }
}

@Injectable({ providedIn: 'root' })
export class UbicacionService extends BaseCrudService<Ubicacion, UbicacionRequest> {
  protected readonly ruta = '/ubicaciones';

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Ubicacion>> {
    return this.api.getPage<Ubicacion>(this.ruta, parametros);
  }

  porAlmacen(almacenId: number): Observable<Ubicacion[]> {
    return this.api.get<Ubicacion[]>(`${this.ruta}/almacen/${almacenId}`);
  }
}
