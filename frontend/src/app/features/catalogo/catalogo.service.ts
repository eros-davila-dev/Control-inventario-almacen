import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import {
  Categoria,
  CategoriaRequest,
  Producto,
  ProductoRequest,
  Proveedor,
  ProveedorRequest,
  UnidadMedida,
  UnidadMedidaRequest,
} from '../../core/models/catalogo.model';
import { BaseCrudService } from '../../shared/services/base-crud.service';

@Injectable({ providedIn: 'root' })
export class ProductoService extends BaseCrudService<Producto, ProductoRequest> {
  protected readonly ruta = '/productos';

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Producto>> {
    return this.api.getPage<Producto>(this.ruta, parametros);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Producto> {
    return this.api.patch<Producto>(`${this.ruta}/${id}/estado`, {}, { activo });
  }
}

@Injectable({ providedIn: 'root' })
export class CategoriaService extends BaseCrudService<Categoria, CategoriaRequest> {
  protected readonly ruta = '/categorias';

  activas(): Observable<Categoria[]> {
    return this.api.get<Categoria[]>(`${this.ruta}/activas`);
  }
}

@Injectable({ providedIn: 'root' })
export class UnidadMedidaService extends BaseCrudService<UnidadMedida, UnidadMedidaRequest> {
  protected readonly ruta = '/unidades-medida';

  activas(): Observable<UnidadMedida[]> {
    return this.api.get<UnidadMedida[]>(`${this.ruta}/activas`);
  }
}

@Injectable({ providedIn: 'root' })
export class ProveedorService extends BaseCrudService<Proveedor, ProveedorRequest> {
  protected readonly ruta = '/proveedores';

  buscar(parametros: ParametrosPagina): Observable<PageResponse<Proveedor>> {
    return this.api.getPage<Proveedor>(this.ruta, parametros);
  }
}
