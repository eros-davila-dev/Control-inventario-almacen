import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Proveedor, ProveedorRequest } from '../../core/models/catalogo.model';
import { NotificacionService } from '../../core/services/notificacion.service';
import { HasPermisoDirective } from '../../shared/directives/has-permiso.directive';
import { ListaPaginada } from '../../shared/services/lista-paginada';
import {
  ColumnaTabla,
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent,
  UiCheckboxComponent,
  UiInputComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
} from '../../shared/ui';
import { ProveedorService } from './catalogo.service';

/** Maestro de proveedores (HU-10). El lead time alimenta la fecha esperada de las OC. */
@Component({
  selector: 'sgai-proveedores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, UiPageHeaderComponent, UiCardComponent, UiTableComponent,
    UiPaginationComponent, UiButtonComponent, UiBadgeComponent, UiModalComponent,
    UiInputComponent, UiCheckboxComponent, HasPermisoDirective,
  ],
  template: `
    <ui-page-header titulo="Proveedores"
                    descripcion="Datos comerciales y plazo de entrega usado en las ordenes de compra.">
      <div acciones>
        <span *hasPermiso="'catalogo:proveedor:escribir'">
          <ui-button (presionado)="abrirNuevo()">Nuevo proveedor</ui-button>
        </span>
      </div>
    </ui-page-header>

    <ui-card [sinRelleno]="true">
      <div class="border-b border-border px-4 py-3">
        <label class="block max-w-md">
          <span class="sr-only">Buscar proveedor</span>
          <input class="sgai-input" type="search" placeholder="RUC, razon social o nombre comercial..."
                 (input)="lista.filtrar({ texto: $any($event.target).value || null })" />
        </label>
      </div>
      <div class="px-4 py-3">
        <ui-table [columnas]="columnas" [datos]="lista.datos()" [cargando]="lista.cargando()"
                  [orden]="lista.orden()" (ordenCambiado)="lista.ordenarPor($event)">
          <ng-template #fila let-proveedor>
            <td class="px-3 py-2.5">
              <p class="font-medium text-fg">{{ proveedor.razonSocial }}</p>
              <p class="font-mono text-xs text-fg-muted">RUC {{ proveedor.ruc }}</p>
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted sm:table-cell">
              {{ proveedor.contacto || '—' }}
              @if (proveedor.telefono) {
                <span class="block text-xs">{{ proveedor.telefono }}</span>
              }
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted lg:table-cell">
              {{ proveedor.email || '—' }}
            </td>
            <td class="hidden px-3 py-2.5 text-fg-muted lg:table-cell">
              {{ proveedor.condicionPago || '—' }}
            </td>
            <td class="px-3 py-2.5 text-right tabular-nums">{{ proveedor.leadTimeDias }} d</td>
            <td class="px-3 py-2.5 text-center">
              <ui-badge [tono]="proveedor.activo ? 'exito' : 'neutral'">
                {{ proveedor.activo ? 'Activo' : 'Inactivo' }}
              </ui-badge>
            </td>
            <td class="px-3 py-2.5 text-right">
              <span *hasPermiso="'catalogo:proveedor:escribir'">
                <button type="button" class="rounded-lg px-2 py-1 text-xs font-medium text-fg-muted
                               hover:bg-surface-muted hover:text-fg"
                        (click)="abrirEdicion(proveedor)">Editar</button>
              </span>
            </td>
          </ng-template>
        </ui-table>
        <ui-pagination [pagina]="lista.paginaActual()" [tamano]="lista.tamano()"
                       [totalElementos]="lista.total()" [totalPaginas]="lista.totalPaginas()"
                       (paginaCambiada)="lista.irAPagina($event)"
                       (tamanoCambiado)="lista.cambiarTamano($event)" />
      </div>
    </ui-card>

    <ui-modal [abierto]="modalAbierto()" tamano="md"
              [titulo]="editando() ? 'Editar proveedor' : 'Nuevo proveedor'"
              (cerrado)="modalAbierto.set(false)">
      <form [formGroup]="formulario" class="grid gap-3 sm:grid-cols-2">
        <ui-input etiqueta="RUC" formControlName="ruc" [requerido]="true" modoEntrada="numeric"
                  [longitudMaxima]="11" [error]="errorRuc()" ayuda="11 digitos" />
        <ui-input etiqueta="Razon social" formControlName="razonSocial" [requerido]="true" />
        <ui-input etiqueta="Nombre comercial" formControlName="nombreComercial" />
        <ui-input etiqueta="Contacto" formControlName="contacto" />
        <ui-input etiqueta="Telefono" formControlName="telefono" tipo="tel" />
        <ui-input etiqueta="Correo" formControlName="email" tipo="email" />
        <ui-input etiqueta="Direccion" formControlName="direccion" class="sm:col-span-2" />
        <ui-input etiqueta="Condicion de pago" formControlName="condicionPago"
                  marcador="CREDITO 30 DIAS" />
        <ui-input etiqueta="Lead time (dias)" tipo="number" formControlName="leadTimeDias"
                  [minimo]="0" [maximo]="365" ayuda="Plazo habitual de entrega" />
        <ui-checkbox etiqueta="Proveedor activo" formControlName="activo" class="sm:col-span-2" />
      </form>
      <div pie class="flex gap-2">
        <ui-button variante="secundario" (presionado)="modalAbierto.set(false)">Cancelar</ui-button>
        <ui-button [cargando]="guardando()" (presionado)="guardar()">Guardar</ui-button>
      </div>
    </ui-modal>
  `,
})
export class ProveedoresComponent {
  private readonly servicio = inject(ProveedorService);
  private readonly notificacion = inject(NotificacionService);
  private readonly fb = inject(FormBuilder);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'razonSocial', titulo: 'Proveedor', ordenPor: 'razonSocial' },
    { clave: 'contacto', titulo: 'Contacto', ocultarEn: 'movil' },
    { clave: 'email', titulo: 'Correo', ocultarEn: 'tablet' },
    { clave: 'pago', titulo: 'Condicion de pago', ocultarEn: 'tablet' },
    { clave: 'lead', titulo: 'Lead time', alineacion: 'der' },
    { clave: 'estado', titulo: 'Estado', alineacion: 'centro' },
    { clave: 'acciones', titulo: '', alineacion: 'der' },
  ];

  protected readonly lista = new ListaPaginada<Proveedor>(
    (parametros) => this.servicio.buscar(parametros), { sort: 'razonSocial,asc' });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Proveedor | null>(null);
  protected readonly guardando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^[0-9]{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    nombreComercial: [''],
    contacto: [''],
    telefono: [''],
    email: ['', [Validators.email]],
    direccion: [''],
    condicionPago: [''],
    leadTimeDias: [7, [Validators.min(0), Validators.max(365)]],
    activo: [true],
  });

  constructor() {
    this.lista.recargar();
  }

  protected errorRuc(): string {
    const control = this.formulario.controls.ruc;
    return control.invalid && (control.dirty || control.touched)
      ? 'El RUC debe tener 11 digitos' : '';
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.formulario.reset({ leadTimeDias: 7, activo: true });
    this.modalAbierto.set(true);
  }

  protected abrirEdicion(proveedor: Proveedor): void {
    this.editando.set(proveedor);
    this.formulario.patchValue({
      ruc: proveedor.ruc,
      razonSocial: proveedor.razonSocial,
      nombreComercial: proveedor.nombreComercial ?? '',
      contacto: proveedor.contacto ?? '',
      telefono: proveedor.telefono ?? '',
      email: proveedor.email ?? '',
      direccion: proveedor.direccion ?? '',
      condicionPago: proveedor.condicionPago ?? '',
      leadTimeDias: proveedor.leadTimeDias,
      activo: proveedor.activo,
    });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const cuerpo: ProveedorRequest = {
      ...valores,
      nombreComercial: valores.nombreComercial || null,
      contacto: valores.contacto || null,
      telefono: valores.telefono || null,
      email: valores.email || null,
      direccion: valores.direccion || null,
      condicionPago: valores.condicionPago || null,
      leadTimeDias: Number(valores.leadTimeDias),
    };
    this.guardando.set(true);
    const editando = this.editando();
    const peticion = editando
      ? this.servicio.actualizar(editando.id, cuerpo)
      : this.servicio.crear(cuerpo);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.notificacion.exito('Proveedor guardado', cuerpo.razonSocial);
        this.lista.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }
}
