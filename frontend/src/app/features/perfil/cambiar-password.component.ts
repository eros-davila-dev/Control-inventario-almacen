import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';
import { UiCardComponent } from '../../shared/ui/ui-card.component';
import { UiPageHeaderComponent } from '../../shared/ui/ui-page-header.component';

/**
 * Cambio de contrasena (HU-03). Valida la politica en el cliente para dar
 * retroalimentacion inmediata; el servidor la vuelve a validar siempre.
 */
@Component({
  selector: 'sgai-cambiar-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiButtonComponent],
  template: `
    <ui-page-header titulo="Cambiar contrasena"
                    [descripcion]="auth.debeCambiarPassword()
                      ? 'Por seguridad debe definir una contrasena propia antes de usar el sistema.'
                      : 'Al cambiar la contrasena se cierran sus sesiones en otros dispositivos.'" />

    <div class="mx-auto max-w-xl">
      <ui-card titulo="Nueva contrasena">
        <form class="space-y-4" [formGroup]="formulario" (ngSubmit)="guardar()">
          <label class="block">
            <span class="sgai-label">Contrasena actual</span>
            <input class="sgai-input" type="password" formControlName="passwordActual"
                   autocomplete="current-password" />
          </label>

          <label class="block">
            <span class="sgai-label">Contrasena nueva</span>
            <input class="sgai-input" type="password" formControlName="passwordNueva"
                   autocomplete="new-password" />
          </label>

          <label class="block">
            <span class="sgai-label">Confirmar contrasena nueva</span>
            <input class="sgai-input" type="password" formControlName="confirmacion"
                   autocomplete="new-password" />
            @if (formulario.errors?.['noCoincide'] && formulario.controls.confirmacion.touched) {
              <span class="mt-1 block text-xs font-medium text-danger">
                La confirmacion no coincide con la contrasena nueva.
              </span>
            }
          </label>

          <div class="rounded-lg border border-border bg-surface-muted p-3">
            <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-fg-muted">
              Politica de contrasenas
            </p>
            <ul class="space-y-1 text-sm">
              @for (regla of reglas(); track regla.texto) {
                <li class="flex items-center gap-2"
                    [class.text-success]="regla.cumple" [class.text-fg-muted]="!regla.cumple">
                  <span class="font-mono text-xs" aria-hidden="true">
                    {{ regla.cumple ? '✓' : '○' }}
                  </span>
                  {{ regla.texto }}
                </li>
              }
            </ul>
          </div>

          <div class="flex justify-end gap-2">
            <ui-button tipo="submit" [cargando]="enviando()" [deshabilitado]="!todasCumplen()">
              Guardar contrasena
            </ui-button>
          </div>
        </form>
      </ui-card>
    </div>
  `,
})
export class CambiarPasswordComponent {
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly notificacion = inject(NotificacionService);

  protected readonly enviando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    passwordActual: ['', [Validators.required]],
    passwordNueva: ['', [Validators.required, Validators.minLength(10)]],
    confirmacion: ['', [Validators.required]],
  }, { validators: [(grupo: AbstractControl) => {
    const nueva = grupo.get('passwordNueva')?.value;
    const confirmacion = grupo.get('confirmacion')?.value;
    return nueva && confirmacion && nueva !== confirmacion ? { noCoincide: true } : null;
  }] });

  private readonly nueva = signal('');

  constructor() {
    this.formulario.controls.passwordNueva.valueChanges.subscribe((valor) =>
      this.nueva.set(valor ?? ''));
  }

  protected readonly reglas = computed(() => {
    const valor = this.nueva();
    return [
      { texto: 'Al menos 10 caracteres', cumple: valor.length >= 10 },
      { texto: 'Una letra mayuscula', cumple: /[A-Z]/.test(valor) },
      { texto: 'Una letra minuscula', cumple: /[a-z]/.test(valor) },
      { texto: 'Un numero', cumple: /\d/.test(valor) },
      { texto: 'Un simbolo (! @ # $ % & * ?)', cumple: /[^A-Za-z0-9]/.test(valor) },
      { texto: 'Distinta de sus 3 contrasenas anteriores', cumple: valor.length >= 10 },
    ];
  });

  protected readonly todasCumplen = computed(() => this.reglas().every((r) => r.cumple));

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.auth.cambiarPassword(this.formulario.getRawValue()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.notificacion.exito('Contrasena actualizada',
          'Vuelva a ingresar con su nueva contrasena.');
        void this.router.navigate(['/login']);
      },
      error: () => this.enviando.set(false),
    });
  }
}
