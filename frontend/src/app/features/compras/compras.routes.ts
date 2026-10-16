import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const COMPRAS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'requerimientos' },
  {
    path: 'requerimientos',
    title: 'Requerimientos de compra · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['compras:requerimiento:leer'] },
    loadComponent: () =>
      import('./requerimientos.component').then((m) => m.RequerimientosComponent),
  },
  {
    path: 'ordenes',
    title: 'Ordenes de compra · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['compras:orden:leer'] },
    loadComponent: () => import('./ordenes.component').then((m) => m.OrdenesComponent),
  },
  {
    path: 'recepciones',
    title: 'Recepciones · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['compras:recepcion:leer'] },
    loadComponent: () => import('./recepciones.component').then((m) => m.RecepcionesComponent),
  },
];
