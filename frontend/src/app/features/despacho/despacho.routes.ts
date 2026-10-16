import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const DESPACHO_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'requisiciones' },
  {
    path: 'requisiciones',
    title: 'Requisiciones · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['despacho:requisicion:leer'] },
    loadComponent: () => import('./requisiciones.component').then((m) => m.RequisicionesComponent),
  },
  {
    path: 'despachos',
    title: 'Despachos · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['despacho:despacho:leer'] },
    loadComponent: () => import('./despachos.component').then((m) => m.DespachosComponent),
  },
];
