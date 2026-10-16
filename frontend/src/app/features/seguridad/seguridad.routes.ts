import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const SEGURIDAD_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'usuarios' },
  {
    path: 'usuarios',
    title: 'Usuarios · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['seguridad:usuario:leer'] },
    loadComponent: () => import('./usuarios.component').then((m) => m.UsuariosComponent),
  },
  {
    path: 'roles',
    title: 'Roles y permisos · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['seguridad:rol:leer'] },
    loadComponent: () => import('./roles.component').then((m) => m.RolesComponent),
  },
];
