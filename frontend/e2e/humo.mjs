/**
 * Prueba de humo de la interfaz: recorre las pantallas de todos los modulos en
 * escritorio y en telefono, comprueba que cargan con datos del servidor y que
 * no se produce ningun error en consola (RNF-04, responsive y accesibilidad).
 *
 * Requiere el backend y la base de datos en marcha, y la aplicacion servida:
 *   cd backend  && mvn spring-boot:run
 *   cd frontend && npm start
 *   cd frontend && npm run e2e
 *
 * Variables de entorno opcionales:
 *   SGAI_URL        direccion de la aplicacion (por omision http://localhost:4200)
 *   SGAI_USUARIO    usuario de prueba (por omision admin)
 *   SGAI_PASSWORD   contrasena temporal de la semilla
 *   SGAI_PASSWORD_NUEVA  contrasena a fijar si el sistema exige cambiarla
 *   CHROME_BIN      ruta del navegador, si no esta en la ubicacion estandar
 */
import { chromium } from 'playwright';

const BASE = process.env.SGAI_URL ?? 'http://localhost:4200';
const USUARIO = process.env.SGAI_USUARIO ?? 'admin';
const PASSWORD = process.env.SGAI_PASSWORD ?? 'Sgai#2026Admin';
const PASSWORD_NUEVA = process.env.SGAI_PASSWORD_NUEVA ?? 'Inventario#2026';

/** Pantallas a recorrer: ruta y texto que debe aparecer en el encabezado. */
const RUTAS = [
  ['/dashboard', 'Tablero de control'],
  ['/inventario/stock', 'Saldos'],
  ['/inventario/movimientos', 'Movimientos'],
  ['/inventario/lotes', 'Lotes'],
  ['/inventario/kardex', 'Kardex'],
  ['/inventario/transferencias', 'Transferencias'],
  ['/despacho/requisiciones', 'Requisiciones'],
  ['/despacho/despachos', 'Despachos'],
  ['/compras/requerimientos', 'Requerimientos de compra'],
  ['/compras/ordenes', 'Ordenes de compra'],
  ['/compras/recepciones', 'Recepcion de mercaderia'],
  ['/conteo/tomas', 'Inventario fisico'],
  ['/alertas', 'Centro de alertas'],
  ['/reportes', 'Reportes de gestion'],
  ['/catalogo/productos', 'Productos'],
  ['/catalogo/proveedores', 'Proveedores'],
  ['/almacenes', 'Almacenes'],
  ['/seguridad/usuarios', 'Usuarios'],
  ['/seguridad/roles', 'Roles'],
  ['/auditoria', 'Auditoria'],
  ['/perfil', 'perfil'],
];

let ok = 0;
let fallas = 0;
const errores = [];

function check(nombre, condicion, detalle = '') {
  if (condicion) {
    ok += 1;
    console.log(`  OK    ${nombre.padEnd(50)} ${detalle}`);
  } else {
    fallas += 1;
    console.log(`  FALLA ${nombre.padEnd(50)} ${detalle}`);
  }
}

async function ingresar(pagina) {
  await pagina.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await pagina.fill('input[formcontrolname="usuario"]', USUARIO);
  await pagina.fill('input[formcontrolname="password"]', PASSWORD);
  await pagina.click('button[type="submit"]');
  await pagina.waitForTimeout(1500);

  // Primer acceso: el sistema exige cambiar la contrasena temporal.
  if (pagina.url().includes('cambiar-password')) {
    await pagina.fill('input[formcontrolname="passwordActual"]', PASSWORD);
    await pagina.fill('input[formcontrolname="passwordNueva"]', PASSWORD_NUEVA);
    await pagina.fill('input[formcontrolname="confirmacion"]', PASSWORD_NUEVA);
    await pagina.click('button[type="submit"]');
    await pagina.waitForURL(/login/, { timeout: 15000 });
    await pagina.fill('input[formcontrolname="usuario"]', USUARIO);
    await pagina.fill('input[formcontrolname="password"]', PASSWORD_NUEVA);
    await pagina.click('button[type="submit"]');
  } else if (pagina.url().includes('/login')) {
    // La contrasena temporal ya fue cambiada en una ejecucion anterior.
    await pagina.fill('input[formcontrolname="usuario"]', USUARIO);
    await pagina.fill('input[formcontrolname="password"]', PASSWORD_NUEVA);
    await pagina.click('button[type="submit"]');
  }
  await pagina.waitForURL(/dashboard/, { timeout: 20000 });
}

async function recorrer(navegador, etiqueta, ancho, alto) {
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: alto } });
  const pagina = await contexto.newPage();
  // Durante el ingreso puede haber un 401 esperado (la clave temporal ya fue
  // cambiada en una ejecucion anterior): la consola se vigila despues.
  let vigilar = false;
  pagina.on('console', (m) => {
    if (vigilar && m.type() === 'error') {
      errores.push(`${etiqueta}: ${m.text().slice(0, 200)}`);
    }
  });
  pagina.on('pageerror', (e) => {
    if (vigilar) {
      errores.push(`${etiqueta}: ${e.message.slice(0, 200)}`);
    }
  });

  console.log(`\n=== ${etiqueta} (${ancho}x${alto}) ===`);
  await ingresar(pagina);
  vigilar = true;
  check('inicio de sesion y llegada al tablero', pagina.url().includes('/dashboard'));

  const indicador = pagina.getByText('Valor del inventario', { exact: false }).first();
  await indicador.waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
  check('indicadores del tablero cargados', await indicador.isVisible());

  for (const [ruta, texto] of RUTAS) {
    await pagina.goto(BASE + ruta, { waitUntil: 'networkidle' });
    await pagina.waitForTimeout(250);
    const visible = (await pagina.getByText(texto, { exact: false }).count()) > 0;
    check(`pantalla ${ruta}`, visible && !pagina.url().includes('/login'));
  }

  // Sin desplazamiento horizontal: la interfaz debe caber en el ancho del dispositivo.
  await pagina.goto(`${BASE}/inventario/stock`, { waitUntil: 'networkidle' });
  const desborda = await pagina.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1);
  check('la pantalla no desborda horizontalmente', !desborda);

  await pagina.goto(`${BASE}/dashboard`, { waitUntil: 'networkidle' });
  const antes = await pagina.evaluate(() => document.documentElement.classList.contains('dark'));
  await pagina.locator('button[aria-label*="tema" i], button[title*="tema" i]').first().click();
  await pagina.waitForTimeout(300);
  const despues = await pagina.evaluate(() => document.documentElement.classList.contains('dark'));
  check('alterna tema claro y oscuro', antes !== despues, `${antes} -> ${despues}`);

  await contexto.close();
}

const navegador = await chromium.launch({
  ...(process.env.CHROME_BIN ? { executablePath: process.env.CHROME_BIN } : {}),
  args: ['--no-sandbox'],
});

await recorrer(navegador, 'Escritorio', 1440, 900);
await recorrer(navegador, 'Tablet', 834, 1112);
await recorrer(navegador, 'Telefono', 390, 844);

await navegador.close();

console.log('\n=================== RESUMEN ===================');
console.log(`  verificaciones OK: ${ok}   fallas: ${fallas}`);
if (errores.length) {
  console.log('\n  errores de consola del navegador:');
  [...new Set(errores)].slice(0, 15).forEach((e) => console.log(`   - ${e}`));
}
process.exit(fallas > 0 || errores.length > 0 ? 1 : 0);
