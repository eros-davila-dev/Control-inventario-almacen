import { Injectable, inject, signal } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { Opcion } from '../../core/models/api.model';
import { Almacen, Ubicacion } from '../../core/models/almacen.model';
import {
  Categoria,
  ClasificacionAbc,
  CondicionAlmacenamiento,
  ProductoResumen,
  Proveedor,
  TipoProducto,
  UnidadMedida,
} from '../../core/models/catalogo.model';

/**
 * Catalogos de referencia compartidos por los formularios (productos, unidades,
 * categorias, proveedores, almacenes). Se cachean en memoria porque cambian poco
 * y los usan casi todas las pantallas.
 */
@Injectable({ providedIn: 'root' })
export class CatalogoRefService {
  private readonly api = inject(ApiService);

  private productos$?: Observable<ProductoResumen[]>;
  private categorias$?: Observable<Categoria[]>;
  private unidades$?: Observable<UnidadMedida[]>;
  private proveedores$?: Observable<Proveedor[]>;

  readonly productos = signal<ProductoResumen[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly unidades = signal<UnidadMedida[]>([]);
  readonly proveedores = signal<Proveedor[]>([]);

  productosResumen(): Observable<ProductoResumen[]> {
    this.productos$ ??= this.api.get<ProductoResumen[]>('/productos/resumen').pipe(
      tap((lista) => this.productos.set(lista)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.productos$;
  }

  categoriasActivas(): Observable<Categoria[]> {
    this.categorias$ ??= this.api.get<Categoria[]>('/categorias/activas').pipe(
      tap((lista) => this.categorias.set(lista)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.categorias$;
  }

  unidadesActivas(): Observable<UnidadMedida[]> {
    this.unidades$ ??= this.api.get<UnidadMedida[]>('/unidades-medida/activas').pipe(
      tap((lista) => this.unidades.set(lista)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.unidades$;
  }

  proveedoresActivos(): Observable<Proveedor[]> {
    this.proveedores$ ??= this.api.get<Proveedor[]>('/proveedores/activos').pipe(
      tap((lista) => this.proveedores.set(lista)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.proveedores$;
  }

  ubicacionesDe(almacenId: number): Observable<Ubicacion[]> {
    return this.api.get<Ubicacion[]>(`/ubicaciones/almacen/${almacenId}`);
  }

  almacenesAutorizados(): Observable<Almacen[]> {
    return this.api.get<Almacen[]>('/almacenes/autorizados');
  }

  /** Invalida la cache tras modificar un maestro. */
  invalidar(): void {
    this.productos$ = undefined;
    this.categorias$ = undefined;
    this.unidades$ = undefined;
    this.proveedores$ = undefined;
  }

  // ------------------------------------------------------------------
  // Opciones de enumerados (texto en espanol para la interfaz)
  // ------------------------------------------------------------------

  readonly tiposProducto: Opcion<TipoProducto>[] = [
    { valor: 'INSUMO_DIALISIS', etiqueta: 'Insumo de dialisis' },
    { valor: 'MEDICAMENTO', etiqueta: 'Medicamento' },
    { valor: 'MATERIAL_MEDICO', etiqueta: 'Material medico' },
    { valor: 'REACTIVO', etiqueta: 'Reactivo de laboratorio' },
    { valor: 'EPP', etiqueta: 'Equipo de proteccion personal' },
    { valor: 'REPUESTO_MAQUINA', etiqueta: 'Repuesto de maquina' },
    { valor: 'LIMPIEZA', etiqueta: 'Limpieza y desinfeccion' },
    { valor: 'OFICINA', etiqueta: 'Utiles de oficina' },
  ];

  readonly condicionesAlmacenamiento: Opcion<CondicionAlmacenamiento>[] = [
    { valor: 'AMBIENTE', etiqueta: 'Temperatura ambiente (15-30 C)' },
    { valor: 'REFRIGERADO_2_8', etiqueta: 'Refrigerado (2-8 C)' },
    { valor: 'CONGELADO', etiqueta: 'Congelado (-20 C)' },
    { valor: 'PROTEGIDO_LUZ', etiqueta: 'Protegido de la luz' },
  ];

  readonly clasificacionesAbc: Opcion<ClasificacionAbc>[] = [
    { valor: 'A', etiqueta: 'A - alto valor de consumo' },
    { valor: 'B', etiqueta: 'B - valor intermedio' },
    { valor: 'C', etiqueta: 'C - bajo valor de consumo' },
  ];

  /** Convierte cualquier catalogo en opciones para ui-select. */
  aOpciones<T extends { id: number }>(
    lista: readonly T[], etiqueta: (item: T) => string): Opcion<number>[] {
    return lista.map((item) => ({ valor: item.id, etiqueta: etiqueta(item) }));
  }
}
