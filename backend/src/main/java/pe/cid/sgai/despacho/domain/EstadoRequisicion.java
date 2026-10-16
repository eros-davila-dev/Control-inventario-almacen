package pe.cid.sgai.despacho.domain;

import java.util.Set;

/** Ciclo de vida de la requisicion interna (HU-32). */
public enum EstadoRequisicion {

    BORRADOR("En elaboracion"),
    ENVIADA("Enviada al almacen"),
    EN_ATENCION("En preparacion"),
    ATENDIDA_PARCIAL("Atendida parcialmente"),
    ATENDIDA("Atendida en su totalidad"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoRequisicion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<EstadoRequisicion> transicionesPermitidas() {
        return switch (this) {
            case BORRADOR -> Set.of(ENVIADA, ANULADA);
            case ENVIADA -> Set.of(EN_ATENCION, ATENDIDA_PARCIAL, ATENDIDA, ANULADA);
            case EN_ATENCION, ATENDIDA_PARCIAL -> Set.of(ATENDIDA_PARCIAL, ATENDIDA, ANULADA);
            case ATENDIDA, ANULADA -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoRequisicion destino) {
        return transicionesPermitidas().contains(destino);
    }

    public boolean admiteAtencion() {
        return this == ENVIADA || this == EN_ATENCION || this == ATENDIDA_PARCIAL;
    }
}
