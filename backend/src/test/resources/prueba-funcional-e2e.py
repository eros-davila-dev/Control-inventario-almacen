import json, urllib.request, urllib.error, datetime, sys

BASE = "http://localhost:18080/api/v1"
TOKEN = {"v": None}
ok = fail = 0

def llamar(metodo, ruta, cuerpo=None, esperado=None, cabeceras=None):
    url = BASE + ruta
    datos = None if cuerpo is None else json.dumps(cuerpo).encode()
    req = urllib.request.Request(url, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if TOKEN["v"]:
        req.add_header("Authorization", "Bearer " + TOKEN["v"])
    for k, v in (cabeceras or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode("utf-8-sig")
            if not texto:
                return r.status, None
            try:
                return r.status, json.loads(texto)
            except json.JSONDecodeError:
                return r.status, {"texto": texto}
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        try:
            return e.code, json.loads(texto)
        except Exception:
            return e.code, {"raw": texto[:300]}

def check(nombre, condicion, detalle=""):
    global ok, fail
    if condicion:
        ok += 1
        print("  OK   %-58s %s" % (nombre, detalle))
    else:
        fail += 1
        print("  FALLA %-58s %s" % (nombre, detalle))

def login(usuario, password):
    st, d = llamar("POST", "/auth/login", {"usuario": usuario, "password": password})
    if st == 200:
        TOKEN["v"] = d["accessToken"]
    return st, d

hoy = datetime.date.today()
f = lambda dias: (hoy + datetime.timedelta(days=dias)).isoformat()

print("\n=== 1. SEGURIDAD ===")
st, d = login("admin", "Sgai#2026Admin")
check("login admin", st == 200 and d["usuario"]["roles"] == ["ADMIN"])
st, _ = llamar("POST", "/perfil/password", {"passwordActual": "Sgai#2026Admin",
    "passwordNueva": "Inventario#2026", "confirmacion": "Inventario#2026"})
check("cambio de password obligatorio", st == 204)
st, d = login("admin", "Inventario#2026")
check("relogin sin marca de cambio", st == 200 and not d["usuario"]["debeCambiarPassword"])

print("\n=== 2. MAESTROS ===")
st, prods = llamar("GET", "/productos?size=200")
check("catalogo cargado", st == 200 and prods["totalElementos"] >= 30,
      "%s productos" % prods.get("totalElementos"))
pid = {p["codigo"]: p["id"] for p in prods["contenido"]}
st, alms = llamar("GET", "/almacenes/activos")
aid = {a["codigo"]: a["id"] for a in alms}
check("red de almacenes", st == 200 and len(aid) == 7, "%s almacenes" % len(aid))
st, ubis = llamar("GET", "/ubicaciones/almacen/%d" % aid["ALM-CEN"])
uid = {u["codigo"]: u["id"] for u in ubis}
check("ubicaciones del almacen central", len(uid) >= 10, "%s ubicaciones" % len(uid))
st, provs = llamar("GET", "/proveedores/activos")
prov = {p["ruc"]: p["id"] for p in provs}
check("proveedores", len(prov) == 5)
st, d = llamar("POST", "/productos", {"codigo": "DIA-HF18", "nombre": "duplicado",
    "categoriaId": 1, "unidadMedidaId": 1, "tipoProducto": "INSUMO_DIALISIS",
    "condicionAlmacenamiento": "AMBIENTE", "stockMinimo": 1, "puntoReorden": 2, "stockMaximo": 3})
check("codigo de producto duplicado rechazado", st == 409 and d["codigo"] == "DUPLICADO")
st, d = llamar("POST", "/productos", {"codigo": "ZZZ-1", "nombre": "incoherente",
    "categoriaId": 1, "unidadMedidaId": 1, "tipoProducto": "INSUMO_DIALISIS",
    "condicionAlmacenamiento": "AMBIENTE", "stockMinimo": 100, "puntoReorden": 10,
    "stockMaximo": 5})
check("parametros de reposicion incoherentes rechazados", st == 400,
      str(d.get("errores", {}))[:60])

print("\n=== 3. SALDO INICIAL (ingreso con lote y vencimiento) ===")
st, mov = llamar("POST", "/movimientos/ingresos", {
    "almacenId": aid["ALM-CEN"], "tipoMovimiento": "INGRESO_AJUSTE",
    "motivoAjuste": "SALDO_INICIAL", "documentoTipo": "ACTA", "documentoNumero": "ACTA-001",
    "observacion": "Carga de saldo inicial del almacen central",
    "lineas": [
        {"productoId": pid["DIA-HF18"], "codigoLote": "LOTE-A-VENCE-PRONTO",
         "fechaVencimiento": f(45), "ubicacionId": uid["A-01-01"], "cantidad": 100,
         "costoUnitario": 48.5},
        {"productoId": pid["DIA-HF18"], "codigoLote": "LOTE-B-VENCE-LEJOS",
         "fechaVencimiento": f(400), "ubicacionId": uid["A-01-01"], "cantidad": 200,
         "costoUnitario": 52.0},
        {"productoId": pid["LIN-AV01"], "codigoLote": "LIN-2026-01",
         "fechaVencimiento": f(300), "ubicacionId": uid["A-02-01"], "cantidad": 500,
         "costoUnitario": 18.9},
        {"productoId": pid["EPP-GUAN"], "cantidad": 50, "costoUnitario": 32.0,
         "ubicacionId": uid["C-02-01"]}
    ]}, cabeceras={"Idempotency-Key": "saldo-inicial-001"})
check("ingreso registrado", st == 201, mov.get("numero", str(mov)[:80]))
check("numeracion correlativa", st == 201 and mov["numero"].startswith("AJU-"),
      mov.get("numero", ""))
check("valorizado del ingreso", st == 201 and abs(float(mov["valorTotal"]) - (100*48.5+200*52+500*18.9+50*32)) < 0.01,
      str(mov.get("valorTotal")))
st, mov2 = llamar("POST", "/movimientos/ingresos", {
    "almacenId": aid["ALM-CEN"], "tipoMovimiento": "INGRESO_AJUSTE",
    "motivoAjuste": "SALDO_INICIAL", "lineas": [{"productoId": pid["DIA-HF18"],
    "codigoLote": "LOTE-A-VENCE-PRONTO", "fechaVencimiento": f(45), "cantidad": 100,
    "costoUnitario": 48.5}]}, cabeceras={"Idempotency-Key": "saldo-inicial-001"})
check("idempotencia: no duplica el movimiento", mov2["numero"] == mov["numero"],
      mov2.get("numero"))

print("\n=== 4. STOCK, CPP Y KARDEX ===")
st, stock = llamar("GET", "/stock?almacenId=%d&productoId=%d" % (aid["ALM-CEN"], pid["DIA-HF18"]))
total = sum(float(s["cantidad"]) for s in stock["contenido"])
check("saldo por lote", st == 200 and total == 300, "total %s en %s celdas" % (total, len(stock["contenido"])))
st, cons = llamar("GET", "/stock/consolidado?almacenId=%d" % aid["ALM-CEN"])
hf18 = next(c for c in cons if c["productoCodigo"] == "DIA-HF18")
cpp_esperado = (100*48.5 + 200*52.0) / 300
check("costo promedio ponderado", abs(float(hf18["costoPromedio"]) - cpp_esperado) < 0.0001,
      "CPP=%s esperado=%.6f" % (hf18["costoPromedio"], cpp_esperado))
check("semaforo de reposicion", hf18["estadoReposicion"] == "BAJO_MINIMO",
      hf18["estadoReposicion"] + " (disp %s, min %s)" % (hf18["disponible"], hf18["stockMinimo"]))
st, kdx = llamar("GET", "/kardex?productoId=%d&almacenId=%d" % (pid["DIA-HF18"], aid["ALM-CEN"]))
check("kardex con saldo acumulado", st == 200 and float(kdx["contenido"][-1]["saldo"]) == 300,
      "ultimo saldo %s" % kdx["contenido"][-1]["saldo"])

print("\n=== 5. FEFO ===")
st, sug = llamar("GET", "/stock/sugerencia-fefo?productoId=%d&almacenId=%d&cantidad=150"
                 % (pid["DIA-HF18"], aid["ALM-CEN"]))
check("FEFO propone primero el lote que vence antes",
      st == 200 and sug[0]["codigoLote"] == "LOTE-A-VENCE-PRONTO" and float(sug[0]["cantidadSugerida"]) == 100,
      " -> ".join("%s:%s" % (s["codigoLote"], s["cantidadSugerida"]) for s in sug))
check("FEFO reparte el saldo entre lotes", len(sug) == 2 and float(sug[1]["cantidadSugerida"]) == 50)
st, d = llamar("GET", "/stock/sugerencia-fefo?productoId=%d&almacenId=%d&cantidad=9999"
               % (pid["DIA-HF18"], aid["ALM-CEN"]))
check("stock insuficiente detectado", st == 409 and d["codigo"] == "STOCK_INSUFICIENTE",
      "disponible=%s" % d.get("disponible"))

print("\n=== 6. REQUISICION Y DESPACHO ===")
st, req = llamar("POST", "/requisiciones", {
    "almacenSolicitanteId": aid["ALM-SJL"], "almacenAtencionId": aid["ALM-CEN"],
    "centroCosto": "CC-HD-SJL", "area": "Hemodialisis SJL - turno manana",
    "prioridad": "URGENTE", "fechaRequerida": f(1),
    "observacion": "Insumos para 40 sesiones",
    "lineas": [{"productoId": pid["DIA-HF18"], "cantidadSolicitada": 120},
               {"productoId": pid["LIN-AV01"], "cantidadSolicitada": 120}]})
check("requisicion creada", st == 201, req.get("numero"))
st, _ = llamar("POST", "/requisiciones/%d/enviar" % req["id"])
check("requisicion enviada", st == 200)
lineas = {l["productoCodigo"]: l["id"] for l in req["lineas"]}
st, desp = llamar("POST", "/despachos", {
    "requisicionId": req["id"], "entregadoA": "Lic. Miriam Torres",
    "observacion": "Despacho con FEFO automatico",
    "lineas": [{"requisicionLineaId": lineas["DIA-HF18"], "cantidad": 120},
               {"requisicionLineaId": lineas["LIN-AV01"], "cantidad": 100}]})
check("despacho registrado", st == 201, desp.get("numero"))
lotes_despachados = [(l["codigoLote"], float(l["cantidad"])) for l in desp["lineas"]]
check("despacho aplico FEFO (agota el lote proximo a vencer)",
      ("LOTE-A-VENCE-PRONTO", 100.0) in lotes_despachados and ("LOTE-B-VENCE-LEJOS", 20.0) in lotes_despachados,
      str(lotes_despachados))
st, req2 = llamar("GET", "/requisiciones/%d" % req["id"])
check("requisicion atendida parcialmente", req2["estado"] == "ATENDIDA_PARCIAL", req2["estado"])
pend = {l["productoCodigo"]: float(l["pendiente"]) for l in req2["lineas"]}
check("pendiente por linea correcto", pend["DIA-HF18"] == 0 and pend["LIN-AV01"] == 20, str(pend))
st, stock = llamar("GET", "/stock?almacenId=%d&productoId=%d" % (aid["ALM-CEN"], pid["DIA-HF18"]))
check("saldo descontado tras el despacho",
      sum(float(s["cantidad"]) for s in stock["contenido"]) == 180,
      "saldo %s" % sum(float(s["cantidad"]) for s in stock["contenido"]))

print("\n=== 7. CONTROL SANITARIO DE LOTES ===")
st, lotes = llamar("GET", "/lotes?productoId=%d" % pid["DIA-HF18"])
lote_b = next(l for l in lotes["contenido"] if l["codigoLote"] == "LOTE-B-VENCE-LEJOS")
st, d = llamar("PATCH", "/lotes/%d/estado" % lote_b["id"],
               {"estado": "RETENIDO", "motivo": "Alerta sanitaria del fabricante: posible falla de empaque"})
check("lote retenido por el QF", st == 200 and d["estadoLote"] == "RETENIDO")
st, d = llamar("POST", "/despachos", {"requisicionId": req["id"],
    "lineas": [{"requisicionLineaId": lineas["LIN-AV01"], "cantidad": 20, "loteId": lote_b["id"]}]})
check("no se despacha un lote retenido", st == 409 and d["codigo"] == "LOTE_NO_DISPONIBLE",
      d.get("codigo"))
st, traz = llamar("GET", "/lotes/%d/trazabilidad" % lote_b["id"])
check("trazabilidad del lote", st == 200 and len(traz["movimientos"]) >= 2,
      "%s movimientos, %s ubicaciones" % (len(traz["movimientos"]), len(traz["ubicacionesActuales"])))
st, d = llamar("PATCH", "/lotes/%d/liberar" % lote_b["id"],
               {"estado": "LIBERADO", "motivo": "Descartada la alerta por el fabricante"})
check("lote liberado nuevamente", st == 200 and d["estadoLote"] == "LIBERADO")

print("\n=== 8. ABASTECIMIENTO ===")
st, sug = llamar("GET", "/requerimientos-compra/sugerencia/%d" % aid["ALM-CEN"])
check("sugerencia de reposicion calculada", st == 200 and len(sug) >= 1,
      "%s productos sugeridos" % len(sug))
st, rq = llamar("POST", "/requerimientos-compra", {"almacenId": aid["ALM-CEN"],
    "justificacion": "Reposicion de insumos criticos",
    "lineas": [{"productoId": pid["DIA-HF18"], "cantidadSolicitada": 500}]})
check("requerimiento creado", st == 201, rq.get("numero"))
llamar("POST", "/requerimientos-compra/%d/enviar" % rq["id"])
st, _ = llamar("POST", "/requerimientos-compra/%d/aprobar" % rq["id"])
check("requerimiento aprobado (ADMIN exento de segregacion)", st == 200)
st, oc = llamar("POST", "/ordenes-compra", {"proveedorId": prov["20100334455"],
    "requerimientoId": rq["id"], "almacenDestinoId": aid["ALM-CEN"], "fechaEsperada": f(10),
    "lineas": [{"productoId": pid["DIA-HF18"], "cantidad": 500, "precioUnitario": 50.0}]})
check("orden de compra creada", st == 201, oc.get("numero"))
check("totales con IGV calculados en servidor",
      float(oc["subtotal"]) == 25000.0 and float(oc["igv"]) == 4500.0 and float(oc["total"]) == 29500.0,
      "sub %s igv %s total %s" % (oc["subtotal"], oc["igv"], oc["total"]))
st, d = llamar("POST", "/recepciones", {"ordenId": oc["id"], "resultadoCalidad": "CONFORME",
    "guiaRemision": "001-0099", "lineas": [{"ordenLineaId": oc["lineas"][0]["id"],
    "cantidad": 100, "codigoLote": "OC-LOTE-1", "fechaVencimiento": f(600)}]})
check("no se recibe una OC sin aprobar", st == 409 and d["codigo"] == "ESTADO_INVALIDO", d.get("codigo"))
st, _ = llamar("POST", "/ordenes-compra/%d/aprobar" % oc["id"])
check("orden aprobada", st == 200)
st, d = llamar("POST", "/recepciones", {"ordenId": oc["id"], "resultadoCalidad": "CONFORME",
    "guiaRemision": "001-0100", "lineas": [{"ordenLineaId": oc["lineas"][0]["id"],
    "cantidad": 100, "codigoLote": "OC-LOTE-CORTO", "fechaVencimiento": f(30)}]})
check("rechaza vida util remanente insuficiente",
      st == 422 and d["codigo"] == "VIDA_UTIL_INSUFICIENTE", d.get("codigo"))
st, d = llamar("POST", "/recepciones", {"ordenId": oc["id"], "resultadoCalidad": "CONFORME",
    "guiaRemision": "001-0101", "lineas": [{"ordenLineaId": oc["lineas"][0]["id"],
    "cantidad": 600, "codigoLote": "OC-LOTE-1", "fechaVencimiento": f(600)}]})
check("rechaza recibir mas de lo pedido",
      st == 409 and d["codigo"] == "CANTIDAD_EXCEDE_PENDIENTE", d.get("codigo"))
st, rec = llamar("POST", "/recepciones", {"ordenId": oc["id"], "resultadoCalidad": "CONFORME",
    "guiaRemision": "001-0102", "ubicacionId": uid["A-01-02"],
    "lineas": [{"ordenLineaId": oc["lineas"][0]["id"], "cantidad": 300,
                "codigoLote": "OC-LOTE-1", "fechaVencimiento": f(600)}]})
check("recepcion parcial conforme", st == 201, rec.get("numero"))
st, oc2 = llamar("GET", "/ordenes-compra/%d" % oc["id"])
check("orden en recepcion parcial", oc2["estado"] == "RECIBIDA_PARCIAL",
      "%s (recibido %s de %s)" % (oc2["estado"], oc2["lineas"][0]["cantidadRecibida"],
                                  oc2["lineas"][0]["cantidad"]))
st, lotes = llamar("GET", "/lotes?productoId=%d&texto=OC-LOTE-1" % pid["DIA-HF18"])
lote_oc = lotes["contenido"][0]
check("lote de compra entra en cuarentena", lote_oc["estadoLote"] == "CUARENTENA",
      lote_oc["estadoLote"])
st, d = llamar("PATCH", "/lotes/%d/liberar" % lote_oc["id"],
               {"estado": "LIBERADO", "motivo": "Certificado de analisis conforme"})
check("QF libera el lote recibido", st == 200 and d["despachable"])

print("\n=== 9. TRANSFERENCIA ENTRE SEDES ===")
st, tr = llamar("POST", "/transferencias", {"almacenOrigenId": aid["ALM-CEN"],
    "almacenDestinoId": aid["ALM-SJL"], "guiaRemision": "T-001",
    "transportista": "Movil CID 02",
    "lineas": [{"productoId": pid["LIN-AV01"], "cantidad": 100}]})
check("transferencia enviada", st == 201 and tr["estado"] == "ENVIADA", tr.get("numero"))
st, d = llamar("POST", "/transferencias/%d/recibir" % tr["id"], {
    "observacion": "Llegaron 98 de 100: 2 sets con empaque roto",
    "lineas": [{"transferenciaLineaId": tr["lineas"][0]["id"], "cantidadRecibida": 98}]})
check("recepcion con diferencia registra merma en transito",
      st == 200 and d["estado"] == "RECIBIDA_PARCIAL" and d["tieneDiferencias"], d.get("estado"))
st, stock_sjl = llamar("GET", "/stock?almacenId=%d&productoId=%d" % (aid["ALM-SJL"], pid["LIN-AV01"]))
check("saldo acreditado en la sede",
      sum(float(s["cantidad"]) for s in stock_sjl["contenido"]) == 98,
      "saldo SJL %s" % sum(float(s["cantidad"]) for s in stock_sjl["contenido"]))

print("\n=== 10. TOMA DE INVENTARIO FISICO ===")
st, toma = llamar("POST", "/tomas-inventario", {"almacenId": aid["ALM-SJL"],
    "alcance": "TOTAL", "observacion": "Conteo mensual de la sede"})
check("toma programada", st == 201, toma.get("numero"))
st, toma = llamar("POST", "/tomas-inventario/%d/abrir" % toma["id"])
check("toma abierta congela el teorico", st == 200 and toma["itemsTotales"] >= 1,
      "%s items" % toma["itemsTotales"])
check("conteo ciego no expone el teorico",
      all(l.get("cantidadTeorica") is None for l in toma["lineas"]))
st, toma = llamar("POST", "/tomas-inventario/%d/conteos" % toma["id"],
    {"lineas": [{"lineaId": l["id"], "cantidadContada": 95,
                 "observacion": "Conteo fisico turno manana"} for l in toma["lineas"]]})
check("conteo registrado", st == 200)
st, toma = llamar("POST", "/tomas-inventario/%d/cerrar-conteo" % toma["id"])
check("diferencias visibles al cerrar", st == 200 and toma["itemsConDiferencia"] == 1,
      "ERI %s%%, %s diferencia(s)" % (toma["eri"], toma["itemsConDiferencia"]))
st, toma = llamar("POST", "/tomas-inventario/%d/aprobar" % toma["id"])
check("aprobacion genera ajuste", st == 200 and toma["estado"] == "APROBADA", toma.get("estado"))
st, stock_sjl = llamar("GET", "/stock?almacenId=%d&productoId=%d" % (aid["ALM-SJL"], pid["LIN-AV01"]))
check("saldo ajustado al conteo fisico",
      sum(float(s["cantidad"]) for s in stock_sjl["contenido"]) == 95,
      "saldo %s" % sum(float(s["cantidad"]) for s in stock_sjl["contenido"]))

print("\n=== 11. ANULACION DE MOVIMIENTO ===")
st, movs = llamar("GET", "/movimientos?almacenId=%d&tipo=INGRESO_COMPRA" % aid["ALM-CEN"])
mov_compra = movs["contenido"][0]
st, inv = llamar("POST", "/movimientos/%d/anular" % mov_compra["id"],
                 {"motivo": "Error de digitacion en la guia de remision"})
check("anulacion genera movimiento inverso", st == 200 and inv["movimientoAnuladoId"] == mov_compra["id"],
      "%s anula %s" % (inv.get("numero"), mov_compra["numero"]))
st, orig = llamar("GET", "/movimientos/%d" % mov_compra["id"])
check("movimiento original marcado ANULADO", orig["estadoMovimiento"] == "ANULADO")
st, d = llamar("POST", "/movimientos/%d/anular" % mov_compra["id"], {"motivo": "otra vez"})
check("no se anula dos veces", st == 409 and d["codigo"] in ("MOVIMIENTO_ANULADO", "CONFLICTO"),
      d.get("codigo"))

print("\n=== 12. ALERTAS, DASHBOARD Y REPORTES ===")
st, res = llamar("POST", "/alertas/recalcular")
check("alertas recalculadas", st == 200, "total %s criticas %s" % (res["total"], res["criticas"]))
st, al = llamar("GET", "/alertas?estado=ACTIVA")
tipos = {a["tipoAlerta"] for a in al["contenido"]}
check("alertas de reposicion y vencimiento generadas",
      "STOCK_BAJO" in tipos or "STOCK_AGOTADO" in tipos, str(sorted(tipos)))
st, dash = llamar("GET", "/dashboard?almacenId=%d" % aid["ALM-CEN"])
check("dashboard con indicadores", st == 200 and float(dash["valorInventario"]) > 0,
      "valor S/ %s, bajo minimo %s, por vencer 30d %s" % (dash["valorInventario"],
      dash["itemsBajoMinimo"], dash["lotesPorVencer30"]))
check("consumo mensual por area", len(dash["consumoMensual"]) >= 1)
check("top rotacion", len(dash["topRotacion"]) >= 1,
      dash["topRotacion"][0]["nombre"] if dash["topRotacion"] else "")
st, venc = llamar("GET", "/reportes/vencimientos?dias=450")
check("reporte de vencimientos", st == 200 and len(venc) >= 1, "%s lotes con saldo" % len(venc))
st, kcsv = llamar("GET", "/reportes/kardex.csv?productoId=%d" % pid["DIA-HF18"])
check("exportacion CSV del kardex", st == 200 and "Costo promedio" in kcsv.get("texto", ""),
      "%s filas" % (len(kcsv.get("texto", "").splitlines()) - 1))
st, val = llamar("GET", "/reportes/valorizacion")
check("valorizacion por categoria", st == 200 and len(val) >= 1,
      ", ".join("%s: S/ %s" % (v["categoriaNombre"], v["valor"]) for v in val[:3]))

print("\n=== 13. AUTORIZACION POR ROL Y ALCANCE POR ALMACEN ===")
st, d = login("enfermeria.sjl", "Clinica#2026")
check("login de enfermeria", st == 200)
llamar("POST", "/perfil/password", {"passwordActual": "Clinica#2026",
    "passwordNueva": "Enfermeria#2026", "confirmacion": "Enfermeria#2026"})
login("enfermeria.sjl", "Enfermeria#2026")
st, d = llamar("POST", "/movimientos/ingresos", {"almacenId": aid["ALM-CEN"],
    "tipoMovimiento": "INGRESO_AJUSTE", "lineas": [{"productoId": pid["EPP-GUAN"],
    "cantidad": 1, "costoUnitario": 1}]})
check("enfermeria no puede registrar movimientos", st == 403, d.get("codigo"))
st, d = llamar("GET", "/usuarios")
check("enfermeria no accede a usuarios", st == 403, d.get("codigo"))
st, d = llamar("GET", "/stock/consolidado?almacenId=%d" % aid["ALM-SJL"])
check("enfermeria consulta stock de su sede", st == 200)
st, d = login("almacenero.sjl", "Almacen#2026")
llamar("POST", "/perfil/password", {"passwordActual": "Almacen#2026",
    "passwordNueva": "AlmacenSjl#2026", "confirmacion": "AlmacenSjl#2026"})
login("almacenero.sjl", "AlmacenSjl#2026")
st, d = llamar("POST", "/movimientos/ingresos", {"almacenId": aid["ALM-CEN"],
    "tipoMovimiento": "INGRESO_AJUSTE", "motivoAjuste": "DONACION",
    "lineas": [{"productoId": pid["EPP-GUAN"], "cantidad": 1, "costoUnitario": 1}]})
check("almacenero de sede no opera el almacen central",
      st == 403 and d["codigo"] == "ALMACEN_NO_AUTORIZADO", d.get("codigo"))
st, d = llamar("POST", "/movimientos/ingresos", {"almacenId": aid["ALM-SJL"],
    "tipoMovimiento": "INGRESO_AJUSTE", "motivoAjuste": "DONACION",
    "lineas": [{"productoId": pid["EPP-GUAN"], "cantidad": 10, "costoUnitario": 32}]})
check("almacenero opera su propio almacen", st == 201, d.get("numero"))

print("\n=== 14. AUDITORIA ===")
login("admin", "Inventario#2026")
st, aud = llamar("GET", "/auditoria?size=200")
acciones = {a["accion"] for a in aud["contenido"]}
check("pista de auditoria poblada", st == 200 and aud["totalElementos"] > 10,
      "%s registros" % aud["totalElementos"])
check("auditoria registra operaciones clave",
      {"LOGIN", "REGISTRAR_DESPACHO", "CAMBIAR_ESTADO_LOTE"} <= acciones,
      str(sorted(acciones))[:160])

print("\n=================== RESUMEN ===================")
print("  verificaciones OK: %d   fallas: %d" % (ok, fail))
sys.exit(1 if fail else 0)
