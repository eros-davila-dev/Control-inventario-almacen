# Plan de Implementación — SGAI-CID

**Sistema automatizado para el registro y control de almacén en una empresa del sector salud — Lima, 2026**

| Dato | Valor |
|---|---|
| Empresa | CORPORACIÓN INTEGRAL DE DIÁLISIS S.A.C. (RUC 20605165410) |
| Área usuaria | Almacén / Logística, con soporte del Departamento de Sistemas |
| Practicante | Yopan Chuquipul, Orlando |
| Jefe inmediato | Ing. Carrasco Peña, Luis Ricardo — Jefe de TI |
| Enfoque de gestión | PMI (Inicio, Planificación, Ejecución, Monitoreo y Control, Cierre) |
| Metodología de desarrollo | Scrum — 6 Sprints de 2 semanas (12 semanas) |
| Producto | SGAI-CID: Sistema de Gestión de Almacén e Inventarios |
| Stack | Java 21 · Spring Boot 3.5 · PostgreSQL 17 · Angular 20 · Tailwind CSS 4 |

---

## 1. FASE DE INICIO (PMI)

### 1.1 Problemática identificada

El almacén de CID abastece a las unidades de hemodiálisis (sedes/clínicas). Hoy el control es
manual (hojas de cálculo y cuadernos de cargo), lo que produce:

| # | Problema observado | Efecto en la operación | Métrica base (AS-IS) |
|---|---|---|---|
| P1 | No existe saldo en línea por producto/lote/almacén | Compras de urgencia y sobrestock de dializadores y líneas arteriovenosas | Exactitud de inventario ≈ 72 % |
| P2 | Vencimientos no controlados (insumos y medicamentos: heparina, EPO, hierro sacarato) | Mermas por producto vencido; riesgo sanitario | Mermas ≈ 3.1 % del valor del inventario |
| P3 | Quiebres de stock de insumos críticos (concentrado ácido, bicarbonato, agujas de fístula) | Reprogramación o suspensión de sesiones de hemodiálisis | 6–9 quiebres/mes |
| P4 | Sin trazabilidad por lote | Imposible ejecutar un retiro de mercado (recall) o responder a DIGEMID | Trazabilidad no disponible |
| P5 | Despacho sin criterio FEFO formal | Se consume el lote más accesible, no el que vence primero | — |
| P6 | Transferencias entre sedes sin registro | Diferencias de inventario no explicadas | Diferencias en 18 % de los ítems |
| P7 | Kardex y valorización elaborados a mano al cierre | Información financiera tardía (hasta 10 días) | Cierre en 8–10 días |
| P8 | Sin control de cadena de frío ni cuarentena de lotes | Lotes no liberados puestos en consumo | — |
| P9 | Sin control de accesos ni auditoría | No se puede determinar quién registró o modificó un movimiento | — |

### 1.2 Interesados

| Interesado | Rol en el producto | Interés principal |
|---|---|---|
| Jefe de TI (Ing. Carrasco Peña) | **Product Owner** | Integración, seguridad y continuidad del servicio |
| Jefe de Almacén | Usuario clave / aprobador | Exactitud de inventario, reposición oportuna |
| Almaceneros (sede central y sedes) | Usuario operativo | Registro rápido de ingresos/salidas |
| Químico Farmacéutico (QF) | Usuario regulatorio | Liberación de lotes, cuarentena, vencimientos |
| Coordinación de Enfermería / Hemodiálisis | Solicitante interno | Atención oportuna de requisiciones |
| Jefatura de Compras | Usuario | Requerimientos y órdenes de compra confiables |
| Contabilidad / Administración | Consumidor de información | Valorización y kardex |
| Auditoría interna / Dirección Médica | Consultor | Trazabilidad y cumplimiento |
| Practicante (desarrollador) | **Scrum Developer** | Entrega del incremento |
| Docente supervisor UCV | Supervisor académico | Cumplimiento del plan |

### 1.3 Visión del producto y valor esperado

> Para el personal de almacén, farmacia y enfermería de CID, que hoy controla el inventario de
> insumos de hemodiálisis en hojas de cálculo, **SGAI-CID** es un sistema web que registra cada
> movimiento con lote y vencimiento, impone FEFO, alerta reposición y vencimientos, y entrega
> kardex valorizado en línea; a diferencia del control manual, garantiza trazabilidad sanitaria
> por lote y evita quiebres que suspenden sesiones de diálisis.

### 1.4 Objetivo general y resultados esperados (TO-BE)

**Objetivo general:** implementar un sistema automatizado de registro y control de almacén que
asegure la exactitud, trazabilidad y disponibilidad de los insumos y medicamentos usados en la
atención de pacientes de hemodiálisis.

| Objetivo específico | Indicador | Base | Meta |
|---|---|---|---|
| OE1 Exactitud de inventario | ERI = ítems correctos / ítems contados | 72 % | ≥ 97 % |
| OE2 Reducir mermas por vencimiento | Valor vencido / valor inventario | 3.1 % | ≤ 0.8 % |
| OE3 Eliminar quiebres de insumos críticos | Quiebres de ítems clase A por mes | 6–9 | ≤ 1 |
| OE4 Trazabilidad por lote | % de movimientos con lote registrado | 0 % | 100 % |
| OE5 Oportunidad de la información | Días para cerrar kardex valorizado | 8–10 | 0 (en línea) |
| OE6 Control de accesos | % de operaciones con usuario y pista de auditoría | 0 % | 100 % |

### 1.5 Alcance y exclusiones

**Incluye:** maestros (productos, UM, categorías, proveedores, almacenes y ubicaciones), lotes y
vencimientos, stock por producto–almacén–lote–ubicación, movimientos (ingreso, salida,
transferencia, ajuste, devolución, merma), recepción contra orden de compra con control de calidad,
requisiciones internas y despacho con sugerencia FEFO, toma de inventario físico y ajuste,
reposición (stock mínimo/máximo, punto de reorden, cobertura), alertas de vencimiento, kardex
valorizado por costo promedio ponderado, dashboard y reportes exportables, seguridad con roles y
permisos granulares, y auditoría.

**Excluye:** historia clínica y datos clínicos del paciente, facturación electrónica y SUNAT,
contabilidad general, nómina, compras a proveedores por EDI, app móvil nativa (la web es
responsive), e integración con el ERP vigente (se deja definida la capa de API REST versionada).

---

## 2. FASE DE PLANIFICACIÓN

### 2.1 Product Goal

> Contar, al final del Sprint 6, con un sistema en producción que registre el 100 % de los
> movimientos de almacén con lote y vencimiento, aplique FEFO en el despacho y entregue kardex
> valorizado y alertas de reposición y vencimiento en línea.

### 2.2 Épicas y mapa de módulos

| Épica | Módulo backend | Módulo frontend |
|---|---|---|
| E1 Seguridad, roles y permisos | `seguridad` | `features/auth`, `features/seguridad` |
| E2 Maestros / catálogo | `catalogo` | `features/catalogo` |
| E3 Red de almacenes y ubicaciones | `almacen` | `features/almacenes` |
| E4 Lotes, vencimientos y trazabilidad | `inventario.lote` | `features/lotes` |
| E5 Movimientos, stock y kardex | `inventario` | `features/inventario` |
| E6 Abastecimiento y recepción | `compras` | `features/compras` |
| E7 Requisiciones internas y despacho FEFO | `despacho` | `features/despacho` |
| E8 Inventario físico y ajustes | `conteo` | `features/conteo` |
| E9 Reposición y alertas | `alertas` | `features/alertas` |
| E10 Dashboard, reportes y auditoría | `reportes`, `shared.audit` | `features/dashboard`, `features/reportes`, `features/auditoria` |

El detalle de historias de usuario y criterios de aceptación está en
[`02-REQUERIMIENTOS.md`](02-REQUERIMIENTOS.md); el modelo de datos en
[`04-MODELO-DE-DATOS.md`](04-MODELO-DE-DATOS.md).

### 2.3 Roadmap de Sprints (12 semanas)

| Sprint | Semanas | Sprint Goal | Incremento |
|---|---|---|---|
| **1** | 1–2 | Plataforma y seguridad operativas | Monorepo, CI, PostgreSQL + Flyway, login JWT con refresh, roles y permisos, layout Angular con tema claro/oscuro, auditoría |
| **2** | 3–4 | Maestros administrables | Catálogo (UM, categorías, productos con registro sanitario y condiciones de almacenamiento), proveedores, almacenes y ubicaciones; tabla, filtros y formularios reutilizables |
| **3** | 5–6 | Saldo real por lote | Lotes con vencimiento y estado sanitario, stock por producto–almacén–lote–ubicación, movimientos de ingreso/salida/ajuste, kardex valorizado (CPP) |
| **4** | 7–8 | Flujo de abastecimiento | Requerimiento de compra por punto de reorden, orden de compra, recepción con control de calidad y cuarentena, liberación de lote por QF |
| **5** | 9–10 | Flujo de atención a sedes | Requisición interna, atención con sugerencia FEFO, despacho parcial, transferencia entre almacenes, confirmación de recepción en sede |
| **6** | 11–12 | Control y cierre | Toma de inventario físico y ajuste aprobado, alertas programadas, dashboard e indicadores, reportes exportables, endurecimiento de seguridad, documentación y despliegue |

### 2.4 Roles Scrum y comunicación

| Rol | Persona | Responsabilidad |
|---|---|---|
| Product Owner | Jefe de TI | Product Backlog, priorización, aceptación |
| Scrum Master | Docente supervisor / líder técnico | Facilitación, impedimentos |
| Developer | Practicante | Diseño, construcción, pruebas, documentación |
| Stakeholders | Jefe de Almacén, QF, Enfermería, Compras | Refinamiento y validación |

Eventos: Sprint Planning (lunes de inicio, 2 h), Daily Scrum (15 min), Sprint Review (1.5 h con
usuarios clave), Sprint Retrospective (1 h). Canales: correo corporativo, reuniones en sede y
tablero del Sprint Backlog.

### 2.5 Definition of Done (DoD)

1. Historia con criterios de aceptación verificados por el PO.
2. Reglas de negocio cubiertas por pruebas unitarias; endpoints por pruebas de integración.
3. Autorización declarada por permiso (`@PreAuthorize`) y verificada.
4. Migración Flyway versionada e idempotente; sin DDL manual.
5. Sin errores de `mvn verify` ni de `ng build`; sin `console.log` ni credenciales en código.
6. API documentada en OpenAPI; pantallas accesibles (teclado, contraste) y responsive.
7. Operación registrada en la pista de auditoría cuando modifica inventario.

### 2.6 Riesgos

| # | Riesgo | P | I | Respuesta |
|---|---|---|---|---|
| R1 | Datos maestros sucios en Excel (duplicados, sin UM) | A | A | Mitigar: plantilla de carga, normalización de UM y validación de duplicados por código/nombre |
| R2 | Resistencia al registro en línea del almacenero | M | A | Mitigar: pantallas de 3 clics, lector de código de barras, capacitación en sede |
| R3 | Error de valorización por costo | M | A | Mitigar: CPP calculado por el servidor, kardex inmutable, pruebas con casos reales |
| R4 | Concurrencia en el mismo lote (dos despachos simultáneos) | M | A | Mitigar: bloqueo optimista por versión + transacción; reintento controlado |
| R5 | Requisitos regulatorios (DIGEMID) no cubiertos | B | A | Mitigar: registro sanitario, cuarentena y liberación por QF desde el Sprint 4 |
| R6 | Infraestructura de servidor no disponible a tiempo | M | M | Mitigar: despliegue en Docker Compose, reproducible en cualquier host |
| R7 | Alcance creciente (facturación, historia clínica) | A | M | Evitar: exclusiones firmadas; nuevas solicitudes al Product Backlog |

### 2.7 Criterios de calidad

Cobertura de pruebas ≥ 70 % en paquetes de aplicación y dominio; respuesta de consultas de stock
< 400 ms con 100 000 movimientos; sin vulnerabilidades altas en `mvn dependency-check` /
`npm audit`; cero contraseñas o secretos en el repositorio; Lighthouse ≥ 90 en accesibilidad.

---

## 3. FASE DE EJECUCIÓN — Arquitectura y práctica de construcción

Detalle completo en [`03-ARQUITECTURA.md`](03-ARQUITECTURA.md) y
[`05-SEGURIDAD.md`](05-SEGURIDAD.md).

- **Backend:** monolito modular con arquitectura en capas por módulo de negocio
  (`domain` → `repository` → `application` → `web`), DTOs inmutables (`record`), mapeo con
  MapStruct, reglas de negocio en servicios de aplicación transaccionales, errores como
  `ProblemDetail` (RFC 7807), migraciones Flyway y OpenAPI.
- **Frontend:** Angular 20 standalone + signals, `ChangeDetectionStrategy.OnPush`, rutas
  perezosas por feature, interceptores funcionales (auth, refresh, errores, carga), guards de
  ruta por permiso, librería de UI reutilizable (`shared/ui`) y `BaseCrudService<T>` genérico.
- **Estilo:** Tailwind CSS 4 con `@theme` y tokens semánticos; tema claro/oscuro persistido y
  respetando `prefers-color-scheme`; layout responsive (móvil con navegación inferior,
  escritorio con sidebar colapsable).
- **Prácticas:** trunk-based con ramas cortas, commits convencionales, PR con revisión, CI
  (build + pruebas + auditoría de dependencias), code review y refactorización continua.

## 4. FASE DE MONITOREO Y CONTROL

Daily Scrum y Sprint Backlog en tablero; métricas por Sprint: velocidad (story points),
defectos abiertos/cerrados, deuda técnica, cobertura y cumplimiento del Sprint Goal; impedimentos
escalados al Jefe de TI en 24 h; verificación de DoD en el Sprint Review antes de aceptar cada
historia; refinamiento del Product Backlog en la segunda semana de cada Sprint.

## 5. FASE DE CIERRE

Validación final con el Jefe de Almacén y el QF sobre datos reales; migración de maestros y saldos
iniciales; capacitación por rol y manual de usuario; acta de entrega y transferencia al
Departamento de Sistemas (repositorio, credenciales en gestor de secretos, runbook de respaldo y
restauración); registro de lecciones aprendidas y presentación de resultados con los indicadores
OE1–OE6 medidos.

## 6. Entregables

| Entregable | Ubicación |
|---|---|
| Plan de implementación | `docs/01-PLAN-DE-IMPLEMENTACION.md` |
| Requerimientos, épicas e historias | `docs/02-REQUERIMIENTOS.md` |
| Arquitectura backend y frontend | `docs/03-ARQUITECTURA.md` |
| Modelo de datos | `docs/04-MODELO-DE-DATOS.md` |
| Seguridad | `docs/05-SEGURIDAD.md` |
| Product Backlog y Sprints | `docs/06-PRODUCT-BACKLOG.md` |
| Manual de usuario por rol | `docs/07-MANUAL-DE-USUARIO.md` |
| Código backend | `backend/` |
| Código frontend | `frontend/` |
| Despliegue | `docker-compose.yml`, `.github/workflows/ci.yml` |
