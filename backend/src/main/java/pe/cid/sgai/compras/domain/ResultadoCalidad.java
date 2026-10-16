package pe.cid.sgai.compras.domain;

/** Resultado del control de calidad en recepcion (HU-31). */
public enum ResultadoCalidad {

    CONFORME("Conforme: ingresa al almacen destino"),
    OBSERVADO("Observado: ingresa a cuarentena para evaluacion del QF"),
    RECHAZADO("Rechazado: no ingresa al inventario");

    private final String descripcion;

    ResultadoCalidad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean generaIngreso() {
        return this != RECHAZADO;
    }
}
