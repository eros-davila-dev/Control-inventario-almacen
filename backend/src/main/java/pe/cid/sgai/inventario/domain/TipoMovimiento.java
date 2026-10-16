package pe.cid.sgai.inventario.domain;

/** Tipos de movimiento de inventario y su signo sobre el saldo. */
public enum TipoMovimiento {

    INGRESO_COMPRA("Ingreso por compra", 1),
    INGRESO_DEVOLUCION("Ingreso por devolucion de area", 1),
    INGRESO_AJUSTE("Ingreso por ajuste", 1),
    INGRESO_TRANSFERENCIA("Ingreso por transferencia entre almacenes", 1),
    SALIDA_DESPACHO("Salida por despacho a area", -1),
    SALIDA_MERMA("Salida por merma o rotura", -1),
    SALIDA_VENCIMIENTO("Salida por vencimiento", -1),
    SALIDA_AJUSTE("Salida por ajuste", -1),
    SALIDA_TRANSFERENCIA("Salida por transferencia entre almacenes", -1),
    SALIDA_DEVOLUCION_PROVEEDOR("Devolucion al proveedor", -1);

    private final String descripcion;
    private final int signo;

    TipoMovimiento(String descripcion, int signo) {
        this.descripcion = descripcion;
        this.signo = signo;
    }

    public String descripcion() {
        return descripcion;
    }

    public int signo() {
        return signo;
    }

    public boolean esIngreso() {
        return signo > 0;
    }

    public boolean esSalida() {
        return signo < 0;
    }

    /** Tipo inverso usado al anular un movimiento (HU-24). */
    public TipoMovimiento inverso() {
        return switch (this) {
            case INGRESO_COMPRA, INGRESO_DEVOLUCION, INGRESO_AJUSTE -> SALIDA_AJUSTE;
            case INGRESO_TRANSFERENCIA -> SALIDA_TRANSFERENCIA;
            case SALIDA_DESPACHO, SALIDA_MERMA, SALIDA_VENCIMIENTO, SALIDA_AJUSTE,
                 SALIDA_DEVOLUCION_PROVEEDOR -> INGRESO_AJUSTE;
            case SALIDA_TRANSFERENCIA -> INGRESO_TRANSFERENCIA;
        };
    }
}
