# Requerimientos — SGAI-CID

Trazabilidad: cada historia (HU) indica el problema que resuelve (P1–P9 del plan), el módulo y los
permisos que exige. `RF` = requerimiento funcional, `RNF` = no funcional.

## 1. Actores y matriz de roles

| Rol | Descripción | Alcance de datos |
|---|---|---|
| `ADMIN` | Administrador del sistema (TI) | Todos los almacenes |
| `JEFE_ALMACEN` | Jefe de almacén | Todos los almacenes |
| `ALMACENERO` | Operador de almacén de su sede | Almacenes asignados |
| `QUIMICO_FARMACEUTICO` | QF: libera o retiene lotes | Todos los almacenes |
| `COMPRAS` | Abastecimiento | Lectura de stock, requerimientos y OC |
| `SOLICITANTE` | Enfermería / coordinación de sede | Sus requisiciones |
| `AUDITOR` | Auditoría interna / contabilidad | Solo lectura, incluye kardex y auditoría |

### 1.1 Matriz de permisos (resumen)

| Permiso | ADMIN | JEFE_ALMACEN | ALMACENERO | QF | COMPRAS | SOLICITANTE | AUDITOR |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| `seguridad:usuario:*` | ✔ | | | | | | |
| `catalogo:producto:leer` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `catalogo:producto:escribir` | ✔ | ✔ | | | | | |
| `almacen:almacen:escribir` | ✔ | ✔ | | | | | |
| `inventario:stock:leer` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `inventario:movimiento:registrar` | ✔ | ✔ | ✔ | | | | |
| `inventario:movimiento:anular` | ✔ | ✔ | | | | | |
| `inventario:lote:liberar` | ✔ | | | ✔ | | | |
| `inventario:lote:retener` | ✔ | ✔ | | ✔ | | | |
| `compras:requerimiento:crear` | ✔ | ✔ | ✔ | | ✔ | | |
| `compras:orden:aprobar` | ✔ | ✔ | | | | | |
| `compras:recepcion:registrar` | ✔ | ✔ | ✔ | | | | |
| `despacho:requisicion:crear` | ✔ | ✔ | ✔ | | | ✔ | |
| `despacho:requisicion:atender` | ✔ | ✔ | ✔ | | | | |
| `conteo:toma:ejecutar` | ✔ | ✔ | ✔ | | | | |
| `conteo:toma:aprobar` | ✔ | ✔ | | | | | |
| `alertas:alerta:leer` | ✔ | ✔ | ✔ | ✔ | ✔ | | ✔ |
| `reportes:kardex:leer` | ✔ | ✔ | ✔ | ✔ | ✔ | | ✔ |
| `auditoria:log:leer` | ✔ | ✔ | | | | | ✔ |

---

## 2. E1 — Seguridad, roles y permisos *(resuelve P9)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-01** | Como usuario quiero iniciar sesión con usuario y contraseña para acceder según mi rol. | 1) Credenciales válidas → `200` con access token (15 min) y refresh token (8 h) en respuesta; 2) credenciales inválidas → `401` genérico sin revelar si el usuario existe; 3) 5 intentos fallidos en 15 min → cuenta bloqueada 15 min y registro en `intento_acceso`; 4) usuario inactivo → `401`; 5) la sesión se renueva sin re-login mientras el refresh token sea válido (**login transitivo**). |
| **HU-02** | Como usuario quiero cerrar sesión y que mi refresh token quede revocado. | `POST /auth/logout` revoca el token; su reuso devuelve `401`. |
| **HU-03** | Como usuario nuevo quiero cambiar mi contraseña en el primer acceso. | Si `debeCambiarPassword` = true, la API bloquea todo endpoint distinto de perfil/cambio de clave (`403 PASSWORD_CHANGE_REQUIRED`) y la UI redirige al formulario. |
| **HU-04** | Como admin quiero crear usuarios y asignarles roles y almacenes. | Email y username únicos; al menos un rol; contraseña temporal generada con política; almacenes asignados limitan los movimientos del usuario. |
| **HU-05** | Como admin quiero definir roles con permisos granulares. | Los permisos se listan por módulo; un rol del sistema (`esSistema`) no se puede eliminar; el cambio de permisos aplica al siguiente token emitido. |
| **HU-06** | Como auditor quiero consultar la pista de auditoría. | Registra usuario, IP, acción, entidad, id, fecha y valores relevantes; filtrable por usuario, entidad y rango de fechas; no editable ni eliminable. |
| **RNF** | Contraseñas con BCrypt (coste 12); política: ≥ 10 caracteres, mayúscula, minúscula, dígito y símbolo; no reutilizar las últimas 3. |

## 3. E2 — Catálogo de insumos y medicamentos *(resuelve P1, P2)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-07** | Como jefe de almacén quiero registrar productos con los datos que exige el sector salud. | Campos: código interno (único), nombre, descripción, categoría, tipo (`INSUMO_DIALISIS`, `MEDICAMENTO`, `MATERIAL_MEDICO`, `REACTIVO`, `EPP`, `REPUESTO_MAQUINA`, `LIMPIEZA`, `OFICINA`), UM base, presentación, marca, **registro sanitario** y su vencimiento, condición de almacenamiento (`AMBIENTE`, `REFRIGERADO_2_8`, `CONGELADO`, `PROTEGIDO_LUZ`), `controlaLote` y `controlaVencimiento` (obligatorios en medicamentos e insumos estériles), clasificación ABC, stock mínimo/máximo, punto de reorden, días de cobertura objetivo, `vidaUtilMinimaDias` para recepción. |
| **HU-08** | Como jefe de almacén quiero categorías jerárquicas. | Árbol con padre opcional; no se permite ciclo; no se elimina categoría con productos o hijos. |
| **HU-09** | Como jefe de almacén quiero unidades de medida con factor de conversión. | UM con `factorABase` y UM base (ej. `CAJA x 20 UNIDAD` → factor 20); los movimientos convierten a UM base para el saldo. |
| **HU-10** | Como compras quiero mantener proveedores. | RUC válido (11 dígitos) y único; contacto, condición de pago, lead time en días, estado; no se elimina proveedor con OC. |
| **HU-11** | Como usuario quiero buscar en el catálogo por código, nombre o categoría con paginación. | Búsqueda insensible a mayúsculas/acentos, filtros combinables, orden por columna y página de 20 por defecto. |

## 4. E3 — Red de almacenes y ubicaciones *(resuelve P6)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-12** | Como jefe de almacén quiero registrar almacenes por sede. | Código único, nombre, sede, dirección, tipo (`CENTRAL`, `SEDE`, `FARMACIA`, `CUARENTENA`, `FRIO`, `RECHAZADOS`), responsable y estado. |
| **HU-13** | Como almacenero quiero ubicar la mercadería por zona/rack/nivel. | Ubicación con código único por almacén, tipo (`PICKING`, `RESERVA`, `FRIO`, `CUARENTENA`), capacidad y estado; el stock se registra por ubicación. |
| **HU-14** | Como admin quiero asignar almacenes a cada usuario. | Un almacenero solo registra movimientos en sus almacenes asignados; el intento en otro almacén devuelve `403 ALMACEN_NO_AUTORIZADO`. |

## 5. E4 — Lotes, vencimientos y trazabilidad *(resuelve P2, P4, P8)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-15** | Como almacenero quiero registrar el lote del fabricante con su vencimiento. | Único por producto + código de lote + proveedor; fecha de vencimiento obligatoria si `controlaVencimiento`; vencimiento futuro al recepcionar; estado inicial `CUARENTENA` si el producto lo requiere, si no `LIBERADO`. |
| **HU-16** | Como QF quiero liberar o retener lotes. | Transiciones válidas: `CUARENTENA→LIBERADO|RECHAZADO`, `LIBERADO→RETENIDO`, `RETENIDO→LIBERADO|RECHAZADO`, cualquiera→`VENCIDO` (automático); solo `LIBERADO` es despachable; el cambio exige motivo y queda auditado. |
| **HU-17** | Como QF quiero bloquear un lote por alerta sanitaria (recall) y saber dónde está. | Al retener, el sistema impide su salida y lista almacenes, ubicaciones, saldos y los despachos que ya lo consumieron (trazabilidad hacia adelante). |
| **HU-18** | Como sistema quiero marcar lotes vencidos automáticamente. | Tarea diaria 00:30: lotes con `fechaVencimiento < hoy` pasan a `VENCIDO`, su stock deja de ser disponible y se genera alerta `VENCIDO`. |

## 6. E5 — Movimientos, stock y kardex *(resuelve P1, P5, P7)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-19** | Como almacenero quiero registrar un ingreso con lote, vencimiento y costo. | Documento de referencia (guía de remisión/factura) obligatorio; cantidad > 0; crea o incrementa `stock(producto, almacén, lote, ubicación)`; registra línea de kardex con saldo y costo promedio ponderado recalculado. |
| **HU-20** | Como almacenero quiero registrar una salida validando saldo disponible. | Si la cantidad excede el disponible del lote → `409 STOCK_INSUFICIENTE` con el disponible; no se permite salida de lote no `LIBERADO` → `409 LOTE_NO_DISPONIBLE`; descuenta al costo del lote y genera kardex. |
| **HU-21** | Como almacenero quiero que el sistema me proponga los lotes por FEFO. | La sugerencia ordena por vencimiento más próximo, luego por antigüedad de ingreso, solo lotes `LIBERADO` con saldo; si se elige otro lote se exige motivo y se audita. |
| **HU-22** | Como jefe de almacén quiero transferir stock entre almacenes con confirmación en destino. | `ENVIADA` descuenta origen y crea stock en tránsito; `RECIBIDA` acredita destino; diferencia entre enviado y recibido genera movimiento de `MERMA_TRANSITO` que requiere motivo; mantiene lote y vencimiento. |
| **HU-23** | Como jefe de almacén quiero ajustar inventario con motivo tipificado. | Motivos: `CONTEO_FISICO`, `MERMA`, `VENCIMIENTO`, `ROTURA`, `ERROR_REGISTRO`, `DONACION`; el ajuste exige comentario y genera kardex con signo correspondiente. |
| **HU-24** | Como jefe de almacén quiero anular un movimiento errado. | Solo movimientos del periodo abierto y no referenciados; la anulación no borra: genera movimiento inverso enlazado (`movimientoAnuladoId`) y marca el original `ANULADO`; exige motivo. |
| **HU-25** | Como contabilidad quiero el kardex valorizado de un producto. | Columnas: fecha, documento, tipo, lote, entrada, salida, saldo, costo unitario, costo promedio, valor de saldo; filtro por producto, almacén y rango; exportable a CSV; el saldo final coincide con `stock`. |
| **HU-26** | Como usuario quiero consultar el saldo en línea por producto, almacén, lote y ubicación. | Muestra cantidad total, disponible (descuenta reservado y no liberado), reservado, por vencer y vencido; responde < 400 ms. |
| **RNF** | Todo movimiento es transaccional y atómico; el stock usa bloqueo optimista (`@Version`); peticiones repetidas con la misma `Idempotency-Key` no duplican el movimiento. |

## 7. E6 — Abastecimiento y recepción *(resuelve P3, P8)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-27** | Como almacenero quiero generar un requerimiento de compra sugerido por el sistema. | Propone los productos con `disponible ≤ puntoReorden`, cantidad = `max(stockMaximo − disponible, consumoPromedioDiario × diasCobertura)`; editable antes de enviar; estados `BORRADOR→ENVIADO→APROBADO|RECHAZADO→ATENDIDO`. |
| **HU-28** | Como compras quiero convertir el requerimiento en orden de compra. | OC con proveedor, moneda, condición de pago, fecha esperada y líneas con precio; numeración correlativa `OC-AAAA-#####`; totales calculados por el servidor (subtotal, IGV 18 %, total). |
| **HU-29** | Como jefe de almacén quiero aprobar o rechazar la OC. | Solo con permiso `compras:orden:aprobar`; quien aprueba no puede ser quien la creó (segregación de funciones) salvo rol `ADMIN`; registra aprobador y fecha. |
| **HU-30** | Como almacenero quiero recepcionar contra la OC, total o parcialmente. | No se recibe más de lo pedido por línea (tolerancia 0); cada línea exige lote y vencimiento cuando aplica; la OC pasa a `RECIBIDA_PARCIAL` o `RECIBIDA`; la recepción conforme genera los ingresos de inventario. |
| **HU-31** | Como almacenero quiero aplicar control de calidad en la recepción. | Verifica integridad de empaque, cadena de frío (temperatura de llegada para `REFRIGERADO_2_8`), registro sanitario vigente y vida útil remanente ≥ `vidaUtilMinimaDias`; el resultado `RECHAZADO` no genera ingreso y queda con observación; `OBSERVADO` ingresa a almacén de cuarentena. |

## 8. E7 — Requisiciones internas y despacho *(resuelve P3, P5, P6)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-32** | Como enfermería quiero solicitar insumos para la sede/turno. | Requisición con almacén solicitante, almacén atendedor, centro de costo, prioridad (`NORMAL`, `URGENTE`), fecha requerida y líneas con cantidad; estados `BORRADOR→ENVIADA→EN_ATENCION→ATENDIDA|ATENDIDA_PARCIAL|ANULADA`. |
| **HU-33** | Como almacenero quiero atender la requisición con lotes sugeridos por FEFO. | Por línea propone lotes FEFO con saldo; permite atención parcial; al confirmar genera las salidas y el documento de despacho `DES-AAAA-#####`. |
| **HU-34** | Como enfermería quiero registrar el consumo por área y centro de costo. | Toda salida exige centro de costo y área destino; el reporte de consumo agrupa por área, mes y producto. |
| **HU-35** | Como solicitante quiero ver el estado de mis requisiciones. | Lista filtrada por mis requisiciones con cantidad pedida, atendida y pendiente por línea. |

## 9. E8 — Inventario físico y ajustes *(resuelve P1, P6)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-36** | Como jefe de almacén quiero programar una toma de inventario total o cíclica (ABC). | Toma con almacén, alcance (`TOTAL`, `POR_CATEGORIA`, `CICLICO_ABC`), fecha y responsable; al abrirla congela el saldo teórico de cada ítem. |
| **HU-37** | Como almacenero quiero registrar el conteo ciego. | La pantalla no muestra el saldo teórico durante el conteo; admite segundo conteo en diferencias. |
| **HU-38** | Como jefe de almacén quiero ver y aprobar las diferencias. | Muestra teórico, contado, diferencia y valor; al aprobar genera un ajuste por ítem con motivo `CONTEO_FISICO`; calcula el ERI de la toma y la cierra. |

## 10. E9 — Reposición y alertas *(resuelve P2, P3)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-39** | Como jefe de almacén quiero alertas de stock bajo. | Severidad: `CRITICA` si disponible = 0 o < 50 % del mínimo, `ALTA` si ≤ mínimo, `MEDIA` si ≤ punto de reorden; una alerta activa por producto/almacén; se cierra sola al reponerse. |
| **HU-40** | Como QF quiero alertas de vencimiento por tramos. | Tramos 90, 60, 30 días y vencido; incluye lote, almacén, cantidad y valor expuesto; tarea diaria 01:00. |
| **HU-41** | Como jefe de almacén quiero ver la cobertura en días por producto. | `cobertura = disponible / consumoPromedioDiario(90 días)`; resalta cobertura < 15 días. |
| **HU-42** | Como usuario quiero ver mis alertas en la campana del encabezado. | Contador por severidad, marcado como leída y enlace al producto/lote; filtrable por tipo y almacén. |

## 11. E10 — Dashboard, reportes y auditoría *(resuelve P7)*

| HU | Historia | Criterios de aceptación |
|---|---|---|
| **HU-43** | Como jefe de almacén quiero un dashboard con los indicadores del almacén. | Tarjetas: valor del inventario, ítems bajo mínimo, lotes por vencer en 30/60/90 días, lotes vencidos, requisiciones pendientes, OC en tránsito, movimientos del día; gráficos de consumo por área (12 meses) y top 10 productos por rotación; filtrado por almacén. |
| **HU-44** | Como contabilidad quiero reportes exportables. | Kardex valorizado, valorización de inventario por categoría, consumo por área/centro de costo, vencimientos, rotación y baja rotación (sin movimiento > 90 días), diferencias de inventario; exportación CSV con separador y codificación compatibles con Excel. |
| **HU-45** | Como auditor quiero reconstruir la historia de un lote. | Dado un lote: recepción, OC, proveedor, liberación, transferencias y despachos con fecha, usuario, área destino y saldo remanente. |

## 12. Requerimientos no funcionales

| ID | Requerimiento |
|---|---|
| RNF-01 | **Seguridad:** JWT firmado HS512 con secreto fuera del código, autorización por permiso en cada endpoint, CORS restringido por origen, cabeceras `HSTS`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y CSP; sin datos clínicos de paciente. |
| RNF-02 | **Rendimiento:** p95 < 400 ms en consultas de stock y < 800 ms en kardex con 100 000 movimientos; paginación obligatoria en toda colección; índices en claves de búsqueda. |
| RNF-03 | **Disponibilidad:** respaldo diario de PostgreSQL con retención de 30 días; `healthcheck` y `readiness` expuestos; despliegue reproducible con Docker Compose. |
| RNF-04 | **Usabilidad:** responsive de 320 px a 4K, tema claro/oscuro, foco visible, navegación por teclado, etiquetas `aria`, mensajes de error en español y contraste AA. |
| RNF-05 | **Mantenibilidad:** monolito modular con dependencias dirigidas, cobertura ≥ 70 %, commits convencionales, CI que falla con la build roja, sin lógica de negocio en controladores ni en componentes. |
| RNF-06 | **Escalabilidad:** API REST sin estado (escalado horizontal), `HikariCP` dimensionado, caché de catálogos, consultas proyectadas (sin `N+1`), preparado para particionar `kardex` por año. |
| RNF-07 | **Auditoría y trazabilidad:** `createdBy/createdAt/updatedBy/updatedAt` en toda entidad, pista de auditoría inmutable y kardex de solo inserción (nunca `UPDATE`/`DELETE`). |
| RNF-08 | **Internacionalización y formato:** español (Perú), zona `America/Lima`, moneda `PEN`, decimales con 4 posiciones en cantidades y 6 en costos unitarios. |
