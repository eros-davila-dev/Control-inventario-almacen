# SGAI-CID · Sistema automatizado de registro y control de almacén

Sistema de gestión de almacén para **CORPORACIÓN INTEGRAL DE DIÁLISIS S.A.C.
(Nipromed)**, empresa del sector salud dedicada a terapia de hemodiálisis
(RUC 20605165410, San Isidro, Lima).

El sistema sustituye el control en hojas de cálculo por un registro
transaccional con trazabilidad por lote, control de vencimientos con FEFO,
cuarentena sanitaria, kardex valorizado y alertas, de acuerdo con el plan de
prácticas preprofesionales *"Implementación de un sistema automatizado para el
registro y control de almacén en una empresa del sector salud, Lima 2026"*.

| | |
|---|---|
| **Gestión del proyecto** | PMI · 5 grupos de procesos |
| **Desarrollo** | Scrum · 6 sprints de 2 semanas (12 semanas) |
| **Arquitectura** | Monolito modular por dominio (backend) · Angular standalone con *signals* (frontend) |

## El problema que resuelve

| | Situación inicial | Meta del sistema |
|---|---|---|
| Exactitud del inventario (ERI) | 72 % | 98 % |
| Merma por vencimiento | 3,1 % del valor comprado | ≤ 1 % |
| Quiebres de insumos críticos | 6 a 9 por mes | ≤ 1 por mes |
| Cierre del kardex | 8 a 10 días | ≤ 1 día |
| Trazabilidad por lote | Inexistente | Total: recepción → despacho → paciente |

El detalle de los problemas P1–P9, los objetivos OE1–OE6 y su línea base está
en [`docs/01-PLAN-DE-IMPLEMENTACION.md`](docs/01-PLAN-DE-IMPLEMENTACION.md).

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 · Spring Security 6 (JWT) · Spring Data JPA · MapStruct · Flyway |
| Base de datos | PostgreSQL 17 (esquema `sgai`, 37 tablas) |
| Frontend | Angular 20 (standalone, *signals*, *zoneless*) · Tailwind CSS 4 (claro/oscuro) |
| Calidad | JUnit 5 · Mockito · JaCoCo · Jasmine/Karma · GitHub Actions |
| Despliegue | Docker Compose · Nginx sin privilegios |

## Módulos

| Módulo | Qué resuelve |
|---|---|
| **Seguridad** | Autenticación JWT, 7 roles, 43 permisos granulares, alcance por almacén, bitácora de auditoría |
| **Catálogo** | Productos con registro sanitario, condición de almacenamiento, vida útil mínima y parámetros de reposición |
| **Almacenes** | Sedes, zonas y ubicaciones (cuarentena, refrigerado, cuarentena de rechazos) |
| **Inventario** | Lotes y vencimientos, saldos por ubicación, movimientos, kardex valorizado con costo promedio ponderado, transferencias entre sedes |
| **Compras** | Reposición sugerida, requerimientos, órdenes de compra y recepción con control de calidad sanitario |
| **Despacho** | Requisiciones de las áreas y despacho con asignación FEFO |
| **Conteo** | Tomas de inventario ciegas (total, por categoría o cíclica ABC), ERI y ajustes |
| **Alertas** | Vencimientos, quiebres, sobre stock, lotes retenidos y registros sanitarios por vencer |
| **Reportes** | Vencimientos, valorización, consumo por área, rotación, inmovilizado y reposición, con exportación CSV |

## Reglas de negocio propias del sector salud

- **FEFO obligatorio**: la salida se asigna por fecha de vencimiento más
  próxima; desviarse exige justificación y queda registrado.
- **Cuarentena y liberación**: lo recibido ingresa en cuarentena y solo el
  Químico Farmacéutico lo libera para consumo.
- **Cadena de frío**: los productos refrigerados exigen temperatura de llegada
  entre 2 °C y 8 °C; fuera de rango la recepción se rechaza.
- **Registro sanitario**: no se recibe mercadería de un producto con registro
  sanitario vencido.
- **Vida útil mínima**: se rechaza el lote que llega con menos vida útil que la
  exigida al proveedor.
- **Kardex inmutable**: los movimientos no se editan; se anulan con un
  movimiento inverso que conserva la historia.
- **Segregación de funciones**: quien registra un documento no puede aprobarlo.

## Puesta en marcha con Docker

```bash
cp .env.example .env          # y reemplace las credenciales
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Aplicación web | http://localhost:8081 |
| API | http://localhost:8080/api/v1 |
| Documentación de la API (OpenAPI) | http://localhost:8080/swagger-ui.html |

## Desarrollo local

**Base de datos**

```bash
docker run -d --name sgai-pg -p 5432:5432 \
  -e POSTGRES_DB=sgai -e POSTGRES_USER=sgai -e POSTGRES_PASSWORD=sgai \
  postgres:17-alpine
```

**Backend** (http://localhost:8080)

```bash
cd backend
export SGAI_JWT_SECRET="$(openssl rand -base64 64)"
mvn spring-boot:run
```

Flyway crea el esquema y carga los datos semilla: permisos, roles, usuarios,
unidades de medida, categorías, almacenes, ubicaciones, proveedores y 33
productos reales de hemodiálisis.

**Frontend** (http://localhost:4200, con proxy a la API)

```bash
cd frontend
npm ci
npm start
```

## Pruebas

```bash
cd backend  && mvn verify                    # 44 pruebas (dominio + integración)
cd frontend && npm test -- --watch=false     # 25 pruebas (servicios, pipes, utilidades)
```

`mvn verify` genera el reporte de cobertura en `backend/target/site/jacoco`.

Además, `backend/src/test/resources/prueba-funcional-e2e.py` recorre los flujos
completos (seguridad, costo promedio, FEFO, compras con control de calidad,
transferencias, conteo físico, anulaciones, alertas y autorización por almacén)
contra una instancia en ejecución: 73 verificaciones.

## Usuarios iniciales

Todos nacen con **cambio de contraseña obligatorio** en el primer acceso; las
claves temporales están en la migración `V2__datos_semilla_seguridad.sql`.

| Usuario | Rol | Qué puede hacer |
|---|---|---|
| `admin` | ADMIN | Todo, incluida la administración de usuarios y roles |
| `jefe.almacen` | JEFE_ALMACEN | Aprueba requerimientos, tomas de inventario y transferencias |
| `almacenero.central` | ALMACENERO | Recepción, despacho, conteo y movimientos del almacén central |
| `almacenero.sjl` | ALMACENERO | Lo mismo, limitado a la sede San Juan de Lurigancho |
| `qf.farmacia` | QUIMICO_FARMACEUTICO | Libera lotes de cuarentena y retiene lotes observados |
| `compras` | COMPRAS | Emite órdenes de compra y hace seguimiento a proveedores |
| `enfermeria.sjl` | SOLICITANTE | Registra requisiciones de su área |
| `auditoria` | AUDITOR | Consulta y bitácora, sin permisos de escritura |

> En producción estos usuarios deben recrearse con credenciales propias de la
> empresa. El secreto JWT y las contraseñas de base de datos se inyectan por
> variables de entorno: el repositorio no contiene credenciales reales.

## Estructura

```
backend/            API Spring Boot (Java 21)
  src/main/java/pe/cid/sgai/
    shared/         Base de entidades, errores, auditoría, correlativos
    seguridad/      Usuarios, roles, permisos, JWT
    catalogo/       Productos, categorías, unidades, proveedores
    almacen/        Almacenes y ubicaciones
    inventario/     Lotes, stock, movimientos, kardex, FEFO, transferencias
    compras/        Requerimientos, órdenes de compra, recepciones
    despacho/       Requisiciones y despachos
    conteo/         Tomas de inventario físico
    alertas/        Reglas de alerta y tareas programadas
    reportes/       Tablero y reportes de gestión
  src/main/resources/db/migration/   Migraciones Flyway
frontend/           Aplicación Angular 20
  src/app/core/     HTTP, interceptores, autenticación, guards, layout
  src/app/shared/   Componentes ui-*, servicios y pipes reutilizables
  src/app/features/ Pantallas por módulo
docs/               Documentación del proyecto
```

Cada módulo del backend sigue la misma separación interna:
`domain → repository → application → web`.

## Documentación

| Documento | Contenido |
|---|---|
| [01 · Plan de implementación](docs/01-PLAN-DE-IMPLEMENTACION.md) | Problemática, objetivos, alcance, épicas, hoja de ruta de 6 sprints y riesgos |
| [02 · Requerimientos](docs/02-REQUERIMIENTOS.md) | Roles, matriz de permisos, 45 historias de usuario con criterios de aceptación y requisitos no funcionales |
| [03 · Arquitectura](docs/03-ARQUITECTURA.md) | Decisiones, capas, patrones y convenciones |
| [04 · Modelo de datos](docs/04-MODELO-DE-DATOS.md) | Tablas, índices y reglas de integridad |
| [05 · Seguridad](docs/05-SEGURIDAD.md) | Autenticación, autorización, política de contraseñas y auditoría |
| [06 · Product backlog](docs/06-PRODUCT-BACKLOG.md) | Backlog estimado (220 puntos) y backlog por sprint |
| [07 · Manual de usuario](docs/07-MANUAL-DE-USUARIO.md) | Operación diaria del sistema por rol |
