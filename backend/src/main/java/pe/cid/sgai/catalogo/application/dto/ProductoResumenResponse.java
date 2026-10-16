package pe.cid.sgai.catalogo.application.dto;

/** Proyeccion ligera para combos y lineas de documento. */
public record ProductoResumenResponse(
        Long id, String codigo, String nombre, String unidadMedida,
        boolean controlaLote, boolean controlaVencimiento, boolean critico
) {
}
