package pe.cid.sgai.shared.util;

/** Prefijos de numeracion de documentos del almacen. */
public enum TipoCorrelativo {

    ING("Ingreso de inventario"),
    SAL("Salida de inventario"),
    AJU("Ajuste de inventario"),
    TRA("Transferencia entre almacenes"),
    REQ("Requisicion interna"),
    DES("Despacho"),
    RQC("Requerimiento de compra"),
    OC("Orden de compra"),
    REC("Recepcion de mercaderia"),
    TOM("Toma de inventario fisico");

    private final String descripcion;

    TipoCorrelativo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
