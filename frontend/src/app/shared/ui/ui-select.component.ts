import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Opcion } from '../../core/models/api.model';

/** Selector de opciones con etiqueta y error, integrado con Reactive Forms. */
@Component({
  selector: 'ui-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UiSelectComponent),
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
      <select
        class="sgai-input appearance-none bg-[length:1rem] pr-9"
        [value]="valor() ?? ''"
        [disabled]="deshabilitado()"
        [attr.aria-invalid]="!!error()"
        (change)="alSeleccionar($event)"
        (blur)="alSalir()">
        @if (marcador()) {
          <option value="">{{ marcador() }}</option>
        }
        @for (opcion of opciones(); track opcion.valor) {
          <option [value]="opcion.valor">{{ opcion.etiqueta }}</option>
        }
      </select>
      @if (error()) {
        <span class="mt-1 block text-xs font-medium text-danger">{{ error() }}</span>
      } @else if (ayuda()) {
        <span class="mt-1 block text-xs text-fg-subtle">{{ ayuda() }}</span>
      }
    </label>
  `,
  host: { class: 'block' },
})
export class UiSelectComponent implements ControlValueAccessor {
  readonly etiqueta = input('');
  readonly opciones = input.required<Opcion<string | number>[]>();
  readonly marcador = input('Seleccione...');
  readonly ayuda = input('');
  readonly error = input('');
  readonly requerido = input(false);
  /** Convierte el valor a numero: util con identificadores de catalogo. */
  readonly numerico = input(false);

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

  protected alSeleccionar(evento: Event): void {
    const bruto = (evento.target as HTMLSelectElement).value;
    const valor = bruto === '' ? null : (this.numerico() ? Number(bruto) : bruto);
    this.valor.set(valor);
    this.alCambiar(valor);
  }

  protected alSalir(): void {
    this.alTocar();
  }
}
