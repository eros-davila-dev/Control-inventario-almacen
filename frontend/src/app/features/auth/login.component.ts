import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { ProblemDetail } from '../../core/models/api.model';
import { ThemeService } from '../../core/services/theme.service';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';

/**
 * Pantalla de ingreso (HU-01). Una vez autenticado, la sesion se renueva de
 * forma transparente mientras el refresh token siga vigente, por lo que el
 * usuario no vuelve a pasar por aqui durante su turno.
 */
@Component({
  selector: 'sgai-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, UiButtonComponent, RouterLink],
  template: `
    <div class="grid min-h-dvh lg:grid-cols-[1.1fr_1fr]">
      <!-- Panel de identidad: solo en pantallas grandes -->
      <section class="relative hidden overflow-hidden bg-brand-700 p-10 text-white lg:flex
                      lg:flex-col lg:justify-between">
        <div class="absolute inset-0 opacity-30"
             style="background:radial-gradient(60% 60% at 20% 20%, #5eead4 0%, transparent 60%),
                    radial-gradient(50% 50% at 90% 80%, #0ea5e9 0%, transparent 65%)"
             aria-hidden="true"></div>
        <div class="relative">
          <span class="inline-flex size-12 items-center justify-center rounded-xl bg-white/15
                       text-lg font-bold backdrop-blur">SG</span>
          <h1 class="mt-8 max-w-md text-3xl font-semibold leading-tight">
            Control de almacen de insumos de hemodialisis
          </h1>
          <p class="mt-4 max-w-md text-sm leading-relaxed text-white/80">
            Trazabilidad por lote, vencimientos bajo control, reposicion a tiempo y kardex
            valorizado en linea para las sedes de Corporacion Integral de Dialisis S.A.C.
          </p>
        </div>
        <ul class="relative space-y-3 text-sm text-white/90">
          @for (punto of beneficios; track punto) {
            <li class="flex items-start gap-2.5">
              <span class="mt-1.5 size-1.5 shrink-0 rounded-full bg-teal-300" aria-hidden="true"></span>
              {{ punto }}
            </li>
          }
        </ul>
        <p class="relative text-xs text-white/60">
          SGAI-CID v1.0 · Departamento de Sistemas
        </p>
      </section>

      <!-- Formulario -->
      <section class="flex flex-col justify-center px-5 py-10 sm:px-10">
        <div class="mx-auto w-full max-w-sm animate-[aparecer_.4s_ease-out]">
          <div class="mb-8 flex items-center justify-between">
            <div class="flex items-center gap-2.5 lg:hidden">
              <span class="flex size-10 items-center justify-center rounded-xl bg-brand-600
                           text-sm font-bold text-white">SG</span>
              <span>
                <span class="block text-sm font-semibold leading-tight text-fg">SGAI-CID</span>
                <span class="block text-xs leading-tight text-fg-muted">Almacen e inventarios</span>
              </span>
            </div>
            <button type="button" class="ml-auto rounded-lg p-2 text-fg-muted hover:bg-surface-muted"
                    [attr.aria-label]="tema.tema() === 'dark' ? 'Tema claro' : 'Tema oscuro'"
                    (click)="tema.alternar()">
              @if (tema.tema() === 'dark') {
                <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="1.6" stroke-linecap="round">
                  <circle cx="12" cy="12" r="4" />
                  <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5M17.5 17.5 19 19M19 5l-1.5 1.5M6.5 17.5 5 19" />
                </svg>
              } @else {
                <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="1.6" stroke-linecap="round">
                  <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />
                </svg>
              }
            </button>
          </div>

          <h2 class="text-2xl font-semibold tracking-tight text-fg">Iniciar sesion</h2>
          <p class="mt-1.5 text-sm text-fg-muted">
            Use su usuario institucional. Si es su primer acceso, el sistema le pedira cambiar
            la contrasena temporal.
          </p>

          <form class="mt-7 space-y-4" [formGroup]="formulario" (ngSubmit)="ingresar()">
            <label class="block">
              <span class="sgai-label">Usuario o correo</span>
              <input class="sgai-input" type="text" formControlName="usuario"
                     autocomplete="username" autocapitalize="none" spellcheck="false"
                     placeholder="almacenero.central"
                     [attr.aria-invalid]="invalido('usuario')" />
              @if (invalido('usuario')) {
                <span class="mt-1 block text-xs font-medium text-danger">
                  Ingrese su usuario o correo institucional.
                </span>
              }
            </label>

            <label class="block">
              <span class="sgai-label">Contrasena</span>
              <span class="relative block">
                <input class="sgai-input pr-11" [type]="verPassword() ? 'text' : 'password'"
                       formControlName="password" autocomplete="current-password"
                       placeholder="••••••••••"
                       [attr.aria-invalid]="invalido('password')" />
                <button type="button"
                        class="absolute right-1 top-1 rounded-lg p-2 text-fg-muted
                               hover:bg-surface-muted"
                        [attr.aria-label]="verPassword() ? 'Ocultar contrasena' : 'Mostrar contrasena'"
                        (click)="verPassword.set(!verPassword())">
                  <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.6" stroke-linecap="round">
                    @if (verPassword()) {
                      <path d="M3 3l18 18M10.6 10.6A2 2 0 0 0 12 14a2 2 0 0 0 1.4-.6M9.9 5.2A9.5 9.5 0 0 1 12 5c5 0 9 4.5 9 7 0 .8-.6 2-1.7 3.2M6.5 6.9C4.4 8.4 3 10.5 3 12c0 2.5 4 7 9 7 1 0 2-.2 2.8-.5" />
                    } @else {
                      <path d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7Z" />
                      <circle cx="12" cy="12" r="2.5" />
                    }
                  </svg>
                </button>
              </span>
              @if (invalido('password')) {
                <span class="mt-1 block text-xs font-medium text-danger">
                  Ingrese su contrasena.
                </span>
              }
            </label>

            @if (mensajeError()) {
              <div class="rounded-lg border border-danger/30 bg-danger-soft px-3 py-2.5
                          text-sm text-danger" role="alert">
                {{ mensajeError() }}
              </div>
            }

            <ui-button tipo="submit" [bloque]="true" tamano="lg" [cargando]="enviando()">
              Ingresar
            </ui-button>
          </form>

          <p class="mt-6 text-xs leading-relaxed text-fg-subtle">
            El acceso es personal y queda registrado en la pista de auditoria. Si olvido su
            contrasena, solicite el reinicio al Departamento de Sistemas.
          </p>
          <a routerLink="/login" class="sr-only">Recargar</a>
        </div>
      </section>
    </div>
  `,
  styles: [`
    @keyframes aparecer {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: none; }
    }
  `],
})
export class LoginComponent {
  protected readonly tema = inject(ThemeService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly beneficios = [
    'Saldo en linea por producto, lote, vencimiento y ubicacion.',
    'Despacho con FEFO: primero el lote que vence antes.',
    'Alertas de reposicion y de vencimiento por tramos.',
    'Kardex valorizado y trazabilidad para inspeccion sanitaria.',
  ];

  protected readonly formulario = this.fb.nonNullable.group({
    usuario: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
  });

  protected readonly enviando = signal(false);
  protected readonly verPassword = signal(false);
  protected readonly mensajeError = signal('');
  protected readonly puedeEnviar = computed(() => !this.enviando());

  constructor() {
    if (this.auth.autenticado()) {
      void this.router.navigate(['/dashboard']);
    }
  }

  protected invalido(campo: 'usuario' | 'password'): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.dirty || control.touched);
  }

  protected ingresar(): void {
    this.mensajeError.set('');
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.auth.login(this.formulario.getRawValue()).subscribe({
      next: (respuesta) => {
        this.enviando.set(false);
        const destino = respuesta.usuario.debeCambiarPassword
          ? '/perfil/cambiar-password'
          : this.retorno();
        void this.router.navigateByUrl(destino);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.mensajeError.set(this.traducir(error));
      },
    });
  }

  /** Vuelve a la pantalla que el usuario intentaba abrir antes de la sesion. */
  private retorno(): string {
    const parametros = new URLSearchParams(window.location.search);
    const retorno = parametros.get('retorno');
    return retorno && retorno.startsWith('/') ? retorno : '/dashboard';
  }

  private traducir(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo conectar con el servidor. Intente nuevamente.';
    }
    const problema = (error.error ?? {}) as ProblemDetail;
    switch (problema.codigo) {
      case 'CUENTA_BLOQUEADA':
        return problema.detail
          ?? 'Cuenta bloqueada temporalmente por intentos fallidos.';
      case 'CUENTA_INACTIVA':
        return 'Su cuenta esta inactiva. Comuniquese con el Departamento de Sistemas.';
      case 'DEMASIADAS_PETICIONES':
        return 'Demasiados intentos desde esta red. Espere un minuto.';
      case 'CREDENCIALES_INVALIDAS':
        return 'Usuario o contrasena incorrectos.';
      default:
        return error.status === 0
          ? 'Sin conexion con el servidor.'
          : 'No se pudo iniciar sesion. Intente nuevamente.';
    }
  }
}
