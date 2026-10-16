import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Campo de texto / numero / fecha con etiqueta, ayuda y error.
 * Implementa ControlValueAccessor para integrarse con Reactive Forms tipados.
 */
@Component({
  selector: 'ui-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UiInputComponent),
    multi: true,
  }],
  template: `
    <label class="block">
      @if (etiqueta()) {
        <span class="sgai-label">
          {{ etiqueta() }}
          @if (requerido()) {
            <span class="text-danger" aria-hidden="true">*</span>
          }
        </span>
      }
      <input
        class="sgai-input"
        [type]="tipo()"
        [value]="valor() ?? ''"
        [attr.placeholder]="marcador() || null"
        [attr.step]="paso() || null"
        [attr.min]="minimo() ?? null"
        [attr.max]="maximo() ?? null"
        [attr.maxlength]="longitudMaxima() ?? null"
        [attr.inputmode]="modoEntrada() || null"
        [attr.autocomplete]="autocompletado() || null"
        [attr.aria-invalid]="!!error()"
        [attr.aria-describedby]="error() ? idError : null"
        [disabled]="deshabilitado()"
        [readOnly]="soloLectura()"
        (input)="alEscribir($event)"
        (blur)="alSalir()" />
      @if (error()) {
        <span [id]="idError" class="mt-1 block text-xs font-medium text-danger">{{ error() }}</span>
      } @else if (ayuda()) {
        <span class="mt-1 block text-xs text-fg-subtle">{{ ayuda() }}</span>
      }
    </label>
  `,
  host: { class: 'block' },
})
export class UiInputComponent implements ControlValueAccessor {
  readonly etiqueta = input('');
  readonly tipo = input<'text' | 'number' | 'date' | 'password' | 'email' | 'tel' | 'search'>('text');
  readonly marcador = input('');
  readonly ayuda = input('');
  readonly error = input('');
  readonly requerido = input(false);
  readonly soloLectura = input(false);
  readonly paso = input<string | number>('');
  readonly minimo = input<number | string | null>(null);
  readonly maximo = input<number | string | null>(null);
  readonly longitudMaxima = input<number | null>(null);
  readonly modoEntrada = input('');
  readonly autocompletado = input('');

  protected readonly idError = `error-${Math.random().toString(36).slice(2, 9)}`;
  protected readonly valor = signal<string | number | null>(null);
  protected readonly deshabilitado = signal(false);

  private alCambiar: (valor: string | number | null) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  writeValue(valor: string | number | null): void {
    this.valor.set(valor ?? null);
  }

  registerOnChange(fn: (valor: string | number | null) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
  }

  protected alEscribir(evento: Event): void {
    const bruto = (evento.target as HTMLInputElement).value;
    const valor = this.tipo() === 'number' ? (bruto === '' ? null : Number(bruto)) : bruto;
    this.valor.set(valor);
    this.alCambiar(valor);
  }

  protected alSalir(): void {
    this.alTocar();
  }
}
