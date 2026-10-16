package pe.cid.sgai.shared.web;

/** Rutas base versionadas de la API. */
public final class ApiRutas {

    public static final String V1 = "/api/v1";

    public static final String AUTH = V1 + "/auth";
    public static final String USUARIOS = V1 + "/usuarios";
    public static final String ROLES = V1 + "/roles";
    public static final String PERMISOS = V1 + "/permisos";
    public static final String PERFIL = V1 + "/perfil";
    public static final String AUDITORIA = V1 + "/auditoria";

    public static final String UNIDADES_MEDIDA = V1 + "/unidades-medida";
    public static final String CATEGORIAS = V1 + "/categorias";
    public static final String PRODUCTOS = V1 + "/productos";
    public static final String PROVEEDORES = V1 + "/proveedores";

    public static final String ALMACENES = V1 + "/almacenes";
    public static final String UBICACIONES = V1 + "/ubicaciones";

    public static final String LOTES = V1 + "/lotes";
    public static final String STOCK = V1 + "/stock";
    public static final String MOVIMIENTOS = V1 + "/movimientos";
    public static final String TRANSFERENCIAS = V1 + "/transferencias";
    public static final String KARDEX = V1 + "/kardex";

    public static final String REQUERIMIENTOS = V1 + "/requerimientos-compra";
    public static final String ORDENES_COMPRA = V1 + "/ordenes-compra";
    public static final String RECEPCIONES = V1 + "/recepciones";

    public static final String REQUISICIONES = V1 + "/requisiciones";
    public static final String DESPACHOS = V1 + "/despachos";

    public static final String TOMAS_INVENTARIO = V1 + "/tomas-inventario";
    public static final String ALERTAS = V1 + "/alertas";
    public static final String REPORTES = V1 + "/reportes";
    public static final String DASHBOARD = V1 + "/dashboard";

    private ApiRutas() {
    }
}
