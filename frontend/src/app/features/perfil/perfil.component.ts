import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AlmacenContextoService } from '../../core/services/almacen-contexto.service';
import { FechaPipe } from '../../shared/pipes/fecha.pipe';
import { UiBadgeComponent } from '../../shared/ui/ui-badge.component';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';
import { UiCardComponent } from '../../shared/ui/ui-card.component';
import { UiPageHeaderComponent } from '../../shared/ui/ui-page-header.component';

/** Perfil del usuario: datos, roles, permisos efectivos y alcance de almacenes. */
@Component({
  selector: 'sgai-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiPageHeaderComponent, UiCardComponent, UiBadgeComponent, UiButtonComponent,
    FechaPipe,
  ],
  template: `
    <ui-page-header titulo="Mi perfil"
                    descripcion="Datos de su cuenta, roles asignados y permisos efectivos.">
      <div acciones>
        <a routerLink="/perfil/cambiar-password">
          <ui-button variante="secundario">Cambiar contrasena</ui-button>
        </a>
      </div>
    </ui-page-header>

    @let usuario = auth.usuario();
    @if (usuario) {
      <div class="grid gap-4 lg:grid-cols-3">
        <ui-card titulo="Datos de la cuenta" class="lg:col-span-2">
          <dl class="grid gap-x-6 gap-y-4 sm:grid-cols-2">
            @for (dato of datos(); track dato.etiqueta) {
              <div>
                <dt class="text-xs font-medium uppercase tracking-wide text-fg-muted">
                  {{ dato.etiqueta }}
                </dt>
                <dd class="mt-0.5 text-sm text-fg">{{ dato.valor }}</dd>
              </div>
            }
            <div>
              <dt class="text-xs font-medium uppercase tracking-wide text-fg-muted">Ultimo acceso</dt>
              <dd class="mt-0.5 text-sm text-fg">{{ usuario.ultimoAcceso | fechaSgai: 'hora' }}</dd>
            </div>
            <div>
              <dt class="text-xs font-medium uppercase tracking-wide text-fg-muted">Estado</dt>
              <dd class="mt-1">
                <ui-badge [tono]="usuario.activo ? 'exito' : 'peligro'">
                  {{ usuario.activo ? 'Activo' : 'Inactivo' }}
                </ui-badge>
              </dd>
            </div>
          </dl>
        </ui-card>

        <div class="space-y-4">
          <ui-card titulo="Roles">
            <div class="flex flex-wrap gap-2">
              @for (rol of usuario.roles; track rol) {
                <ui-badge tono="marca">{{ rol.replace('_', ' ') }}</ui-badge>
              }
            </div>
          </ui-card>

          <ui-card titulo="Almacenes autorizados"
                   [subtitulo]="usuario.almacenIds.length
                     ? 'Puede operar unicamente estos almacenes'
                     : 'Sin restriccion por almacen (vision global)'">
            <ul class="space-y-1 text-sm text-fg">
              @for (id of usuario.almacenIds; track id) {
                <li>· {{ contexto.nombreDe(id) }}</li>
              } @empty {
                <li class="text-fg-muted">Todos los almacenes de la red</li>
              }
            </ul>
          </ui-card>
        </div>

        <ui-card titulo="Permisos efectivos"
                 [subtitulo]="usuario.permisos.length + ' permiso(s) otorgados por sus roles'"
                 class="lg:col-span-3">
          <div class="flex flex-wrap gap-1.5">
            @for (permiso of usuario.permisos; track permiso) {
              <code class="rounded-md bg-surface-muted px-2 py-1 text-xs text-fg-muted">
                {{ permiso }}
              </code>
            }
          </div>
        </ui-card>
      </div>
    }
  `,
})
export class PerfilComponent {
  protected readonly auth = inject(AuthService);
  protected readonly contexto = inject(AlmacenContextoService);

  protected datos(): { etiqueta: string; valor: string }[] {
    const usuario = this.auth.usuario();
    if (!usuario) {
      return [];
    }
    return [
      { etiqueta: 'Nombre completo', valor: usuario.nombreCompleto },
      { etiqueta: 'Usuario', valor: usuario.username },
      { etiqueta: 'Correo', valor: usuario.email },
      { etiqueta: 'Cargo', valor: usuario.cargo ?? '-' },
      { etiqueta: 'Documento', valor: usuario.dni ?? '-' },
      { etiqueta: 'Telefono', valor: usuario.telefono ?? '-' },
    ];
  }
}
