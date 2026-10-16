# Seguridad — SGAI-CID

El sistema maneja información de abastecimiento clínico (insumos y medicamentos usados en
hemodiálisis). No almacena datos clínicos ni identificables de pacientes; el consumo se registra por
**área y centro de costo**, no por paciente, para no incorporar datos sensibles de salud
(Ley 29733 de Protección de Datos Personales).

## 1. Autenticación

| Aspecto | Implementación |
|---|---|
| Esquema | JWT **Bearer** firmado `HS512`; secreto de ≥ 64 bytes por variable de entorno `SGAI_JWT_SECRET` (la aplicación no arranca en `prod` sin él) |
| Access token | 15 min · claims: `sub`, `uid`, `roles`, `permisos`, `almacenes`, `jti`, `iat`, `exp`, `iss=sgai-cid` |
| Refresh token | 8 h · opaco (UUID de 256 bits), **se almacena solo su hash SHA-256**, con rotación en cada uso y revocación del anterior (detección de reuso ⇒ se revocan todos los tokens del usuario) |
| Login transitivo | El frontend renueva el access token de forma transparente: el interceptor captura `401`, encola las peticiones en vuelo, llama a `POST /auth/refresh` una sola vez y las reintenta; el usuario nunca vuelve a la pantalla de login mientras el refresh sea válido |
| Contraseñas | BCrypt coste 12; política: ≥ 10 caracteres con mayúscula, minúscula, dígito y símbolo; sin reutilizar las 3 últimas; cambio obligatorio en el primer acceso |
| Fuerza bruta | 5 intentos fallidos en 15 min ⇒ bloqueo de 15 min; todo intento (exitoso o no) se registra con IP y `user-agent`; respuesta genérica `Credenciales inválidas` para no revelar usuarios |
| Cierre de sesión | `POST /auth/logout` revoca el refresh token; el frontend limpia memoria y almacenamiento |
| Almacenamiento en cliente | Access token en memoria (signal) y refresh token en `localStorage` con marca de expiración; sin tokens en URL ni en `sessionStorage` compartido |

## 2. Autorización

Modelo **RBAC con permisos granulares**: `usuario → roles → permisos`, con el permiso como
autoridad de Spring Security (`modulo:recurso:accion`). El rol nunca se consulta directamente en el
código de negocio; siempre el permiso.

```java
@PreAuthorize("hasAuthority('inventario:movimiento:registrar')")
public MovimientoResponse registrarIngreso(IngresoRequest req) { … }
```

- **Alcance por almacén:** además del permiso, `AlmacenAccessService.verificarAcceso(almacenId)`
  valida que el usuario tenga el almacén asignado (salvo `ADMIN`/`JEFE_ALMACEN`), devolviendo
  `403 ALMACEN_NO_AUTORIZADO`. Evita que el almacenero de una sede mueva stock de otra.
- **Segregación de funciones:** quien crea una orden de compra no puede aprobarla; quien ejecuta un
  conteo no aprueba su ajuste.
- **Defensa en profundidad:** la UI oculta acciones con la directiva `*hasPermiso`, pero la
  decisión real siempre es del servidor.
- **Cambio de contraseña obligatorio:** un filtro bloquea cualquier endpoint distinto de
  `/auth/**`, `/perfil` y `/perfil/password` cuando `debeCambiarPassword` es verdadero.

## 3. Protección de la API

| Control | Detalle |
|---|---|
| Validación de entrada | Bean Validation en todos los `record` de request; `@Validated` en servicios; rechazo de campos desconocidos (`FAIL_ON_UNKNOWN_PROPERTIES`) |
| Inyección SQL | Exclusivamente JPA/JPQL con parámetros y `Specification`; cero concatenación de SQL |
| XSS | API JSON sin HTML; Angular escapa por defecto y no se usa `innerHTML` con datos del servidor; CSP restrictiva en Nginx |
| CSRF | No aplica con JWT en cabecera `Authorization` y sesión `STATELESS`; sin cookies de sesión |
| CORS | Orígenes explícitos por perfil (`SGAI_CORS_ORIGINS`), métodos y cabeceras limitados, `maxAge` 1 h |
| Cabeceras | `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, `Content-Security-Policy` |
| Rate limiting | Bucket por IP+usuario en `/auth/login` (10/min) y límite global por IP en escritura (120/min) |
| Tamaño de carga | `spring.servlet.multipart.max-file-size` y límite de `max-http-request-header-size` |
| Fuga de información | `ProblemDetail` sin stack trace; `server.error.include-*: never`; los errores 5xx se registran con `traceId` y el cliente solo recibe el identificador |
| Enumeración de recursos | Respuestas `404` uniformes y verificación de pertenencia antes de responder |
| Idempotencia | `Idempotency-Key` obligatoria en los POST que mueven inventario |
| Dependencias | `mvn versions:display-dependency-updates`, `npm audit` en CI; build falla con vulnerabilidad alta |

## 4. Trazabilidad y auditoría

1. Toda entidad registra `created_by/created_at/updated_by/updated_at` (`AuditorAware` toma el
   usuario del `SecurityContext`).
2. El aspecto `@Auditado` escribe en `auditoria_log` acción, entidad, id, detalle `jsonb`, IP y
   fecha. La tabla es de solo inserción y se consulta con permiso `auditoria:log:leer`.
3. `kardex` es de solo inserción: una corrección se hace con movimiento inverso enlazado, de modo
   que el historial es reconstruible y a prueba de manipulación.
4. Los cambios de estado de lote (liberación, retención, rechazo) exigen motivo y quedan con el
   usuario responsable — requisito de inspección sanitaria.

## 5. Operación

- Secretos por variables de entorno/gestor de secretos; `.env` fuera del repositorio
  (`.gitignore`), con `.env.example` sin valores reales.
- TLS terminado en el reverse proxy; HTTP redirige a HTTPS.
- Usuario de base de datos de la aplicación sin privilegios de DDL en producción (Flyway corre con
  usuario de migración separado); `ddl-auto: validate`.
- Respaldo diario `pg_dump` cifrado con retención de 30 días y prueba de restauración mensual.
- Actuator expuesto solo en `/actuator/health` y `/actuator/info` sin detalles; el resto requiere
  autenticación y el puerto de gestión no se publica.
- Registro de logs sin contraseñas, tokens ni cabeceras `Authorization` (filtro de enmascarado).

## 6. Verificación

| Prueba | Alcance |
|---|---|
| Unitaria | Política de contraseñas, bloqueo por intentos, emisión/validación de JWT, rotación de refresh |
| Integración | Cada endpoint sin token ⇒ `401`; con token sin permiso ⇒ `403`; con almacén ajeno ⇒ `403`; token expirado ⇒ `401` |
| Manual | Lista de verificación OWASP ASVS nivel 1 antes del cierre del Sprint 6 |
