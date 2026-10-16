package pe.cid.sgai.compras.domain;

import java.util.Set;

/** Ciclo de vida del requerimiento de compra (HU-27). */
public enum EstadoRequerimiento {

    BORRADOR("En elaboracion"),
    ENVIADO("Enviado para aprobacion"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado"),
    ATENDIDO("Atendido con orden de compra"),
    ANULADO("Anulado");

    private final String descripcion;

    EstadoRequerimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<EstadoRequerimiento> transicionesPermitidas() {
        return switch (this) {
            case BORRADOR -> Set.of(ENVIADO, ANULADO);
            case ENVIADO -> Set.of(APROBADO, RECHAZADO, ANULADO);
            case APROBADO -> Set.of(ATENDIDO, ANULADO);
            case RECHAZADO, ATENDIDO, ANULADO -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoRequerimiento destino) {
        return transicionesPermitidas().contains(destino);
    }
}
