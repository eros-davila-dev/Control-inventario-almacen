import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const CONTEO_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tomas' },
  {
    path: 'tomas',
    title: 'Inventario fisico · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['conteo:toma:leer'] },
    loadComponent: () => import('./tomas.component').then((m) => m.TomasComponent),
  },
  {
    path: 'tomas/:id',
    title: 'Conteo fisico · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['conteo:toma:leer'] },
    loadComponent: () => import('./conteo-detalle.component').then((m) => m.ConteoDetalleComponent),
  },
];
