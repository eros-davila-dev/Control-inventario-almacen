package pe.cid.sgai.alertas.domain;

/** Tipos de alerta operativa del almacen (HU-39 a HU-42). */
public enum TipoAlerta {

    STOCK_AGOTADO("Sin stock disponible"),
    STOCK_BAJO("Stock bajo el minimo o en punto de reorden"),
    SOBRE_STOCK("Stock por encima del maximo"),
    POR_VENCER("Lote proximo a vencer"),
    VENCIDO("Lote vencido con saldo"),
    LOTE_RETENIDO("Lote retenido o rechazado con saldo"),
    SIN_MOVIMIENTO("Producto sin movimiento en el periodo"),
    REGISTRO_SANITARIO("Registro sanitario vencido");

    private final String descripcion;

    TipoAlerta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
