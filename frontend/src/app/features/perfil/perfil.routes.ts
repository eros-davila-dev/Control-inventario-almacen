import { Routes } from '@angular/router';

export const PERFIL_ROUTES: Routes = [
  {
    path: '',
    title: 'Mi perfil · SGAI-CID',
    loadComponent: () => import('./perfil.component').then((m) => m.PerfilComponent),
  },
  {
    path: 'cambiar-password',
    title: 'Cambiar contrasena · SGAI-CID',
    loadComponent: () =>
      import('./cambiar-password.component').then((m) => m.CambiarPasswordComponent),
  },
];
