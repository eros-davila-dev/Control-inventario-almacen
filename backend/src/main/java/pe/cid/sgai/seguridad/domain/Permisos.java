package pe.cid.sgai.seguridad.domain;

/**
 * Constantes de permisos usadas en las anotaciones {@code @PreAuthorize}.
 * Mantener sincronizadas con la migracion Flyway de datos semilla.
 */
public final class Permisos {

    // Seguridad
    public static final String USUARIO_LEER = "seguridad:usuario:leer";
    public static final String USUARIO_ESCRIBIR = "seguridad:usuario:escribir";
    public static final String ROL_LEER = "seguridad:rol:leer";
    public static final String ROL_ESCRIBIR = "seguridad:rol:escribir";
    public static final String AUDITORIA_LEER = "auditoria:log:leer";

    // Catalogo
    public static final String PRODUCTO_LEER = "catalogo:producto:leer";
    public static final String PRODUCTO_ESCRIBIR = "catalogo:producto:escribir";
    public static final String CATEGORIA_ESCRIBIR = "catalogo:categoria:escribir";
    public static final String UNIDAD_ESCRIBIR = "catalogo:unidad:escribir";
    public static final String PROVEEDOR_LEER = "catalogo:proveedor:leer";
    public static final String PROVEEDOR_ESCRIBIR = "catalogo:proveedor:escribir";

    // Almacenes
    public static final String ALMACEN_LEER = "almacen:almacen:leer";
    public static final String ALMACEN_ESCRIBIR = "almacen:almacen:escribir";
    public static final String UBICACION_ESCRIBIR = "almacen:ubicacion:escribir";

    // Inventario
    public static final String STOCK_LEER = "inventario:stock:leer";
    public static final String MOVIMIENTO_REGISTRAR = "inventario:movimiento:registrar";
    public static final String MOVIMIENTO_ANULAR = "inventario:movimiento:anular";
    public static final String LOTE_LEER = "inventario:lote:leer";
    public static final String LOTE_ESCRIBIR = "inventario:lote:escribir";
    public static final String LOTE_LIBERAR = "inventario:lote:liberar";
    public static final String LOTE_RETENER = "inventario:lote:retener";
    public static final String TRANSFERENCIA_REGISTRAR = "inventario:transferencia:registrar";
    public static final String TRANSFERENCIA_RECIBIR = "inventario:transferencia:recibir";

    // Compras
    public static final String REQUERIMIENTO_LEER = "compras:requerimiento:leer";
    public static final String REQUERIMIENTO_CREAR = "compras:requerimiento:crear";
    public static final String REQUERIMIENTO_APROBAR = "compras:requerimiento:aprobar";
    public static final String ORDEN_LEER = "compras:orden:leer";
    public static final String ORDEN_CREAR = "compras:orden:crear";
    public static final String ORDEN_APROBAR = "compras:orden:aprobar";
    public static final String RECEPCION_LEER = "compras:recepcion:leer";
    public static final String RECEPCION_REGISTRAR = "compras:recepcion:registrar";

    // Despacho
    public static final String REQUISICION_LEER = "despacho:requisicion:leer";
    public static final String REQUISICION_CREAR = "despacho:requisicion:crear";
    public static final String REQUISICION_ATENDER = "despacho:requisicion:atender";
    public static final String DESPACHO_LEER = "despacho:despacho:leer";

    // Conteo fisico
    public static final String TOMA_LEER = "conteo:toma:leer";
    public static final String TOMA_PROGRAMAR = "conteo:toma:programar";
    public static final String TOMA_EJECUTAR = "conteo:toma:ejecutar";
    public static final String TOMA_APROBAR = "conteo:toma:aprobar";

    // Alertas y reportes
    public static final String ALERTA_LEER = "alertas:alerta:leer";
    public static final String KARDEX_LEER = "reportes:kardex:leer";
    public static final String REPORTE_LEER = "reportes:reporte:leer";
    public static final String DASHBOARD_LEER = "reportes:dashboard:leer";

    private Permisos() {
    }
}
