import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Area de texto para observaciones y motivos. */
@Component({
  selector: 'ui-textarea',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UiTextareaComponent),
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
      <textarea
        class="sgai-input resize-y"
        [rows]="filas()"
        [value]="valor() ?? ''"
        [attr.placeholder]="marcador() || null"
        [attr.maxlength]="longitudMaxima() ?? null"
        [attr.aria-invalid]="!!error()"
        [disabled]="deshabilitado()"
        (input)="alEscribir($event)"
        (blur)="alSalir()"></textarea>
      @if (error()) {
        <span class="mt-1 block text-xs font-medium text-danger">{{ error() }}</span>
      } @else if (ayuda()) {
        <span class="mt-1 block text-xs text-fg-subtle">{{ ayuda() }}</span>
      }
    </label>
  `,
  host: { class: 'block' },
})
export class UiTextareaComponent implements ControlValueAccessor {
  readonly etiqueta = input('');
  readonly marcador = input('');
  readonly ayuda = input('');
  readonly error = input('');
  readonly requerido = input(false);
  readonly filas = input(3);
  readonly longitudMaxima = input<number | null>(500);

  protected readonly valor = signal<string | null>(null);
  protected readonly deshabilitado = signal(false);

  private alCambiar: (valor: string | null) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  writeValue(valor: string | null): void {
    this.valor.set(valor ?? null);
  }

  registerOnChange(fn: (valor: string | null) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
  }

  protected alEscribir(evento: Event): void {
    const valor = (evento.target as HTMLTextAreaElement).value;
    this.valor.set(valor);
    this.alCambiar(valor);
  }

  protected alSalir(): void {
    this.alTocar();
  }
}
