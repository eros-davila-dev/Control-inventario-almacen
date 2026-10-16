import { Signal, computed, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';

/**
 * Estado reutilizable de una tabla paginada: filtros, pagina, orden y carga.
 * Cada pantalla de listado solo aporta la funcion que consulta al servidor, de
 * modo que la paginacion y el ordenamiento se comportan igual en todo el sistema.
 */
export class ListaPaginada<T> {
  private readonly _parametros = signal<ParametrosPagina>({ page: 0, size: 20 });
  private readonly _pagina = signal<PageResponse<T> | null>(null);
  private readonly _cargando = signal(false);

  readonly parametros = this._parametros.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly datos: Signal<readonly T[]> = computed(() => this._pagina()?.contenido ?? []);
  readonly total = computed(() => this._pagina()?.totalElementos ?? 0);
  readonly totalPaginas = computed(() => this._pagina()?.totalPaginas ?? 0);
  readonly paginaActual = computed(() => this._parametros().page ?? 0);
  readonly tamano = computed(() => this._parametros().size ?? 20);
  readonly orden = computed(() => String(this._parametros().sort ?? ''));

  constructor(
    private readonly cargador: (parametros: ParametrosPagina) => Observable<PageResponse<T>>,
    parametrosIniciales: ParametrosPagina = {},
  ) {
    this._parametros.update((actual) => ({ ...actual, ...parametrosIniciales }));
  }

  recargar(): void {
    this._cargando.set(true);
    this.cargador(this._parametros()).subscribe({
      next: (pagina) => {
        this._pagina.set(pagina);
        this._cargando.set(false);
      },
      error: () => this._cargando.set(false),
    });
  }

  /** Aplica filtros y vuelve a la primera pagina. */
  filtrar(filtros: Partial<ParametrosPagina>): void {
    this._parametros.update((actual) => ({ ...actual, ...filtros, page: 0 }));
    this.recargar();
  }

  irAPagina(pagina: number): void {
    this._parametros.update((actual) => ({ ...actual, page: pagina }));
    this.recargar();
  }

  cambiarTamano(size: number): void {
    this._parametros.update((actual) => ({ ...actual, size, page: 0 }));
    this.recargar();
  }

  ordenarPor(sort: string): void {
    this._parametros.update((actual) => ({ ...actual, sort }));
    this.recargar();
  }
}
