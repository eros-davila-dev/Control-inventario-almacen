/** Definicion del menu: cada entrada declara el permiso que la habilita. */
export interface ItemNavegacion {
  ruta: string;
  etiqueta: string;
  icono: string;
  permisos?: string[];
  /** Se muestra en la navegacion inferior de movil. */
  enMovil?: boolean;
}

export interface GrupoNavegacion {
  titulo: string;
  items: ItemNavegacion[];
}

/** Iconos como rutas SVG (sin dependencias externas de iconografia). */
export const ICONOS: Record<string, string> = {
  tablero: 'M4 13h6V4H4v9Zm0 7h6v-5H4v5Zm10 0h6v-9h-6v9Zm0-16v5h6V4h-6Z',
  stock: 'M3 7.5 12 3l9 4.5v9L12 21l-9-4.5v-9Zm0 0L12 12m0 0 9-4.5M12 12v9',
  movimientos: 'M3 7h13l-3-3m3 3-3 3M21 17H8l3-3m-3 3 3 3',
  lote: 'M20 7 12 3 4 7v10l8 4 8-4V7Zm-8 4v10m0-10L4 7m8 4 8-4',
  kardex: 'M4 4h16v16H4V4Zm0 5h16M9 9v11',
  transferencia: 'M7 8h10M7 8l3-3M7 8l3 3m7 5H7m10 0-3-3m3 3-3 3',
  requisicion: 'M9 5h6m-6 0a2 2 0 0 0-2 2v12h10V7a2 2 0 0 0-2-2M9 5V4h6v1m-6 6h6m-6 4h4',
  despacho: 'M3 7h11v8H3V7Zm11 3h4l3 3v2h-7V10ZM6 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm11 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z',
  compras: 'M4 5h2l2 10h10l2-7H7M9 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2Zm9 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z',
  orden: 'M7 3h10l3 4v14H4V7l3-4Zm-3 4h16M9 12h6m-6 4h6',
  recepcion: 'M12 3v10m0 0 4-4m-4 4-4-4M4 17h16v4H4v-4Z',
  conteo: 'M5 4h14v16H5V4Zm3 4h8M8 12h8M8 16h4',
  alerta: 'M12 4a6 6 0 0 0-6 6v4l-2 3h16l-2-3v-4a6 6 0 0 0-6-6Zm-2 16a2 2 0 0 0 4 0',
  reporte: 'M5 20V9m7 11V4m7 16v-7',
  producto: 'M20 7 12 3 4 7v10l8 4 8-4V7ZM8 5.5l8 4',
  categoria: 'M4 6h7v6H4V6Zm9 0h7v4h-7V6ZM4 14h7v4H4v-4Zm9-2h7v6h-7v-6Z',
  unidad: 'M4 9h16M4 15h16M9 4v16',
  proveedor: 'M4 7h9v10H4V7Zm9 4h4l3 3v3h-7v-6ZM7 20a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z',
  almacen: 'M3 10 12 4l9 6v10H3V10Zm6 10v-6h6v6',
  ubicacion: 'M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11Zm0-8.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z',
  usuario: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-8 8a8 8 0 0 1 16 0',
  rol: 'M12 3 4 6v6c0 4.4 3.4 8.3 8 9 4.6-.7 8-4.6 8-9V6l-8-3Zm0 7v4',
  auditoria: 'M4 5h16v14H4V5Zm3 4h10M7 12h10M7 15h6',
  perfil: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-8 8a8 8 0 0 1 16 0',
};

export const NAVEGACION: GrupoNavegacion[] = [
  {
    titulo: 'Gestion',
    items: [
      {
        ruta: '/dashboard', etiqueta: 'Tablero', icono: 'tablero',
        permisos: ['reportes:dashboard:leer'], enMovil: true,
      },
      {
        ruta: '/alertas', etiqueta: 'Alertas', icono: 'alerta',
        permisos: ['alertas:alerta:leer'], enMovil: true,
      },
      {
        ruta: '/reportes', etiqueta: 'Reportes', icono: 'reporte',
        permisos: ['reportes:reporte:leer'],
      },
    ],
  },
  {
    titulo: 'Inventario',
    items: [
      {
        ruta: '/inventario/stock', etiqueta: 'Saldos', icono: 'stock',
        permisos: ['inventario:stock:leer'], enMovil: true,
      },
      {
        ruta: '/inventario/movimientos', etiqueta: 'Movimientos', icono: 'movimientos',
        permisos: ['inventario:stock:leer'],
      },
      {
        ruta: '/inventario/lotes', etiqueta: 'Lotes y vencimientos', icono: 'lote',
        permisos: ['inventario:lote:leer'],
      },
      {
        ruta: '/inventario/kardex', etiqueta: 'Kardex valorizado', icono: 'kardex',
        permisos: ['reportes:kardex:leer'],
      },
      {
        ruta: '/inventario/transferencias', etiqueta: 'Transferencias', icono: 'transferencia',
        permisos: ['inventario:stock:leer'],
      },
    ],
  },
  {
    titulo: 'Atencion a sedes',
    items: [
      {
        ruta: '/despacho/requisiciones', etiqueta: 'Requisiciones', icono: 'requisicion',
        permisos: ['despacho:requisicion:leer'], enMovil: true,
      },
      {
        ruta: '/despacho/despachos', etiqueta: 'Despachos', icono: 'despacho',
        permisos: ['despacho:despacho:leer'],
      },
    ],
  },
  {
    titulo: 'Abastecimiento',
    items: [
      {
        ruta: '/compras/requerimientos', etiqueta: 'Requerimientos', icono: 'compras',
        permisos: ['compras:requerimiento:leer'],
      },
      {
        ruta: '/compras/ordenes', etiqueta: 'Ordenes de compra', icono: 'orden',
        permisos: ['compras:orden:leer'],
      },
      {
        ruta: '/compras/recepciones', etiqueta: 'Recepciones', icono: 'recepcion',
        permisos: ['compras:recepcion:leer'],
      },
    ],
  },
  {
    titulo: 'Control',
    items: [
      {
        ruta: '/conteo/tomas', etiqueta: 'Inventario fisico', icono: 'conteo',
        permisos: ['conteo:toma:leer'],
      },
      {
        ruta: '/auditoria', etiqueta: 'Auditoria', icono: 'auditoria',
        permisos: ['auditoria:log:leer'],
      },
    ],
  },
  {
    titulo: 'Maestros',
    items: [
      {
        ruta: '/catalogo/productos', etiqueta: 'Productos', icono: 'producto',
        permisos: ['catalogo:producto:leer'],
      },
      {
        ruta: '/catalogo/categorias', etiqueta: 'Categorias', icono: 'categoria',
        permisos: ['catalogo:producto:leer'],
      },
      {
        ruta: '/catalogo/unidades', etiqueta: 'Unidades de medida', icono: 'unidad',
        permisos: ['catalogo:producto:leer'],
      },
      {
        ruta: '/catalogo/proveedores', etiqueta: 'Proveedores', icono: 'proveedor',
        permisos: ['catalogo:proveedor:leer'],
      },
      {
        ruta: '/almacenes', etiqueta: 'Almacenes', icono: 'almacen',
        permisos: ['almacen:almacen:leer'],
      },
      {
        ruta: '/almacenes/ubicaciones', etiqueta: 'Ubicaciones', icono: 'ubicacion',
        permisos: ['almacen:almacen:leer'],
      },
    ],
  },
  {
    titulo: 'Administracion',
    items: [
      {
        ruta: '/seguridad/usuarios', etiqueta: 'Usuarios', icono: 'usuario',
        permisos: ['seguridad:usuario:leer'],
      },
      {
        ruta: '/seguridad/roles', etiqueta: 'Roles y permisos', icono: 'rol',
        permisos: ['seguridad:rol:leer'],
      },
    ],
  },
];
