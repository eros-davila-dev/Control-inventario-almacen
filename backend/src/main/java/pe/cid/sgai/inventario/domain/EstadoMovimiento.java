package pe.cid.sgai.inventario.domain;

public enum EstadoMovimiento {

    REGISTRADO("Registrado"),
    ANULADO("Anulado con movimiento inverso");

    private final String descripcion;

    EstadoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
