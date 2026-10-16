import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Casilla de verificacion con etiqueta y descripcion. */
@Component({
  selector: 'ui-checkbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UiCheckboxComponent),
    multi: true,
  }],
  template: `
    <label class="flex cursor-pointer items-start gap-2.5 py-1 select-none">
      <input
        type="checkbox"
        class="mt-0.5 size-4 shrink-0 rounded border-border-strong text-brand-600
               accent-brand-600 focus:ring-2 focus:ring-brand-500/30"
        [checked]="valor()"
        [disabled]="deshabilitado()"
        (change)="alMarcar($event)" />
      <span class="min-w-0">
        <span class="block text-sm font-medium text-fg">{{ etiqueta() }}</span>
        @if (descripcion()) {
          <span class="block text-xs text-fg-muted">{{ descripcion() }}</span>
        }
      </span>
    </label>
  `,
  host: { class: 'block' },
})
export class UiCheckboxComponent implements ControlValueAccessor {
  readonly etiqueta = input.required<string>();
  readonly descripcion = input('');

  protected readonly valor = signal(false);
  protected readonly deshabilitado = signal(false);

  private alCambiar: (valor: boolean) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  writeValue(valor: boolean): void {
    this.valor.set(!!valor);
  }

  registerOnChange(fn: (valor: boolean) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
  }

  protected alMarcar(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).checked;
    this.valor.set(valor);
    this.alCambiar(valor);
    this.alTocar();
  }
}
