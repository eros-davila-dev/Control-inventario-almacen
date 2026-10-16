package pe.cid.sgai.catalogo.domain;

/**
 * Tipos de articulo que maneja el almacen de una clinica de hemodialisis.
 * Determina si el control de lote y vencimiento es obligatorio.
 */
public enum TipoProducto {

    INSUMO_DIALISIS("Insumo de dialisis", true),
    MEDICAMENTO("Medicamento", true),
    MATERIAL_MEDICO("Material medico", true),
    REACTIVO("Reactivo de laboratorio", true),
    EPP("Equipo de proteccion personal", false),
    REPUESTO_MAQUINA("Repuesto de maquina de hemodialisis", false),
    LIMPIEZA("Limpieza y desinfeccion", false),
    OFICINA("Utiles de oficina", false);

    private final String descripcion;
    private final boolean exigeTrazabilidad;

    TipoProducto(String descripcion, boolean exigeTrazabilidad) {
        this.descripcion = descripcion;
        this.exigeTrazabilidad = exigeTrazabilidad;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Los productos sanitarios exigen lote y fecha de vencimiento (HU-07). */
    public boolean exigeTrazabilidad() {
        return exigeTrazabilidad;
    }
}
