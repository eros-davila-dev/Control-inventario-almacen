package pe.cid.sgai.almacen.domain;

/** Tipo de almacen de la red de CID. */
public enum TipoAlmacen {

    CENTRAL("Almacen central", true),
    SEDE("Almacen de sede / clinica", true),
    FARMACIA("Farmacia interna", true),
    CUARENTENA("Cuarentena sanitaria", false),
    FRIO("Camara de frio", true),
    RECHAZADOS("Rechazados y bajas", false);

    private final String descripcion;
    private final boolean despachable;

    TipoAlmacen(String descripcion, boolean despachable) {
        this.descripcion = descripcion;
        this.despachable = despachable;
    }

    public String descripcion() {
        return descripcion;
    }

    /** De cuarentena y rechazados no se despacha a las areas asistenciales. */
    public boolean esDespachable() {
        return despachable;
    }
}
