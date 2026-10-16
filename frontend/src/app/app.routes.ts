import { Routes } from '@angular/router';
import { autenticadoGuard } from './core/guards/autenticado.guard';
import { cambioPasswordGuard } from './core/guards/cambio-password.guard';
import { permisoGuard } from './core/guards/permiso.guard';

/**
 * Rutas de la aplicacion. Cada feature se carga de forma perezosa y declara los
 * permisos que exige; el guard de permisos los verifica antes de activar la ruta.
 */
export const routes: Routes = [
  {
    path: 'login',
    title: 'Ingresar · SGAI-CID',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [autenticadoGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Tablero · SGAI-CID',
        canActivate: [cambioPasswordGuard, permisoGuard],
        data: { permisos: ['reportes:dashboard:leer'] },
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'perfil',
        loadChildren: () => import('./features/perfil/perfil.routes').then((m) => m.PERFIL_ROUTES),
      },
      {
        path: 'inventario',
        canActivate: [cambioPasswordGuard],
        loadChildren: () =>
          import('./features/inventario/inventario.routes').then((m) => m.INVENTARIO_ROUTES),
      },
      {
        path: 'despacho',
        canActivate: [cambioPasswordGuard],
        loadChildren: () =>
          import('./features/despacho/despacho.routes').then((m) => m.DESPACHO_ROUTES),
      },
      {
        path: 'compras',
        canActivate: [cambioPasswordGuard],
        loadChildren: () => import('./features/compras/compras.routes').then((m) => m.COMPRAS_ROUTES),
      },
      {
        path: 'conteo',
        canActivate: [cambioPasswordGuard],
        loadChildren: () => import('./features/conteo/conteo.routes').then((m) => m.CONTEO_ROUTES),
      },
      {
        path: 'catalogo',
        canActivate: [cambioPasswordGuard],
        loadChildren: () =>
          import('./features/catalogo/catalogo.routes').then((m) => m.CATALOGO_ROUTES),
      },
      {
        path: 'almacenes',
        canActivate: [cambioPasswordGuard],
        loadChildren: () =>
          import('./features/almacenes/almacenes.routes').then((m) => m.ALMACENES_ROUTES),
      },
      {
        path: 'seguridad',
        canActivate: [cambioPasswordGuard],
        loadChildren: () =>
          import('./features/seguridad/seguridad.routes').then((m) => m.SEGURIDAD_ROUTES),
      },
      {
        path: 'alertas',
        title: 'Alertas · SGAI-CID',
        canActivate: [cambioPasswordGuard, permisoGuard],
        data: { permisos: ['alertas:alerta:leer'] },
        loadComponent: () =>
          import('./features/alertas/alertas.component').then((m) => m.AlertasComponent),
      },
      {
        path: 'reportes',
        title: 'Reportes · SGAI-CID',
        canActivate: [cambioPasswordGuard, permisoGuard],
        data: { permisos: ['reportes:reporte:leer'] },
        loadComponent: () =>
          import('./features/reportes/reportes.component').then((m) => m.ReportesComponent),
      },
      {
        path: 'auditoria',
        title: 'Auditoria · SGAI-CID',
        canActivate: [cambioPasswordGuard, permisoGuard],
        data: { permisos: ['auditoria:log:leer'] },
        loadComponent: () =>
          import('./features/auditoria/auditoria.component').then((m) => m.AuditoriaComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
