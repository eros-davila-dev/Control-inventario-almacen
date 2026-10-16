# Arquitectura — SGAI-CID

## 1. Vista general

```
┌──────────────────────── Navegador (320 px → 4K) ────────────────────────┐
│  Angular 20 · standalone components · signals · Tailwind CSS 4          │
│  core (auth, http, theme, guards, interceptors) · shared/ui · features  │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTPS · JSON · JWT Bearer
┌───────────────────────────────▼─────────────────────────────────────────┐
│ Spring Boot 3.5 (Java 21) — monolito modular                            │
│  web  →  application  →  domain  ←  repository (Spring Data JPA)        │
│  Módulos: seguridad · catalogo · almacen · inventario · compras ·       │
│           despacho · conteo · alertas · reportes · shared               │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ JDBC (HikariCP) · Flyway
┌───────────────────────────────▼─────────────────────────────────────────┐
│ PostgreSQL 17 — esquema `sgai`                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Por qué monolito modular y no microservicios:** el equipo es de una persona, la operación es de
una sola empresa y las transacciones de inventario deben ser atómicas (stock + kardex + documento).
Los límites de módulo quedan explícitos para poder extraer un servicio después sin reescribir el
dominio.

## 2. Backend

### 2.1 Capas por módulo

```
pe.cid.sgai.<modulo>
├── domain/          entidades JPA, enums, reglas invariantes (sin Spring)
├── repository/      puertos de persistencia (Spring Data) + proyecciones
├── application/     servicios transaccionales, DTO (record), mappers MapStruct
└── web/             controladores REST, validación de entrada, OpenAPI
```

Reglas de dependencia (verificadas por revisión y por tests de ArchUnit opcionales):

1. `web` → `application` → `domain`; nunca al revés.
2. `web` no inyecta repositorios ni expone entidades: solo DTOs.
3. Un módulo usa otro **solo** a través de su capa `application` (servicio público).
4. `shared` no depende de ningún módulo de negocio.
5. Sin lógica de negocio en controladores ni en entidades anémicas: las invariantes de stock
   viven en `inventario.domain` (`Stock.descontar()`, `Lote.esDespachable()`).

### 2.2 Decisiones técnicas

| Tema | Decisión | Motivo |
|---|---|---|
| DTOs | `record` inmutables con Bean Validation | Seguridad de tipos, sin setters, sin sobre-exposición de entidades |
| Mapeo | MapStruct (`componentModel = spring`) | Mapeo en tiempo de compilación, sin reflexión ni código repetitivo |
| Errores | `@RestControllerAdvice` + `ProblemDetail` (RFC 7807) con `codigo` de negocio | Contrato de error único y traducible en el frontend |
| Paginación | `Pageable` + `PageResponse<T>` propio | Respuesta estable, independiente de la serialización de `Page` |
| Transacciones | `@Transactional` en servicios de aplicación, nunca en controladores | Límite transaccional explícito |
| Concurrencia | `@Version` en `stock` y `lote`; reintento en conflicto | Dos despachos del mismo lote no sobrevenden |
| Idempotencia | Cabecera `Idempotency-Key` en POST de movimientos | Evita duplicados por doble clic o reintento de red |
| Numeración | `correlativo` por tipo y año con secuencia en base de datos | `ING-2026-00042` sin colisiones |
| Auditoría | `AuditingEntityListener` (`createdBy/At`, `updatedBy/At`) + tabla `auditoria_log` vía AOP | Trazabilidad exigida por auditoría interna |
| Dinero y cantidades | `BigDecimal` (cantidad 18,4 · costo 18,6) | Nunca `double` en valores monetarios |
| Fechas | `LocalDate`/`LocalDateTime` en `America/Lima`; `Instant` en auditoría | Evita ambigüedad de zona |
| Esquema | Flyway `V1__`, `V2__`… + `R__` para vistas | Versionado reproducible, sin `ddl-auto` |
| Documentación | springdoc-openapi (`/swagger-ui.html`) | Contrato vivo para integraciones |

### 2.3 Patrones aplicados

- **Service Layer** y **DTO/Assembler** por módulo.
- **Repository** con proyecciones de solo lectura para reportes (evita `N+1` y entidades pesadas).
- **Strategy** en la asignación de lotes: `EstrategiaAsignacionLote` con implementación `FEFO`
  (vence primero) y `FIFO`, seleccionable por producto a futuro.
- **State machine** explícita en `EstadoLote`, `EstadoOrdenCompra`, `EstadoRequisicion` y
  `EstadoToma`: cada transición se valida en un único punto (`puedeTransitarA`).
- **Template Method** en `BaseCrudService<E, ID, REQ, RES>` para los maestros, dejando a cada
  servicio solo sus validaciones propias.
- **Specification** (`JpaSpecificationExecutor`) para filtros combinables de las consultas.
- **Observer/Eventos** de Spring (`ApplicationEventPublisher`) para desacoplar efectos
  secundarios: un movimiento publica `StockActualizadoEvent` y el módulo `alertas` reacciona.
- **Scheduled jobs** para vencimientos, alertas y cálculo de consumo promedio.

### 2.4 Estructura de carpetas (backend)

```
backend/src/main/java/pe/cid/sgai/
├── SgaiApplication.java
├── shared/        config · domain(BaseEntity) · exception · web(PageResponse) · audit · util
├── seguridad/     domain · repository · application · infrastructure(JWT) · web
├── catalogo/      unidadmedida · categoria · producto · proveedor
├── almacen/       almacen · ubicacion
├── inventario/    lote · stock · movimiento(kardex, FEFO, CPP) · transferencia
├── compras/       requerimiento · ordencompra · recepcion
├── despacho/      requisicion · despacho
├── conteo/        toma · detalle
├── alertas/       alerta · jobs
└── reportes/      kardex · valorizacion · consumo · dashboard
backend/src/main/resources/db/migration/   V1..V6 + seed
```

## 3. Frontend

### 3.1 Estructura

```
frontend/src/app/
├── core/
│   ├── auth/         auth.service(signals) · auth.store · token.storage · permisos
│   ├── http/         api.service · interceptors(auth, refresh, error, loading)
│   ├── guards/       auth.guard · permiso.guard · cambio-password.guard
│   ├── layout/       shell · sidebar · topbar · bottom-nav · user-menu
│   └── services/     theme · notificacion(toast) · confirm · breakpoint · export-csv
├── shared/
│   ├── ui/           ui-button · ui-input · ui-select · ui-textarea · ui-checkbox ·
│   │                 ui-card · ui-badge · ui-modal · ui-table(paginación/orden) ·
│   │                 ui-pagination · ui-empty-state · ui-skeleton · ui-toast ·
│   │                 ui-tabs · ui-stat-card · ui-autocomplete · ui-date-range
│   ├── directives/   hasPermiso · autofocus · clickOutside
│   ├── pipes/        fechaLocal · moneda · cantidad · estadoBadge
│   └── models/       api.model · page.model · filtro.model
└── features/  auth · dashboard · seguridad · catalogo · almacenes · inventario ·
              compras · despacho · conteo · alertas · reportes · auditoria · perfil
```

### 3.2 Decisiones

| Tema | Decisión |
|---|---|
| Componentes | `standalone: true`, `ChangeDetectionStrategy.OnPush`, nueva sintaxis `@if/@for/@switch` |
| Estado | `signal`, `computed`, `linkedSignal` y `resource`/`httpResource` para datos remotos; sin librería externa de estado |
| Formularios | Reactive Forms tipados + componentes de control con `ControlValueAccessor` |
| HTTP | `provideHttpClient(withFetch(), withInterceptors([...]))`, interceptores funcionales |
| Rutas | `loadChildren` por feature (code splitting) y `canMatch` por permiso |
| Reutilización | `BaseCrudService<T>` genérico (`list/get/create/update/remove`) + `ui-table` con columnas declarativas; cada CRUD se reduce a configuración |
| Accesibilidad | Roles ARIA, `aria-live` en toasts, foco atrapado en modales, `prefers-reduced-motion` |
| Tema | Clase `dark` en `<html>`, servicio `ThemeService` con `signal` persistido en `localStorage` y escucha de `prefers-color-scheme` |
| Zoneless | `provideZonelessChangeDetection()` (Angular 20) para menor overhead |

### 3.3 Sistema de diseño (Tailwind CSS 4)

Tokens semánticos en `styles.css` con `@theme`, no colores crudos en las plantillas:

```css
@import "tailwindcss";
@custom-variant dark (&:where(.dark, .dark *));
@theme {
  --color-brand-50 … --color-brand-900;   /* identidad clínica (teal/azul) */
  --color-surface, --color-surface-muted, --color-border, --color-fg, --color-fg-muted;
  --color-success, --color-warning, --color-danger, --color-info;
}
```

Las variables de `surface/fg/border` se redefinen bajo `.dark`, de modo que **un componente usa
`bg-surface text-fg` y funciona en ambos temas sin condicionales**. Severidades de alerta y estados
de documento se renderizan con `ui-badge` y un mapa de tono único.

### 3.4 Responsive

| Breakpoint | Comportamiento |
|---|---|
| `< 640 px` | Navegación inferior de 5 accesos, tablas en tarjetas apiladas, formularios de una columna, modales a pantalla completa |
| `640–1024 px` | Sidebar colapsado a iconos, tablas con scroll horizontal y columnas prioritarias |
| `> 1024 px` | Sidebar expandido, tablas completas, paneles laterales de detalle |

## 4. Flujos críticos

### 4.1 Despacho con FEFO

```
UI requisición → POST /api/v1/despachos
  DespachoService (@Transactional)
    1 valida requisición ENVIADA/EN_ATENCION y permisos sobre el almacén
    2 por línea: AsignacionLoteService.sugerirFEFO(producto, almacén, cantidad)
        → lotes LIBERADO con saldo, orden: fechaVencimiento ASC, fechaIngreso ASC
    3 MovimientoService.registrarSalida(...) por cada asignación
        → Stock.descontar() con @Version  → KardexService.registrar()
    4 actualiza cantidades atendidas y estado de la requisición
    5 publica StockActualizadoEvent → AlertaService evalúa stock bajo
  ← 201 DespachoResponse (documento, líneas, lotes, costos)
```

### 4.2 Recepción contra orden de compra

```
POST /api/v1/recepciones
  1 valida OC APROBADA y cantidades ≤ pendiente por línea
  2 control de calidad: registro sanitario vigente · vida útil remanente ≥ mínima ·
    cadena de frío (temperatura) · integridad de empaque
  3 CONFORME → lote LIBERADO (o CUARENTENA si el producto lo exige) + ingreso de inventario
    OBSERVADO → ingreso al almacén de CUARENTENA, pendiente de liberación del QF
    RECHAZADO → sin ingreso, observación obligatoria
  4 recalcula costo promedio ponderado del producto y actualiza estado de la OC
```

### 4.3 Costo promedio ponderado (CPP)

En cada ingreso: `CPP' = (saldo × CPP + cantidad × costoUnitario) / (saldo + cantidad)`.
Las salidas se valorizan al CPP vigente. El kardex guarda `costoUnitario`, `costoPromedio` y
`valorSaldo` por línea y es de **solo inserción**: una corrección se hace con un movimiento
inverso, nunca con `UPDATE`.

## 5. Calidad y entrega

- **Pruebas:** unitarias (JUnit 5 + Mockito) sobre FEFO, CPP, validación de stock y máquinas de
  estado; de integración (`@SpringBootTest` + Testcontainers PostgreSQL 17) sobre los endpoints
  críticos; `ng test` para servicios y componentes reutilizables.
- **CI:** `.github/workflows/ci.yml` ejecuta `mvn -B verify`, `npm ci && npm run build` y
  `npm audit --omit=dev`.
- **Entorno local:** `docker-compose up` levanta PostgreSQL 17, backend y frontend (Nginx).
- **Configuración:** perfiles `dev` / `prod`; sin secretos en el repositorio (`JWT_SECRET`,
  `DB_PASSWORD` por variable de entorno); `application-prod.yml` con `ddl-auto: validate`.
