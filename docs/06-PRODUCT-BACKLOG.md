# Product Backlog y Sprint Backlogs — SGAI-CID

Estimación en story points (Fibonacci), prioridad `MoSCoW`.

## Product Backlog ordenado

| # | HU | Épica | Descripción corta | SP | Prioridad | Sprint |
|---|---|---|---|---:|---|---|
| 1 | HU-01 | E1 | Login JWT con bloqueo por intentos | 5 | Must | 1 |
| 2 | HU-02 | E1 | Logout con revocación de refresh | 2 | Must | 1 |
| 3 | HU-03 | E1 | Cambio de contraseña obligatorio | 3 | Must | 1 |
| 4 | HU-04 | E1 | Gestión de usuarios y asignación de almacenes | 5 | Must | 1 |
| 5 | HU-05 | E1 | Roles con permisos granulares | 5 | Must | 1 |
| 6 | HU-06 | E10 | Pista de auditoría | 3 | Should | 1 |
| 7 | HU-09 | E2 | Unidades de medida con conversión | 3 | Must | 2 |
| 8 | HU-08 | E2 | Categorías jerárquicas | 3 | Must | 2 |
| 9 | HU-07 | E2 | Productos con datos sanitarios | 8 | Must | 2 |
| 10 | HU-10 | E2 | Proveedores | 3 | Must | 2 |
| 11 | HU-11 | E2 | Búsqueda y paginación del catálogo | 3 | Must | 2 |
| 12 | HU-12 | E3 | Almacenes por sede | 3 | Must | 2 |
| 13 | HU-13 | E3 | Ubicaciones (zona/rack/nivel) | 3 | Should | 2 |
| 14 | HU-14 | E3 | Alcance de almacén por usuario | 3 | Must | 2 |
| 15 | HU-15 | E4 | Lotes con vencimiento | 5 | Must | 3 |
| 16 | HU-19 | E5 | Ingreso con lote y costo | 8 | Must | 3 |
| 17 | HU-20 | E5 | Salida con validación de saldo | 8 | Must | 3 |
| 18 | HU-26 | E5 | Consulta de saldo en línea | 5 | Must | 3 |
| 19 | HU-25 | E5 | Kardex valorizado (CPP) | 8 | Must | 3 |
| 20 | HU-23 | E5 | Ajuste con motivo tipificado | 5 | Must | 3 |
| 21 | HU-24 | E5 | Anulación con movimiento inverso | 5 | Should | 3 |
| 22 | HU-16 | E4 | Liberación/retención de lotes por QF | 5 | Must | 4 |
| 23 | HU-27 | E6 | Requerimiento de compra sugerido | 8 | Must | 4 |
| 24 | HU-28 | E6 | Orden de compra con totales | 5 | Must | 4 |
| 25 | HU-29 | E6 | Aprobación con segregación de funciones | 3 | Must | 4 |
| 26 | HU-30 | E6 | Recepción total/parcial contra OC | 8 | Must | 4 |
| 27 | HU-31 | E6 | Control de calidad en recepción | 5 | Must | 4 |
| 28 | HU-32 | E7 | Requisición interna | 5 | Must | 5 |
| 29 | HU-21 | E5 | Sugerencia FEFO | 8 | Must | 5 |
| 30 | HU-33 | E7 | Atención con FEFO y despacho parcial | 8 | Must | 5 |
| 31 | HU-34 | E7 | Consumo por área y centro de costo | 3 | Must | 5 |
| 32 | HU-35 | E7 | Seguimiento de requisiciones | 3 | Should | 5 |
| 33 | HU-22 | E5 | Transferencia con confirmación en destino | 8 | Must | 5 |
| 34 | HU-17 | E4 | Bloqueo por alerta sanitaria y trazabilidad | 5 | Should | 6 |
| 35 | HU-18 | E4 | Marcado automático de vencidos | 3 | Must | 6 |
| 36 | HU-36 | E8 | Programación de toma de inventario | 5 | Must | 6 |
| 37 | HU-37 | E8 | Conteo ciego | 5 | Must | 6 |
| 38 | HU-38 | E8 | Aprobación de diferencias y ERI | 5 | Must | 6 |
| 39 | HU-39 | E9 | Alertas de stock bajo | 5 | Must | 6 |
| 40 | HU-40 | E9 | Alertas de vencimiento por tramos | 5 | Must | 6 |
| 41 | HU-41 | E9 | Cobertura en días | 3 | Should | 6 |
| 42 | HU-42 | E9 | Centro de notificaciones | 3 | Should | 6 |
| 43 | HU-43 | E10 | Dashboard de indicadores | 8 | Must | 6 |
| 44 | HU-44 | E10 | Reportes exportables | 8 | Must | 6 |
| 45 | HU-45 | E10 | Reconstrucción de historia de lote | 5 | Should | 6 |

**Total: 220 SP / 6 Sprints ⇒ velocidad objetivo ≈ 37 SP por Sprint.**

## Sprint Backlogs

### Sprint 1 — Plataforma y seguridad (23 SP)
*Sprint Goal:* un usuario con rol puede autenticarse y navegar el esqueleto del sistema, con toda
operación auditada.
Tareas: monorepo y CI · `docker-compose` con PostgreSQL 17 · Flyway `V1` seguridad · entidades y
repositorios de seguridad · `JwtService` y filtro · `AuthService` (login/refresh/logout/bloqueo) ·
CRUD de usuarios y roles · aspecto de auditoría · proyecto Angular 20 + Tailwind 4 + tema
claro/oscuro · shell responsive · interceptores y guards · login y cambio de contraseña.

### Sprint 2 — Maestros (26 SP)
*Sprint Goal:* el jefe de almacén administra el catálogo completo de insumos de diálisis y la red
de almacenes sin apoyo de TI.
Tareas: Flyway `V2` catálogo y almacenes · `BaseCrudService` + `Specification` de filtros ·
servicios y controladores de UM, categorías, productos, proveedores, almacenes y ubicaciones ·
`ui-table`, `ui-modal`, formularios tipados y `BaseCrudService` del frontend · asignación de
almacenes al usuario.

### Sprint 3 — Saldo real y kardex (39 SP)
*Sprint Goal:* el saldo del sistema refleja el físico por lote y el kardex valorizado está en línea.
Tareas: Flyway `V3` inventario · entidades `Lote`, `Stock`, `Movimiento`, `MovimientoLinea`,
`Kardex` · `StockService` con bloqueo optimista · `MovimientoService` (ingreso, salida, ajuste,
anulación) · `CostoPromedioService` · `KardexService` · idempotencia · vistas de stock · pantallas
de consulta de stock, registro de movimientos y kardex con exportación.

### Sprint 4 — Abastecimiento (29 SP)
*Sprint Goal:* la reposición nace del sistema y lo que llega se recibe con control de calidad y
trazabilidad de lote.
Tareas: Flyway `V4` compras · sugerencia de reposición · requerimiento, OC con totales y
aprobación segregada · recepción total/parcial · control de calidad y cuarentena · liberación de
lote por QF · pantallas de compras y recepción con wizard.

### Sprint 5 — Atención a sedes (35 SP)
*Sprint Goal:* enfermería solicita y almacén despacha aplicando FEFO, con transferencias entre
sedes controladas.
Tareas: Flyway `V5` despacho · requisición y seguimiento · `EstrategiaAsignacionLote` FEFO ·
despacho total/parcial con registro de desviación FEFO · transferencia con stock en tránsito y
confirmación en destino · consumo por área/centro de costo · pantallas de requisición, picking y
transferencias.

### Sprint 6 — Control, indicadores y cierre (46 SP)
*Sprint Goal:* el almacén se controla con conteos, alertas e indicadores, y el sistema queda
documentado y desplegado.
Tareas: Flyway `V6` conteo y alertas · toma de inventario, conteo ciego, diferencias, ERI y ajuste
aprobado · jobs de vencimiento y alertas · centro de notificaciones · dashboard · reportes y
exportación CSV · trazabilidad de lote · endurecimiento de seguridad · manual de usuario y acta de
entrega.

## Definición de métricas del Sprint

| Métrica | Fórmula | Fuente |
|---|---|---|
| Velocidad | SP aceptados / Sprint | Tablero |
| Cumplimiento del Sprint Goal | Historias aceptadas / comprometidas | Sprint Review |
| Densidad de defectos | Defectos / SP entregados | Tablero de defectos |
| Cobertura | `jacoco` + `ng test --code-coverage` | CI |
| Deuda técnica | Pendientes de refactorización abiertos | Retrospectiva |
