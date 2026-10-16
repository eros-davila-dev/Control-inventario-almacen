package pe.cid.sgai.inventario.domain;

/** Motivos tipificados de ajuste de inventario (HU-23). */
public enum MotivoAjuste {

    CONTEO_FISICO("Diferencia de conteo fisico"),
    MERMA("Merma operativa"),
    VENCIMIENTO("Baja por vencimiento"),
    ROTURA("Rotura o dano del empaque"),
    ERROR_REGISTRO("Correccion de error de registro"),
    DONACION("Ingreso por donacion"),
    SALDO_INICIAL("Carga de saldo inicial"),
    DEVOLUCION_AREA("Devolucion desde area asistencial"),
    MERMA_TRANSITO("Diferencia detectada en transferencia");

    private final String descripcion;

    MotivoAjuste(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
