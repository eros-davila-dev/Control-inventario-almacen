package pe.cid.sgai.catalogo.domain;

/** Condicion de conservacion exigida por el fabricante / registro sanitario. */
public enum CondicionAlmacenamiento {

    AMBIENTE("Temperatura ambiente (15-30 C)", false),
    REFRIGERADO_2_8("Refrigerado (2-8 C)", true),
    CONGELADO("Congelado (-20 C)", true),
    PROTEGIDO_LUZ("Protegido de la luz", false);

    private final String descripcion;
    private final boolean cadenaFrio;

    CondicionAlmacenamiento(String descripcion, boolean cadenaFrio) {
        this.descripcion = descripcion;
        this.cadenaFrio = cadenaFrio;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean requiereCadenaFrio() {
        return cadenaFrio;
    }
}
