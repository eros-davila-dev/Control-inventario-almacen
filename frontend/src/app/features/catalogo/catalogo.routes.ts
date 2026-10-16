import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const CATALOGO_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'productos' },
  {
    path: 'productos',
    title: 'Productos · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['catalogo:producto:leer'] },
    loadComponent: () => import('./productos.component').then((m) => m.ProductosComponent),
  },
  {
    path: 'categorias',
    title: 'Categorias · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['catalogo:producto:leer'] },
    loadComponent: () => import('./categorias.component').then((m) => m.CategoriasComponent),
  },
  {
    path: 'unidades',
    title: 'Unidades de medida · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['catalogo:producto:leer'] },
    loadComponent: () => import('./unidades.component').then((m) => m.UnidadesComponent),
  },
  {
    path: 'proveedores',
    title: 'Proveedores · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['catalogo:proveedor:leer'] },
    loadComponent: () => import('./proveedores.component').then((m) => m.ProveedoresComponent),
  },
];
