package pe.cid.sgai.conteo.domain;

/** Alcance de la toma de inventario fisico (HU-36). */
public enum AlcanceToma {

    TOTAL("Inventario total del almacen"),
    POR_CATEGORIA("Solo una categoria del catalogo"),
    CICLICO_ABC("Conteo ciclico por clasificacion ABC");

    private final String descripcion;

    AlcanceToma(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
