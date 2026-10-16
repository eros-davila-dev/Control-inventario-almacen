# Modelo de datos — SGAI-CID (PostgreSQL 17, esquema `sgai`)

Convenciones: `snake_case`; PK `bigint generated always as identity`; toda tabla lleva
`created_at`, `created_by`, `updated_at`, `updated_by`; borrado lógico con `activo boolean` en
maestros; cantidades `numeric(18,4)`; costos `numeric(18,6)`; importes `numeric(18,2)`;
enumerados como `varchar` + `check` (legibles en consultas y sin `ALTER TYPE` en migraciones).

## 1. Diagrama lógico (resumen)

```
                     ┌────────────┐      ┌─────────────┐
  rol ──< rol_permiso >── permiso  │      │ unidad_medida│
   │                                      └──────┬──────┘
   └──< usuario_rol >── usuario ──< usuario_almacen >── almacen ──< ubicacion
                           │                              │
                           │                              │
 categoria_producto ──< producto >────────── stock ────────┘
          │                 │  │               │
          │                 │  └──< lote >─────┘
          │                 │        │
          │                 │        └──< movimiento_linea >── movimiento ──> kardex
          │                 │
          └── proveedor ──< orden_compra >── orden_compra_linea
                 │              │
                 │              └──< recepcion >── recepcion_linea ──> lote
                 │
    requerimiento_compra ──< requerimiento_compra_linea
    requisicion ──< requisicion_linea >──  despacho ──< despacho_linea >── lote
    toma_inventario ──< toma_inventario_linea >── (ajuste) movimiento
    alerta >── producto/lote/almacen
    auditoria_log >── usuario
```

## 2. Seguridad y acceso

| Tabla | Columnas principales | Reglas |
|---|---|---|
| `usuario` | `id`, `username` **unq**, `email` **unq**, `password_hash`, `nombres`, `apellidos`, `dni`, `cargo`, `telefono`, `activo`, `debe_cambiar_password`, `intentos_fallidos`, `bloqueado_hasta`, `ultimo_acceso` | BCrypt coste 12; bloqueo tras 5 intentos / 15 min |
| `rol` | `id`, `nombre` **unq**, `descripcion`, `es_sistema` | `es_sistema` no eliminable |
| `permiso` | `id`, `codigo` **unq** (`modulo:recurso:accion`), `modulo`, `descripcion` | Catálogo fijo poblado por migración |
| `usuario_rol` | `usuario_id`, `rol_id` | PK compuesta |
| `rol_permiso` | `rol_id`, `permiso_id` | PK compuesta |
| `usuario_almacen` | `usuario_id`, `almacen_id` | Limita el alcance operativo |
| `refresh_token` | `id`, `usuario_id`, `token_hash` **unq**, `expira_en`, `revocado`, `ip`, `user_agent` | Se guarda el hash SHA-256, no el token |
| `intento_acceso` | `id`, `username`, `exitoso`, `ip`, `user_agent`, `fecha` | Soporte de bloqueo y análisis |
| `password_historico` | `usuario_id`, `password_hash`, `fecha` | Impide reutilizar las 3 últimas |
| `auditoria_log` | `id`, `usuario`, `accion`, `entidad`, `entidad_id`, `detalle jsonb`, `ip`, `fecha` | Solo inserción; índice por `(entidad, entidad_id)` y `fecha` |

## 3. Catálogo

| Tabla | Columnas principales | Reglas |
|---|---|---|
| `unidad_medida` | `codigo` **unq**, `nombre`, `simbolo`, `um_base_id`, `factor_a_base numeric(18,6)`, `activo` | `factor_a_base > 0`; UM base → `um_base_id` nulo y factor 1 |
| `categoria_producto` | `codigo` **unq**, `nombre`, `categoria_padre_id`, `requiere_cadena_frio`, `activo` | Sin ciclos; no se borra con hijos o productos |
| `producto` | `codigo` **unq**, `nombre`, `descripcion`, `categoria_id`, `unidad_medida_id`, `tipo_producto`, `presentacion`, `marca`, `registro_sanitario`, `vence_registro_sanitario`, `condicion_almacenamiento`, `controla_lote`, `controla_vencimiento`, `vida_util_minima_dias`, `clasificacion_abc`, `stock_minimo`, `stock_maximo`, `punto_reorden`, `dias_cobertura`, `costo_promedio numeric(18,6)`, `critico boolean`, `activo` | `stock_minimo ≤ punto_reorden ≤ stock_maximo`; medicamentos ⇒ `controla_lote` y `controla_vencimiento`; índice `GIN` sobre `nombre` sin acentos para búsqueda |
| `proveedor` | `ruc` **unq** (11 dígitos), `razon_social`, `nombre_comercial`, `contacto`, `telefono`, `email`, `direccion`, `condicion_pago`, `lead_time_dias`, `activo` | No se elimina con órdenes asociadas |

`tipo_producto`: `INSUMO_DIALISIS · MEDICAMENTO · MATERIAL_MEDICO · REACTIVO · EPP ·
REPUESTO_MAQUINA · LIMPIEZA · OFICINA`
`condicion_almacenamiento`: `AMBIENTE · REFRIGERADO_2_8 · CONGELADO · PROTEGIDO_LUZ`

## 4. Almacenes

| Tabla | Columnas principales | Reglas |
|---|---|---|
| `almacen` | `codigo` **unq**, `nombre`, `sede`, `direccion`, `tipo_almacen`, `responsable_id`, `permite_negativo` (false), `activo` | Un almacén `CUARENTENA` por sede como destino de recepciones observadas |
| `ubicacion` | `almacen_id`, `codigo` (**unq** por almacén), `descripcion`, `zona`, `rack`, `nivel`, `tipo_ubicacion`, `activo` | `tipo_ubicacion`: `PICKING · RESERVA · FRIO · CUARENTENA` |

## 5. Inventario (núcleo)

| Tabla | Columnas principales | Reglas |
|---|---|---|
| `lote` | `producto_id`, `codigo_lote`, `proveedor_id`, `fecha_fabricacion`, `fecha_vencimiento`, `estado_lote`, `motivo_estado`, `fecha_liberacion`, `liberado_por`, `costo_unitario`, `version` · **unq** `(producto_id, codigo_lote, proveedor_id)` | Estados: `CUARENTENA · LIBERADO · RETENIDO · RECHAZADO · VENCIDO`; solo `LIBERADO` despacha; índice por `fecha_vencimiento` |
| `stock` | `producto_id`, `almacen_id`, `lote_id` (null si no controla lote), `ubicacion_id`, `cantidad`, `cantidad_reservada`, `version` · **unq** `(producto_id, almacen_id, lote_id, ubicacion_id)` | `cantidad ≥ 0` (salvo `permite_negativo`); `cantidad_reservada ≤ cantidad`; bloqueo optimista |
| `movimiento` | `numero` **unq** (`ING-2026-00001`), `tipo_movimiento`, `fecha`, `almacen_origen_id`, `almacen_destino_id`, `estado_movimiento`, `motivo_ajuste`, `documento_tipo`, `documento_numero`, `proveedor_id`, `centro_costo`, `area_destino`, `observacion`, `movimiento_anulado_id`, `referencia_tipo`, `referencia_id`, `idempotency_key` **unq**, `usuario_id` | `tipo_movimiento`: `INGRESO_COMPRA · INGRESO_DEVOLUCION · INGRESO_AJUSTE · INGRESO_TRANSFERENCIA · SALIDA_DESPACHO · SALIDA_MERMA · SALIDA_VENCIMIENTO · SALIDA_AJUSTE · SALIDA_TRANSFERENCIA`; `estado_movimiento`: `REGISTRADO · ANULADO` |
| `movimiento_linea` | `movimiento_id`, `producto_id`, `lote_id`, `ubicacion_id`, `cantidad`, `costo_unitario`, `costo_total`, `cantidad_base` | `cantidad > 0`; el signo lo da el tipo de movimiento |
| `kardex` | `producto_id`, `almacen_id`, `movimiento_id`, `movimiento_linea_id`, `fecha`, `tipo_movimiento`, `lote_id`, `entrada`, `salida`, `saldo`, `costo_unitario`, `costo_promedio`, `valor_saldo` | **Append-only**; índice `(producto_id, almacen_id, fecha, id)`; candidato a partición por año |

## 6. Compras

| Tabla | Columnas principales |
|---|---|
| `requerimiento_compra` | `numero` **unq**, `almacen_id`, `fecha`, `estado` (`BORRADOR · ENVIADO · APROBADO · RECHAZADO · ATENDIDO · ANULADO`), `justificacion`, `solicitante_id`, `aprobador_id`, `fecha_aprobacion` |
| `requerimiento_compra_linea` | `requerimiento_id`, `producto_id`, `cantidad_solicitada`, `cantidad_sugerida`, `disponible_actual`, `observacion` |
| `orden_compra` | `numero` **unq** (`OC-2026-00001`), `proveedor_id`, `requerimiento_id`, `almacen_destino_id`, `fecha_emision`, `fecha_esperada`, `moneda`, `condicion_pago`, `subtotal`, `igv`, `total`, `estado` (`BORRADOR · APROBADA · RECHAZADA · RECIBIDA_PARCIAL · RECIBIDA · ANULADA`), `aprobador_id`, `fecha_aprobacion` |
| `orden_compra_linea` | `orden_id`, `producto_id`, `cantidad`, `cantidad_recibida`, `precio_unitario`, `importe` · `cantidad_recibida ≤ cantidad` |
| `recepcion` | `numero` **unq**, `orden_id`, `almacen_id`, `fecha`, `guia_remision`, `factura`, `transportista`, `temperatura_llegada`, `resultado_calidad` (`CONFORME · OBSERVADO · RECHAZADO`), `observacion`, `movimiento_id`, `recibido_por` |
| `recepcion_linea` | `recepcion_id`, `orden_linea_id`, `producto_id`, `codigo_lote`, `fecha_vencimiento`, `cantidad`, `precio_unitario`, `lote_id`, `conforme`, `observacion` |

## 7. Despacho

| Tabla | Columnas principales |
|---|---|
| `requisicion` | `numero` **unq** (`REQ-2026-00001`), `almacen_solicitante_id`, `almacen_atencion_id`, `centro_costo`, `area`, `prioridad` (`NORMAL · URGENTE`), `fecha`, `fecha_requerida`, `estado` (`BORRADOR · ENVIADA · EN_ATENCION · ATENDIDA_PARCIAL · ATENDIDA · ANULADA`), `solicitante_id`, `observacion` |
| `requisicion_linea` | `requisicion_id`, `producto_id`, `cantidad_solicitada`, `cantidad_atendida`, `observacion` · `cantidad_atendida ≤ cantidad_solicitada` |
| `despacho` | `numero` **unq** (`DES-2026-00001`), `requisicion_id`, `almacen_id`, `fecha`, `movimiento_id`, `estado` (`REGISTRADO · ANULADO`), `entregado_a`, `despachado_por`, `observacion` |
| `despacho_linea` | `despacho_id`, `requisicion_linea_id`, `producto_id`, `lote_id`, `cantidad`, `costo_unitario`, `fefo_respetado boolean`, `motivo_no_fefo` |

## 8. Conteo físico

| Tabla | Columnas principales |
|---|---|
| `toma_inventario` | `numero` **unq**, `almacen_id`, `alcance` (`TOTAL · POR_CATEGORIA · CICLICO_ABC`), `categoria_id`, `clasificacion_abc`, `fecha_programada`, `fecha_inicio`, `fecha_cierre`, `estado` (`PROGRAMADA · EN_PROCESO · CONTADA · APROBADA · ANULADA`), `responsable_id`, `aprobador_id`, `eri numeric(5,2)`, `observacion` |
| `toma_inventario_linea` | `toma_id`, `producto_id`, `lote_id`, `ubicacion_id`, `cantidad_teorica`, `cantidad_contada`, `segundo_conteo`, `diferencia`, `valor_diferencia`, `contado_por`, `fecha_conteo`, `observacion` |

## 9. Alertas

| Tabla | Columnas principales |
|---|---|
| `alerta` | `tipo_alerta` (`STOCK_BAJO · STOCK_AGOTADO · POR_VENCER · VENCIDO · LOTE_RETENIDO · SOBRE_STOCK · SIN_MOVIMIENTO`), `severidad` (`BAJA · MEDIA · ALTA · CRITICA`), `producto_id`, `almacen_id`, `lote_id`, `mensaje`, `valor_referencia`, `estado` (`ACTIVA · LEIDA · RESUELTA`), `fecha_generacion`, `fecha_resolucion` · **unq parcial** `(tipo_alerta, producto_id, almacen_id, lote_id)` donde `estado <> 'RESUELTA'` |

## 10. Vistas de reporte (migración `R__vistas.sql`)

| Vista | Contenido |
|---|---|
| `v_stock_disponible` | Stock por producto/almacén/lote con `disponible = cantidad − reservada` y bandera de lote despachable |
| `v_stock_producto_almacen` | Saldo consolidado por producto/almacén con mínimo, reorden, máximo y estado de reposición |
| `v_vencimientos` | Lotes con saldo, días a vencer y tramo (`VENCIDO · D30 · D60 · D90 · OK`) y valor expuesto |
| `v_consumo_mensual` | Salidas por producto/almacén/área/mes (cantidad y valor) |
| `v_valorizacion` | Valor de inventario por categoría y almacén |
| `v_rotacion` | Consumo de 90 días, cobertura en días y última fecha de movimiento |

## 11. Índices relevantes

```sql
create index ix_stock_prod_alm       on sgai.stock (producto_id, almacen_id);
create index ix_stock_lote           on sgai.stock (lote_id) where lote_id is not null;
create index ix_lote_vencimiento     on sgai.lote (fecha_vencimiento) where estado_lote <> 'RECHAZADO';
create index ix_kardex_consulta      on sgai.kardex (producto_id, almacen_id, fecha, id);
create index ix_mov_fecha_tipo       on sgai.movimiento (fecha desc, tipo_movimiento);
create index ix_mov_referencia       on sgai.movimiento (referencia_tipo, referencia_id);
create index ix_auditoria_entidad    on sgai.auditoria_log (entidad, entidad_id, fecha desc);
create index ix_producto_busqueda    on sgai.producto using gin (to_tsvector('spanish', nombre));
```

## 12. Reglas de integridad en base de datos

1. `check (cantidad >= 0)` en `stock` y `check (cantidad > 0)` en `movimiento_linea`.
2. `check (stock_minimo <= punto_reorden and punto_reorden <= stock_maximo)` en `producto`.
3. `check (cantidad_recibida <= cantidad)` en `orden_compra_linea`.
4. `check (cantidad_atendida <= cantidad_solicitada)` en `requisicion_linea`.
5. `check (fecha_vencimiento > fecha_fabricacion)` en `lote`.
6. `unique (producto_id, almacen_id, coalesce(lote_id,0), coalesce(ubicacion_id,0))` en `stock`
   mediante índice único sobre expresión.
7. `kardex` y `auditoria_log` sin `ON UPDATE`: la aplicación solo inserta.
