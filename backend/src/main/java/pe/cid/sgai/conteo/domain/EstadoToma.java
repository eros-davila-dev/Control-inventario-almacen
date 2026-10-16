package pe.cid.sgai.conteo.domain;

import java.util.Set;

public enum EstadoToma {

    PROGRAMADA("Programada"),
    EN_PROCESO("En conteo"),
    CONTADA("Contada, pendiente de aprobacion"),
    APROBADA("Aprobada con ajustes aplicados"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoToma(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<EstadoToma> transicionesPermitidas() {
        return switch (this) {
            case PROGRAMADA -> Set.of(EN_PROCESO, ANULADA);
            case EN_PROCESO -> Set.of(CONTADA, ANULADA);
            case CONTADA -> Set.of(APROBADA, EN_PROCESO, ANULADA);
            case APROBADA, ANULADA -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoToma destino) {
        return transicionesPermitidas().contains(destino);
    }
}
