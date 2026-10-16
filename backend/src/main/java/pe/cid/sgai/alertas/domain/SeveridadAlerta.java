package pe.cid.sgai.alertas.domain;

public enum SeveridadAlerta {

    BAJA("Informativa"),
    MEDIA("Requiere programacion"),
    ALTA("Requiere accion en el dia"),
    CRITICA("Riesgo de suspension de sesiones de hemodialisis");

    private final String descripcion;

    SeveridadAlerta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
