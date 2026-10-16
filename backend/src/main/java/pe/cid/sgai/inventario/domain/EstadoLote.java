package pe.cid.sgai.inventario.domain;

import java.util.Set;

/**
 * Estado sanitario del lote (HU-16). Solo un lote LIBERADO puede despacharse a
 * las areas asistenciales; las transiciones estan restringidas y requieren motivo.
 */
public enum EstadoLote {

    CUARENTENA("En cuarentena, pendiente de liberacion del QF"),
    LIBERADO("Liberado para consumo"),
    RETENIDO("Retenido por alerta sanitaria o reclamo"),
    RECHAZADO("Rechazado, no apto para consumo"),
    VENCIDO("Vencido");

    private final String descripcion;

    EstadoLote(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean esDespachable() {
        return this == LIBERADO;
    }

    public Set<EstadoLote> transicionesPermitidas() {
        return switch (this) {
            case CUARENTENA -> Set.of(LIBERADO, RECHAZADO, VENCIDO);
            case LIBERADO -> Set.of(RETENIDO, RECHAZADO, VENCIDO);
            case RETENIDO -> Set.of(LIBERADO, RECHAZADO, VENCIDO);
            case RECHAZADO, VENCIDO -> Set.of();
        };
    }

    public boolean puedeTransitarA(EstadoLote destino) {
        return transicionesPermitidas().contains(destino);
    }
}
