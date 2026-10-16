package pe.cid.sgai.catalogo.domain;

/** Clasificacion ABC por valor de consumo: define la frecuencia de conteo ciclico. */
public enum ClasificacionAbc {

    A("Alto valor de consumo", 30),
    B("Valor intermedio", 90),
    C("Bajo valor de consumo", 180);

    private final String descripcion;
    private final int diasEntreConteos;

    ClasificacionAbc(String descripcion, int diasEntreConteos) {
        this.descripcion = descripcion;
        this.diasEntreConteos = diasEntreConteos;
    }

    public String descripcion() {
        return descripcion;
    }

    public int diasEntreConteos() {
        return diasEntreConteos;
    }
}
