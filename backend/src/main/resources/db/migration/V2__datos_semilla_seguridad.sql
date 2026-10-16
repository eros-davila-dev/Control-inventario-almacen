-- =====================================================================
-- V2: datos semilla de seguridad.
-- Catalogo de permisos (sincronizado con pe.cid.sgai.seguridad.domain.Permisos),
-- roles del sistema segun la matriz de docs/02-REQUERIMIENTOS.md y usuarios
-- iniciales. Todos los usuarios nacen con cambio de contrasena obligatorio.
-- =====================================================================

set search_path to sgai;

-- ---------------------------------------------------------------------
-- Permisos
-- ---------------------------------------------------------------------
insert into sgai.permiso (codigo, modulo, descripcion, created_at, created_by) values
    ('seguridad:usuario:leer', 'seguridad', 'Consultar usuarios', now(), 'migracion'),
    ('seguridad:usuario:escribir', 'seguridad', 'Crear, editar, resetear y desbloquear usuarios', now(), 'migracion'),
    ('seguridad:rol:leer', 'seguridad', 'Consultar roles y permisos', now(), 'migracion'),
    ('seguridad:rol:escribir', 'seguridad', 'Crear y editar roles y sus permisos', now(), 'migracion'),
    ('auditoria:log:leer', 'auditoria', 'Consultar la pista de auditoria', now(), 'migracion'),
    ('catalogo:producto:leer', 'catalogo', 'Consultar el catalogo de productos', now(), 'migracion'),
    ('catalogo:producto:escribir', 'catalogo', 'Crear y editar productos', now(), 'migracion'),
    ('catalogo:categoria:escribir', 'catalogo', 'Administrar categorias del catalogo', now(), 'migracion'),
    ('catalogo:unidad:escribir', 'catalogo', 'Administrar unidades de medida', now(), 'migracion'),
    ('catalogo:proveedor:leer', 'catalogo', 'Consultar proveedores', now(), 'migracion'),
    ('catalogo:proveedor:escribir', 'catalogo', 'Crear y editar proveedores', now(), 'migracion'),
    ('almacen:almacen:leer', 'almacen', 'Consultar almacenes', now(), 'migracion'),
    ('almacen:almacen:escribir', 'almacen', 'Crear y editar almacenes', now(), 'migracion'),
    ('almacen:ubicacion:escribir', 'almacen', 'Administrar ubicaciones de almacen', now(), 'migracion'),
    ('inventario:stock:leer', 'inventario', 'Consultar saldos y movimientos', now(), 'migracion'),
    ('inventario:movimiento:registrar', 'inventario', 'Registrar ingresos, salidas y ajustes', now(), 'migracion'),
    ('inventario:movimiento:anular', 'inventario', 'Anular movimientos con movimiento inverso', now(), 'migracion'),
    ('inventario:lote:leer', 'inventario', 'Consultar lotes y su trazabilidad', now(), 'migracion'),
    ('inventario:lote:escribir', 'inventario', 'Registrar lotes del fabricante', now(), 'migracion'),
    ('inventario:lote:liberar', 'inventario', 'Liberar lotes de cuarentena (quimico farmaceutico)', now(), 'migracion'),
    ('inventario:lote:retener', 'inventario', 'Retener o rechazar lotes por alerta sanitaria', now(), 'migracion'),
    ('inventario:transferencia:registrar', 'inventario', 'Enviar transferencias entre almacenes', now(), 'migracion'),
    ('inventario:transferencia:recibir', 'inventario', 'Confirmar la recepcion de transferencias', now(), 'migracion'),
    ('compras:requerimiento:leer', 'compras', 'Consultar requerimientos de compra', now(), 'migracion'),
    ('compras:requerimiento:crear', 'compras', 'Crear y enviar requerimientos de compra', now(), 'migracion'),
    ('compras:requerimiento:aprobar', 'compras', 'Aprobar o rechazar requerimientos', now(), 'migracion'),
    ('compras:orden:leer', 'compras', 'Consultar ordenes de compra', now(), 'migracion'),
    ('compras:orden:crear', 'compras', 'Emitir ordenes de compra', now(), 'migracion'),
    ('compras:orden:aprobar', 'compras', 'Aprobar, rechazar o anular ordenes de compra', now(), 'migracion'),
    ('compras:recepcion:leer', 'compras', 'Consultar recepciones de mercaderia', now(), 'migracion'),
    ('compras:recepcion:registrar', 'compras', 'Registrar recepciones con control de calidad', now(), 'migracion'),
    ('despacho:requisicion:leer', 'despacho', 'Consultar requisiciones internas', now(), 'migracion'),
    ('despacho:requisicion:crear', 'despacho', 'Crear y enviar requisiciones internas', now(), 'migracion'),
    ('despacho:requisicion:atender', 'despacho', 'Atender requisiciones y despachar con FEFO', now(), 'migracion'),
    ('despacho:despacho:leer', 'despacho', 'Consultar despachos', now(), 'migracion'),
    ('conteo:toma:leer', 'conteo', 'Consultar tomas de inventario', now(), 'migracion'),
    ('conteo:toma:programar', 'conteo', 'Programar y anular tomas de inventario', now(), 'migracion'),
    ('conteo:toma:ejecutar', 'conteo', 'Ejecutar el conteo fisico', now(), 'migracion'),
    ('conteo:toma:aprobar', 'conteo', 'Aprobar diferencias y generar ajustes', now(), 'migracion'),
    ('alertas:alerta:leer', 'alertas', 'Consultar y gestionar alertas', now(), 'migracion'),
    ('reportes:kardex:leer', 'reportes', 'Consultar y exportar el kardex valorizado', now(), 'migracion'),
    ('reportes:reporte:leer', 'reportes', 'Consultar y exportar reportes de gestion', now(), 'migracion'),
    ('reportes:dashboard:leer', 'reportes', 'Consultar el tablero de indicadores', now(), 'migracion');

-- ---------------------------------------------------------------------
-- Roles del sistema (es_sistema = true: no se pueden eliminar)
-- ---------------------------------------------------------------------
insert into sgai.rol (nombre, descripcion, es_sistema, activo, created_at, created_by) values
    ('ADMIN', 'Administrador del sistema (Departamento de Sistemas)', true, true, now(), 'migracion'),
    ('JEFE_ALMACEN', 'Jefe de almacen: administra maestros, aprueba y controla', true, true, now(), 'migracion'),
    ('ALMACENERO', 'Operador de almacen de su sede', true, true, now(), 'migracion'),
    ('QUIMICO_FARMACEUTICO', 'Quimico farmaceutico: libera, retiene y controla vencimientos', true, true, now(), 'migracion'),
    ('COMPRAS', 'Abastecimiento: requerimientos y ordenes de compra', true, true, now(), 'migracion'),
    ('SOLICITANTE', 'Enfermeria y coordinacion de sede: solicita insumos', true, true, now(), 'migracion'),
    ('AUDITOR', 'Auditoria interna y contabilidad: solo lectura', true, true, now(), 'migracion');

-- ---------------------------------------------------------------------
-- Asignacion de permisos por rol
-- ---------------------------------------------------------------------
-- ADMIN recibe el catalogo completo de permisos
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r cross join sgai.permiso p where r.nombre = 'ADMIN';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'alertas:alerta:leer',
        'almacen:almacen:escribir',
        'almacen:almacen:leer',
        'almacen:ubicacion:escribir',
        'auditoria:log:leer',
        'catalogo:categoria:escribir',
        'catalogo:producto:escribir',
        'catalogo:producto:leer',
        'catalogo:proveedor:escribir',
        'catalogo:proveedor:leer',
        'catalogo:unidad:escribir',
        'compras:orden:aprobar',
        'compras:orden:crear',
        'compras:orden:leer',
        'compras:recepcion:leer',
        'compras:recepcion:registrar',
        'compras:requerimiento:aprobar',
        'compras:requerimiento:crear',
        'compras:requerimiento:leer',
        'conteo:toma:aprobar',
        'conteo:toma:ejecutar',
        'conteo:toma:leer',
        'conteo:toma:programar',
        'despacho:despacho:leer',
        'despacho:requisicion:atender',
        'despacho:requisicion:crear',
        'despacho:requisicion:leer',
        'inventario:lote:escribir',
        'inventario:lote:leer',
        'inventario:lote:retener',
        'inventario:movimiento:anular',
        'inventario:movimiento:registrar',
        'inventario:stock:leer',
        'inventario:transferencia:recibir',
        'inventario:transferencia:registrar',
        'reportes:dashboard:leer',
        'reportes:kardex:leer',
        'reportes:reporte:leer',
        'seguridad:rol:leer')
where r.nombre = 'JEFE_ALMACEN';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'alertas:alerta:leer',
        'almacen:almacen:leer',
        'catalogo:producto:leer',
        'catalogo:proveedor:leer',
        'compras:orden:leer',
        'compras:recepcion:leer',
        'compras:recepcion:registrar',
        'compras:requerimiento:crear',
        'compras:requerimiento:leer',
        'conteo:toma:ejecutar',
        'conteo:toma:leer',
        'despacho:despacho:leer',
        'despacho:requisicion:atender',
        'despacho:requisicion:crear',
        'despacho:requisicion:leer',
        'inventario:lote:escribir',
        'inventario:lote:leer',
        'inventario:movimiento:registrar',
        'inventario:stock:leer',
        'inventario:transferencia:recibir',
        'inventario:transferencia:registrar',
        'reportes:dashboard:leer',
        'reportes:kardex:leer',
        'reportes:reporte:leer')
where r.nombre = 'ALMACENERO';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'alertas:alerta:leer',
        'almacen:almacen:leer',
        'catalogo:producto:leer',
        'inventario:lote:leer',
        'inventario:lote:liberar',
        'inventario:lote:retener',
        'inventario:stock:leer',
        'reportes:dashboard:leer',
        'reportes:kardex:leer',
        'reportes:reporte:leer')
where r.nombre = 'QUIMICO_FARMACEUTICO';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'alertas:alerta:leer',
        'almacen:almacen:leer',
        'catalogo:producto:leer',
        'catalogo:proveedor:escribir',
        'catalogo:proveedor:leer',
        'compras:orden:crear',
        'compras:orden:leer',
        'compras:recepcion:leer',
        'compras:requerimiento:crear',
        'compras:requerimiento:leer',
        'inventario:stock:leer',
        'reportes:dashboard:leer',
        'reportes:reporte:leer')
where r.nombre = 'COMPRAS';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'almacen:almacen:leer',
        'catalogo:producto:leer',
        'despacho:despacho:leer',
        'despacho:requisicion:crear',
        'despacho:requisicion:leer',
        'inventario:stock:leer')
where r.nombre = 'SOLICITANTE';
insert into sgai.rol_permiso (rol_id, permiso_id)
select r.id, p.id from sgai.rol r join sgai.permiso p on p.codigo in (
        'alertas:alerta:leer',
        'almacen:almacen:leer',
        'auditoria:log:leer',
        'catalogo:producto:leer',
        'catalogo:proveedor:leer',
        'compras:orden:leer',
        'compras:recepcion:leer',
        'compras:requerimiento:leer',
        'conteo:toma:leer',
        'despacho:despacho:leer',
        'despacho:requisicion:leer',
        'inventario:lote:leer',
        'inventario:stock:leer',
        'reportes:dashboard:leer',
        'reportes:kardex:leer',
        'reportes:reporte:leer',
        'seguridad:rol:leer',
        'seguridad:usuario:leer')
where r.nombre = 'AUDITOR';

-- ---------------------------------------------------------------------
-- Usuarios iniciales
-- Contrasenas temporales (deben cambiarse en el primer acceso):
--   admin                -> Sgai#2026Admin
--   jefe.almacen         -> Almacen#2026
--   almacenero.central   -> Almacen#2026
--   almacenero.sjl       -> Almacen#2026
--   qf.farmacia          -> Clinica#2026
--   compras              -> Clinica#2026
--   enfermeria.sjl       -> Clinica#2026
--   auditoria            -> Clinica#2026
-- ---------------------------------------------------------------------
insert into sgai.usuario (username, email, password_hash, nombres, apellidos, cargo, telefono,
                          activo, debe_cambiar_password, intentos_fallidos, created_at, created_by)
values
    ('admin', 'sistemas@nipromed.com', '$2a$12$Pu0uKWNwZJIcht/Ff/auXukT6lqc1RQmK3GCs88B9ONmlOgpyP7z2', 'Administrador', 'del Sistema', 'Jefe de TI', '942111005', true, true, 0, now(), 'migracion'),
    ('jefe.almacen', 'jefe.almacen@nipromed.com', '$2a$12$XJ6LBBT4cmlPlswM/6RHMe01zegIp7jrrQ9opPaRFn9sp4jZ/jnLC', 'Rosa Maria', 'Quispe Loayza', 'Jefe de Almacen', '942111006', true, true, 0, now(), 'migracion'),
    ('almacenero.central', 'almacen.central@nipromed.com', '$2a$12$XJ6LBBT4cmlPlswM/6RHMe01zegIp7jrrQ9opPaRFn9sp4jZ/jnLC', 'Carlos Alberto', 'Mendoza Rios', 'Almacenero', '942111007', true, true, 0, now(), 'migracion'),
    ('almacenero.sjl', 'almacen.sjl@nipromed.com', '$2a$12$XJ6LBBT4cmlPlswM/6RHMe01zegIp7jrrQ9opPaRFn9sp4jZ/jnLC', 'Luis Fernando', 'Huaman Ccopa', 'Almacenero de sede', '942111008', true, true, 0, now(), 'migracion'),
    ('qf.farmacia', 'qf@nipromed.com', '$2a$12$WhOtSt4spjytXrXLeixOHOKaUbZQtL/0KWFg2rX6Bjoh/wpqIc1J.', 'Ana Lucia', 'Ramirez Vega', 'Quimico Farmaceutico', '942111009', true, true, 0, now(), 'migracion'),
    ('compras', 'compras@nipromed.com', '$2a$12$WhOtSt4spjytXrXLeixOHOKaUbZQtL/0KWFg2rX6Bjoh/wpqIc1J.', 'Jorge Enrique', 'Salas Pinto', 'Coordinador de Compras', '942111010', true, true, 0, now(), 'migracion'),
    ('enfermeria.sjl', 'enfermeria.sjl@nipromed.com', '$2a$12$WhOtSt4spjytXrXLeixOHOKaUbZQtL/0KWFg2rX6Bjoh/wpqIc1J.', 'Miriam', 'Torres Aguilar', 'Coordinadora de Enfermeria', '942111011', true, true, 0, now(), 'migracion'),
    ('auditoria', 'auditoria@nipromed.com', '$2a$12$WhOtSt4spjytXrXLeixOHOKaUbZQtL/0KWFg2rX6Bjoh/wpqIc1J.', 'Patricia', 'Núñez Carrion', 'Auditor Interno', '942111012', true, true, 0, now(), 'migracion');
-- Asignacion de roles
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'admin' and r.nombre = 'ADMIN';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'jefe.almacen' and r.nombre = 'JEFE_ALMACEN';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'almacenero.central' and r.nombre = 'ALMACENERO';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'almacenero.sjl' and r.nombre = 'ALMACENERO';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'qf.farmacia' and r.nombre = 'QUIMICO_FARMACEUTICO';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'compras' and r.nombre = 'COMPRAS';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'enfermeria.sjl' and r.nombre = 'SOLICITANTE';
insert into sgai.usuario_rol (usuario_id, rol_id) select u.id, r.id from sgai.usuario u, sgai.rol r where u.username = 'auditoria' and r.nombre = 'AUDITOR';

-- El historico de contrasenas arranca con la clave temporal, de modo que la
-- politica de no reutilizacion aplique desde el primer cambio.
insert into sgai.password_historico (usuario_id, password_hash, fecha)
select u.id, u.password_hash, now() from sgai.usuario u;
