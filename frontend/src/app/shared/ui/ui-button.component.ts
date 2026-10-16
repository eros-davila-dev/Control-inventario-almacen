import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

export type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'fantasma' | 'exito';
export type TamanoBoton = 'sm' | 'md' | 'lg';

/** Boton base de la aplicacion: unico lugar donde se definen variantes y tamanos. */
@Component({
  selector: 'ui-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="tipo()"
      [disabled]="deshabilitado() || cargando()"
      [attr.aria-busy]="cargando()"
      [attr.title]="titulo() || null"
      [class]="clases()"
      (click)="presionado.emit($event)">
      @if (cargando()) {
        <span class="size-4 animate-spin rounded-full border-2 border-current border-t-transparent"
              aria-hidden="true"></span>
      }
      <ng-content />
    </button>
  `,
  host: { class: 'contents' },
})
export class UiButtonComponent {
  readonly variante = input<VarianteBoton>('primario');
  readonly tamano = input<TamanoBoton>('md');
  readonly tipo = input<'button' | 'submit' | 'reset'>('button');
  readonly deshabilitado = input(false);
  readonly cargando = input(false);
  readonly bloque = input(false);
  readonly titulo = input<string>('');
  readonly presionado = output<MouseEvent>();

  private static readonly VARIANTES: Record<VarianteBoton, string> = {
    primario: 'bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 shadow-sm',
    secundario: 'bg-surface text-fg border border-border hover:bg-surface-muted active:bg-surface-muted',
    peligro: 'bg-danger text-white hover:brightness-110 active:brightness-95 shadow-sm',
    exito: 'bg-success text-white hover:brightness-110 active:brightness-95 shadow-sm',
    fantasma: 'bg-transparent text-fg-muted hover:bg-surface-muted hover:text-fg',
  };

  private static readonly TAMANOS: Record<TamanoBoton, string> = {
    sm: 'h-8 px-3 text-xs gap-1.5',
    md: 'h-10 px-4 text-sm gap-2',
    lg: 'h-12 px-6 text-base gap-2',
  };

  protected readonly clases = computed(() => [
    'inline-flex items-center justify-center rounded-lg font-medium transition-colors',
    'disabled:opacity-50 disabled:cursor-not-allowed select-none',
    UiButtonComponent.VARIANTES[this.variante()],
    UiButtonComponent.TAMANOS[this.tamano()],
    this.bloque() ? 'w-full' : '',
  ].join(' '));
}
