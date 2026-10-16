import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'light' | 'dark';

/**
 * Tema claro/oscuro. Respeta la preferencia del sistema hasta que el usuario
 * elige explicitamente, y persiste la eleccion entre sesiones.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private static readonly CLAVE = 'sgai.tema';

  private readonly _tema = signal<Tema>(this.temaInicial());
  readonly tema = this._tema.asReadonly();

  constructor() {
    effect(() => {
      const tema = this._tema();
      const raiz = document.documentElement;
      raiz.classList.toggle('dark', tema === 'dark');
      raiz.dataset['theme'] = tema;
      try {
        localStorage.setItem(ThemeService.CLAVE, tema);
      } catch {
        /* sin persistencia disponible */
      }
    });

    // Si el usuario no ha elegido, se sigue la preferencia del sistema en vivo.
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (evento) => {
      if (!this.hayPreferenciaGuardada()) {
        this._tema.set(evento.matches ? 'dark' : 'light');
      }
    });
  }

  alternar(): void {
    this._tema.update((actual) => (actual === 'dark' ? 'light' : 'dark'));
  }

  establecer(tema: Tema): void {
    this._tema.set(tema);
  }

  private temaInicial(): Tema {
    const guardado = this.leerPreferencia();
    if (guardado === 'dark' || guardado === 'light') {
      return guardado;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private hayPreferenciaGuardada(): boolean {
    return this.leerPreferencia() !== null;
  }

  private leerPreferencia(): string | null {
    try {
      return localStorage.getItem(ThemeService.CLAVE);
    } catch {
      return null;
    }
  }
}
