import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const ALMACENES_ROUTES: Routes = [
  {
    path: '',
    title: 'Almacenes · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['almacen:almacen:leer'] },
    loadComponent: () => import('./almacenes.component').then((m) => m.AlmacenesComponent),
  },
  {
    path: 'ubicaciones',
    title: 'Ubicaciones · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['almacen:almacen:leer'] },
    loadComponent: () => import('./ubicaciones.component').then((m) => m.UbicacionesComponent),
  },
];
