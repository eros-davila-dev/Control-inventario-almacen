package pe.cid.sgai.catalogo.application.dto;

public record CategoriaResponse(
        Long id, String codigo, String nombre, String descripcion,
        Long categoriaPadreId, String categoriaPadreNombre,
        boolean requiereCadenaFrio, boolean activo
) {
}
