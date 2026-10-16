import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from '../http/api.service';
import { Almacen } from '../models/almacen.model';

/**
 * Almacen de trabajo seleccionado en el encabezado. Toda pantalla operativa
 * (stock, movimientos, despacho, conteo) filtra por este contexto, que ademas
 * solo ofrece los almacenes que el usuario tiene autorizados (HU-14).
 */
@Injectable({ providedIn: 'root' })
export class AlmacenContextoService {
  private static readonly CLAVE = 'sgai.almacen';

  private readonly api = inject(ApiService);
  private readonly _almacenes = signal<Almacen[]>([]);
  private readonly _seleccionadoId = signal<number | null>(this.leerPreferencia());

  readonly almacenes = this._almacenes.asReadonly();
  readonly seleccionadoId = this._seleccionadoId.asReadonly();
  readonly seleccionado = computed(() =>
    this._almacenes().find((a) => a.id === this._seleccionadoId()) ?? null);
  readonly despachables = computed(() => this._almacenes().filter((a) => a.despachable));

  cargar(): void {
    this.api.get<Almacen[]>('/almacenes/autorizados').subscribe((almacenes) => {
      this._almacenes.set(almacenes);
      const actual = this._seleccionadoId();
      if (!actual || !almacenes.some((a) => a.id === actual)) {
        this.seleccionar(almacenes[0]?.id ?? null);
      }
    });
  }

  seleccionar(id: number | null): void {
    this._seleccionadoId.set(id);
    try {
      if (id === null) {
        localStorage.removeItem(AlmacenContextoService.CLAVE);
      } else {
        localStorage.setItem(AlmacenContextoService.CLAVE, String(id));
      }
    } catch {
      /* sin persistencia disponible */
    }
  }

  nombreDe(id: number | null | undefined): string {
    return this._almacenes().find((a) => a.id === id)?.nombre ?? '-';
  }

  private leerPreferencia(): number | null {
    try {
      const valor = localStorage.getItem(AlmacenContextoService.CLAVE);
      return valor ? Number(valor) : null;
    } catch {
      return null;
    }
  }
}
