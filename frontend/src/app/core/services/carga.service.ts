import { Injectable, computed, signal } from '@angular/core';

/** Contador de peticiones en vuelo para la barra de progreso global. */
@Injectable({ providedIn: 'root' })
export class CargaService {
  private readonly peticiones = signal(0);
  readonly cargando = computed(() => this.peticiones() > 0);

  iniciar(): void {
    this.peticiones.update((n) => n + 1);
  }

  terminar(): void {
    this.peticiones.update((n) => Math.max(0, n - 1));
  }
}
