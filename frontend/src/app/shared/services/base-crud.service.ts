import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';

/**
 * Servicio generico de maestros. Evita repetir el mismo CRUD en unidades de
 * medida, categorias, proveedores, almacenes y ubicaciones: cada servicio
 * concreto solo declara su ruta y, si lo necesita, agrega metodos propios.
 *
 * @template T   DTO de respuesta
 * @template REQ DTO de entrada
 */
export abstract class BaseCrudService<T, REQ> {
  protected readonly api = inject(ApiService);

  /** Ruta base del recurso, por ejemplo `/productos`. */
  protected abstract readonly ruta: string;

  listar(parametros?: ParametrosPagina): Observable<PageResponse<T>> {
    return this.api.getPage<T>(this.ruta, parametros);
  }

  obtener(id: number): Observable<T> {
    return this.api.get<T>(`${this.ruta}/${id}`);
  }

  crear(cuerpo: REQ): Observable<T> {
    return this.api.post<T>(this.ruta, cuerpo);
  }

  actualizar(id: number, cuerpo: REQ): Observable<T> {
    return this.api.put<T>(`${this.ruta}/${id}`, cuerpo);
  }

  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`${this.ruta}/${id}`);
  }
}
