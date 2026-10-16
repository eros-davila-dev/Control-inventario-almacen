import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/guards/permiso.guard';

export const INVENTARIO_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'stock' },
  {
    path: 'stock',
    title: 'Saldos de inventario · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['inventario:stock:leer'] },
    loadComponent: () => import('./stock.component').then((m) => m.StockComponent),
  },
  {
    path: 'movimientos',
    title: 'Movimientos · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['inventario:stock:leer'] },
    loadComponent: () => import('./movimientos.component').then((m) => m.MovimientosComponent),
  },
  {
    path: 'lotes',
    title: 'Lotes y vencimientos · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['inventario:lote:leer'] },
    loadComponent: () => import('./lotes.component').then((m) => m.LotesComponent),
  },
  {
    path: 'kardex',
    title: 'Kardex valorizado · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['reportes:kardex:leer'] },
    loadComponent: () => import('./kardex.component').then((m) => m.KardexComponent),
  },
  {
    path: 'transferencias',
    title: 'Transferencias · SGAI-CID',
    canActivate: [permisoGuard],
    data: { permisos: ['inventario:stock:leer'] },
    loadComponent: () =>
      import('./transferencias.component').then((m) => m.TransferenciasComponent),
  },
];
