package pe.cid.sgai.alertas.domain;

public enum EstadoAlerta {

    ACTIVA("Activa"),
    LEIDA("Leida"),
    RESUELTA("Resuelta");

    private final String descripcion;

    EstadoAlerta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
