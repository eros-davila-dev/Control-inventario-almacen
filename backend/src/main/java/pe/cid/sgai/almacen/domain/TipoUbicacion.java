package pe.cid.sgai.almacen.domain;

/** Tipo de ubicacion fisica dentro de un almacen. */
public enum TipoUbicacion {

    PICKING("Zona de preparacion de pedidos"),
    RESERVA("Zona de reserva / stock de respaldo"),
    FRIO("Refrigeracion 2-8 C"),
    CUARENTENA("Zona de cuarentena");

    private final String descripcion;

    TipoUbicacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
