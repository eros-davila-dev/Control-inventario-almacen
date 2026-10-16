package pe.cid.sgai.compras.domain;

import java.util.Set;

/** Ciclo de vida de la orden de compra (HU-28 a HU-30). */
public enum EstadoOrdenCompra {

    BORRADOR("En elaboracion"),
    APROBADA("Aprobada, pendiente de atencion"),
    RECHAZADA("Rechazada"),
    RECIBIDA_PARCIAL("Recibida parcialmente"),
    RECIBIDA("Recibida en su totalidad"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoOrdenCompra(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<EstadoOrdenCompra> transicionesPermitidas() {
        return switch (this) {
            case BORRADOR -> Set.of(APROBADA, RECHAZADA, ANULADA);
            case APROBADA -> Set.of(RECIBIDA_PARCIAL, RECIBIDA, ANULADA);
            case RECIBIDA_PARCIAL -> Set.of(RECIBIDA, ANULADA);
            case RECHAZADA, RECIBIDA, ANULADA -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoOrdenCompra destino) {
        return transicionesPermitidas().contains(destino);
    }

    public boolean admiteRecepcion() {
        return this == APROBADA || this == RECIBIDA_PARCIAL;
    }
}
