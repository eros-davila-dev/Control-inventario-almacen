# Manual de usuario — SGAI-CID

Manual de operación del sistema de registro y control de almacén de
CORPORACIÓN INTEGRAL DE DIÁLISIS S.A.C. (Nipromed). Está organizado por tarea,
no por pantalla: cada sección indica quién la ejecuta, qué valida el sistema y
qué registra.

Las historias de usuario referenciadas (HU) están en
[`02-REQUERIMIENTOS.md`](02-REQUERIMIENTOS.md).

---

## 1. Acceso al sistema

### 1.1 Iniciar sesión

1. Abra la dirección del sistema en el navegador (equipo de almacén, tablet o
   teléfono: la interfaz se adapta a cualquier tamaño de pantalla).
2. Escriba su **usuario** y **contraseña** y pulse *Ingresar*.
3. En el primer acceso el sistema **obliga a cambiar la contraseña**. Debe
   tener al menos 10 caracteres con mayúscula, minúscula, número y símbolo, y
   no puede repetir ninguna de las 3 últimas.

**La sesión se mantiene al recargar la página.** No es necesario volver a
escribir las credenciales al cerrar y reabrir el navegador durante la jornada:
el sistema renueva la sesión solo. Si estuvo inactivo mucho tiempo, pedirá
ingresar de nuevo.

**Intentos fallidos.** Tras 5 intentos fallidos en 15 minutos el usuario queda
bloqueado 15 minutos. Si ocurre, espere o solicite a TI el desbloqueo.

### 1.2 Elementos permanentes de la pantalla

| Elemento | Para qué sirve |
|---|---|
| **Selector de almacén** (arriba) | Define el almacén de trabajo. Todas las pantallas operativas (saldos, movimientos, despacho, conteo) filtran por él. Solo aparecen los almacenes que tiene autorizados. |
| **Campana de alertas** | Número de alertas abiertas; lleva al centro de alertas. |
| **Claro / oscuro** | Cambia el tema. La preferencia se recuerda en el equipo. |
| **Menú de usuario** | Perfil, cambio de contraseña y cierre de sesión. |
| **Menú lateral** | Módulos. En teléfono se convierte en una barra inferior con los accesos más usados. |

Solo verá los módulos y botones que sus permisos habilitan: si una opción no
aparece, su rol no la tiene asignada.

---

## 2. Tablero (todos los roles con acceso)

El tablero reúne los indicadores del almacén seleccionado:

- Valor del inventario y número de ítems con saldo.
- Ítems bajo mínimo y agotados.
- Lotes por vencer a 30, 60 y 90 días, lotes vencidos y **valor expuesto**.
- Pendientes: requisiciones por atender, órdenes por recibir, transferencias en
  tránsito y tomas de inventario en proceso.
- Desviaciones FEFO de los últimos 30 días.
- Valorización por categoría, consumo mensual y productos de mayor rotación.

Úselo como primera pantalla del día: indica dónde hay que actuar antes de
abrir cada módulo.

---

## 3. Recepción de mercadería (ALMACENERO) — HU-30, HU-31

El momento más delicado del proceso: aquí nace el lote y su trazabilidad.

**Ruta:** *Abastecimiento → Recepciones → Registrar recepción*

1. **Seleccione la orden de compra** recibida. Solo se listan las órdenes
   aprobadas o con recepción parcial pendiente.
2. Registre **guía de remisión**, **factura** y **transportista**.
3. Si la orden incluye productos de cadena de frío, el sistema lo advierte y
   exige la **temperatura de llegada**. Fuera del rango 2 °C – 8 °C la
   recepción debe registrarse como rechazada.
4. Marque si el **empaque** llegó íntegro. Un empaque dañado no puede
   registrarse como conforme.
5. Elija el **resultado del control de calidad**:
   - *Conforme*: la mercadería ingresa a **cuarentena**, a la espera de la
     liberación del Químico Farmacéutico.
   - *Observado*: ingresa con observación (obligatoria).
   - *Rechazado*: **no ingresa** al almacén; queda el acta para el reclamo al
     proveedor.
6. Por cada línea pendiente registre:
   - **Cantidad realmente recibida** (puede ser menor a lo pedido: el saldo
     queda pendiente en la orden y permite una segunda recepción).
   - **Lote** y **fecha de vencimiento**, obligatorios en los productos que lo
     controlan. La pantalla avisa si el vencimiento no alcanza la vida útil
     mínima exigida al proveedor.
7. Confirme. El sistema genera el movimiento de ingreso en el kardex y
   actualiza el costo promedio del producto.

**Lo que el sistema rechaza y por qué**

| Mensaje | Causa |
|---|---|
| `REGISTRO_SANITARIO_VENCIDO` | El producto tiene el registro sanitario vencido: no puede recibirse. |
| `VIDA_UTIL_INSUFICIENTE` | El lote llega con menos vida útil que la mínima exigida. |
| `CALIDAD_RECHAZADA` | Temperatura fuera de 2–8 °C o empaque no íntegro declarado como conforme. |
| `PRODUCTO_REQUIERE_LOTE` | Falta el lote o la fecha de vencimiento de un producto que los controla. |
| `CANTIDAD_EXCEDE_PENDIENTE` | La cantidad supera lo que queda pendiente en la orden. |

---

## 4. Liberación de lotes (QUÍMICO FARMACÉUTICO) — HU-12, HU-13

**Ruta:** *Inventario → Lotes y vencimientos*

Filtre por estado **Cuarentena** para ver lo pendiente de su revisión.

- **Liberar**: el lote pasa a estado *Liberado* y queda disponible para
  despacho. Mientras esté en cuarentena **no se puede despachar**.
- **Retener**: inmoviliza el lote (sospecha de calidad, alerta sanitaria,
  reclamo). Exige motivo y bloquea su salida de inmediato.
- **Rechazar**: el lote se descarta y no vuelve a circular.

Un lote que alcanza su fecha de vencimiento pasa a *Vencido* automáticamente y
el sistema impide despacharlo.

---

## 5. Despacho a las áreas y sedes

### 5.1 Registrar una requisición (SOLICITANTE) — HU-32

**Ruta:** *Atención a sedes → Requisiciones → Nueva requisición*

1. Indique el **área** y el **centro de costo** que consumirá los insumos.
2. Agregue los productos y las cantidades necesarias; el buscador muestra el
   saldo disponible de cada uno.
3. Marque la **prioridad** (normal o urgente) y envíe. La requisición queda a
   la espera de atención por el almacén.

### 5.2 Atender y despachar (ALMACENERO) — HU-33, HU-34

**Ruta:** *Atención a sedes → Requisiciones* → *Preparar despacho*

1. El sistema propone los **lotes según FEFO**: primero los que vencen antes.
2. Revise la propuesta. Puede modificar el lote, pero el sistema **exige
   justificación** y registra la desviación FEFO para su revisión posterior.
3. Confirme el despacho. Queda registrado:
   - la salida en el kardex, valorizada al costo promedio;
   - qué lote fue a qué área (**trazabilidad completa**);
   - la atención total o parcial de la requisición.

Si lo disponible no alcanza, el despacho puede ser **parcial**: la requisición
queda en atención parcial con el saldo pendiente.

---

## 6. Transferencias entre sedes (ALMACENERO / JEFE DE ALMACÉN) — HU-20, HU-21

**Ruta:** *Inventario → Transferencias*

1. **Enviar**: seleccione almacén origen y destino, los lotes y las cantidades.
   El stock sale del origen y queda **en tránsito**: no está disponible en
   ninguno de los dos almacenes.
2. **Recibir** en el destino: registre la cantidad efectivamente recibida.
   - Si coincide, el stock ingresa completo.
   - Si es menor, el sistema registra la **diferencia en tránsito** para su
     investigación; nada se pierde sin quedar documentado.

---

## 7. Abastecimiento (COMPRAS / JEFE DE ALMACÉN)

### 7.1 Reposición sugerida y requerimientos — HU-26, HU-27

**Ruta:** *Abastecimiento → Requerimientos*

La pestaña **Reposición sugerida** muestra, para el almacén seleccionado, los
productos bajo su punto de reorden con: disponible, parámetros (mínimo,
reorden, máximo), consumo promedio diario y **cobertura en días** (en rojo si
es menor a 15). La cantidad a comprar la calcula el sistema.

- *Generar desde sugerencia* crea un requerimiento con todas las cantidades
  propuestas, editables antes de enviarlo.
- *Nuevo requerimiento* permite armarlo producto por producto.

El flujo es: **Borrador → Enviado → Aprobado** (o Rechazado con motivo).
Quien registra el requerimiento no puede aprobarlo.

### 7.2 Órdenes de compra — HU-28, HU-29

**Ruta:** *Abastecimiento → Órdenes de compra → Nueva orden*

1. Seleccione el **requerimiento aprobado**: sus líneas se cargan solas y queda
   la trazabilidad necesidad → compra.
2. Elija **proveedor**, **almacén destino**, fecha esperada de entrega,
   condición de pago y moneda.
3. Complete el **precio unitario** de cada línea. El sistema calcula subtotal,
   IGV (18 %) y total.
4. Emita la orden. Nace en **Borrador** y requiere aprobación.

En el listado puede aprobar, rechazar o anular una orden (con motivo), y el
**detalle** muestra lo pedido, lo recibido y lo pendiente por línea, junto con
las recepciones registradas. La fecha de entrega se marca en rojo cuando está
vencida.

---

## 8. Inventario físico (ALMACENERO ejecuta, JEFE DE ALMACÉN aprueba) — HU-36 a HU-39

**Ruta:** *Control → Inventario físico*

### 8.1 Programar

*Programar toma* con el alcance:

| Alcance | Cuándo usarlo |
|---|---|
| **Total** | Inventario general del almacén (cierre de ejercicio). |
| **Por categoría** | Verificación de una familia de insumos. |
| **Cíclico ABC** | Rutina: clase A mensual, B trimestral, C semestral. |

### 8.2 Contar (conteo ciego)

Al *Iniciar*, el sistema **congela el saldo teórico** de cada producto, lote y
ubicación, y **lo oculta**: la hoja de conteo no muestra lo que el sistema
cree que hay. Así el conteo no se "cuadra" contra el registro.

1. Recorra las ubicaciones y escriba la **cantidad contada** de cada línea. El
   filtro por producto, lote o ubicación ayuda a seguir el recorrido físico.
2. Use el **segundo conteo** para verificar lo que no cuadró a la primera.
3. *Guardar avance* conserva lo registrado; puede contarse en varias jornadas.
4. *Cerrar conteo* revela el saldo del sistema, las **diferencias** y el
   **ERI** (exactitud del registro de inventario). Lo que quede sin contar se
   toma como cero y genera diferencia.

### 8.3 Aprobar

El Jefe de Almacén revisa las diferencias y aprueba. La aprobación **genera los
ajustes de inventario** en el kardex con motivo *conteo físico*. Quien ejecutó
el conteo no puede aprobarlo.

El **ERI** de cada toma es el indicador del objetivo institucional: llevar la
exactitud del 72 % al 98 %.

---

## 9. Centro de alertas — HU-40 a HU-43

**Ruta:** *Gestión → Alertas*

El sistema evalúa las reglas cada madrugada y además cuando el stock cambia.
Cada alerta trae el **acceso directo a la pantalla donde se corrige**:

| Alerta | Acción propuesta |
|---|---|
| Stock agotado / bajo | Generar reposición |
| Por vencer | Priorizar salida (FEFO) |
| Vencido | Retirar del almacén y dar de baja |
| Lote retenido | Revisar lote |
| Sobre stock / sin movimiento | Evaluar rotación |
| Registro sanitario por vencer | Actualizar registro del producto |

Las alertas se **marcan como leídas** (vistas, aún abiertas) o se **resuelven**
(el problema se atendió). *Recalcular ahora* fuerza la reevaluación sin esperar
a la madrugada.

---

## 10. Consultas e informes

### 10.1 Saldos y kardex (todos los roles de consulta)

| Pantalla | Qué responde |
|---|---|
| *Inventario → Saldos* | Cuánto hay, cuánto está reservado y cuánto disponible, por producto y ubicación, con su valorización. |
| *Inventario → Movimientos* | Todo ingreso, salida, ajuste y transferencia, con su documento de origen. |
| *Inventario → Lotes y vencimientos* | Estado de cada lote, su vencimiento y su saldo. |
| *Inventario → Kardex valorizado* | Historia de un producto con saldo y costo promedio corrido, exportable a CSV. |

### 10.2 Reportes de gestión (JEFE DE ALMACÉN, COMPRAS, AUDITOR) — HU-44, HU-45

**Ruta:** *Gestión → Reportes*

| Reporte | Para qué |
|---|---|
| **Vencimientos** | Lotes por vencer y vencidos con el valor expuesto; base del indicador de merma. Ventana configurable (30 a 180 días). Exportable. |
| **Valorización** | Valor del inventario por categoría y su participación; cierre contable. |
| **Consumo por área** | Lo despachado a cada área y centro de costo, valorizado. Exportable. |
| **Rotación** | Consumo acumulado por producto; orienta la clasificación ABC y los parámetros de reposición. |
| **Inmovilizado** | Saldo sin movimiento: capital detenido y riesgo de obsolescencia. |
| **Reposición** | Productos bajo el punto de reorden con su cobertura en días. |

Los archivos CSV se descargan con separador `;` y decimales con coma: se abren
directamente en Excel en español.

### 10.3 Auditoría (AUDITOR / ADMIN)

*Control → Auditoría* registra quién hizo qué y cuándo: operación, entidad
afectada, usuario, fecha y dirección IP. No se puede modificar ni borrar.

---

## 11. Administración (ADMIN)

| Tarea | Ruta |
|---|---|
| Crear usuarios, asignar roles y **almacenes autorizados** | *Administración → Usuarios* |
| Revisar o ajustar los permisos de cada rol | *Administración → Roles y permisos* |
| Restablecer la contraseña de un usuario | *Administración → Usuarios* → acción de restablecer |
| Mantener productos, categorías, unidades y proveedores | *Maestros* |
| Mantener almacenes y ubicaciones | *Maestros → Almacenes / Ubicaciones* |

Al crear un producto es clave definir: si **controla lote** y **vencimiento**,
su **condición de almacenamiento** (de ella depende la exigencia de cadena de
frío), su **registro sanitario**, su **vida útil mínima** y sus parámetros de
reposición (mínimo, punto de reorden, máximo). De esos datos dependen las
validaciones de recepción, el FEFO y las alertas.

---

## 12. Preguntas frecuentes

**Registré un movimiento con un error, ¿puedo editarlo?**
No. El kardex no se edita: se **anula**, y la anulación genera un movimiento
inverso que deja constancia de ambos hechos. Luego registre el movimiento
correcto.

**El sistema no me deja despachar un lote que veo en el almacén.**
Puede estar en cuarentena (falta la liberación del QF), retenido o vencido.
Revise su estado en *Inventario → Lotes y vencimientos*.

**¿Por qué el sistema me propone un lote y no el que tengo a mano?**
Por FEFO: sale primero lo que vence antes, para evitar merma. Si debe usar otro
lote, el sistema lo permite con justificación y lo registra.

**No veo un almacén en el selector.**
No está entre sus almacenes autorizados. Solicite la asignación a TI.

**Aprobé un documento y el sistema lo rechazó diciendo que no puedo.**
Quien registra un documento no puede aprobarlo (segregación de funciones).
Debe aprobarlo otra persona con el permiso correspondiente.

**¿Puedo trabajar desde el teléfono en el almacén?**
Sí. La interfaz es responsiva: en pantallas pequeñas las tablas se muestran
como tarjetas y el menú pasa a la barra inferior.
