package pe.cid.sgai.inventario.domain;

import java.util.Set;

/** Estados de una transferencia entre almacenes (HU-22). */
public enum EstadoTransferencia {

    ENVIADA("Enviada, en transito"),
    RECIBIDA("Recibida conforme"),
    RECIBIDA_PARCIAL("Recibida con diferencias"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoTransferencia(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<EstadoTransferencia> transicionesPermitidas() {
        return switch (this) {
            case ENVIADA -> Set.of(RECIBIDA, RECIBIDA_PARCIAL, ANULADA);
            case RECIBIDA, RECIBIDA_PARCIAL, ANULADA -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoTransferencia destino) {
        return transicionesPermitidas().contains(destino);
    }
}
