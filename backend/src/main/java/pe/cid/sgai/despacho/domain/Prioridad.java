package pe.cid.sgai.despacho.domain;

/** Prioridad de la requisicion: URGENTE se atiende el mismo turno. */
public enum Prioridad {

    NORMAL("Normal"),
    URGENTE("Urgente: sesion de hemodialisis comprometida");

    private final String descripcion;

    Prioridad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
