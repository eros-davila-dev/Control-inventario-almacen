package pe.cid.sgai.almacen.application.dto;

import pe.cid.sgai.almacen.domain.TipoUbicacion;

public record UbicacionResponse(
        Long id, Long almacenId, String almacenNombre, String codigo, String descripcion,
        String zona, String rack, String nivel, TipoUbicacion tipoUbicacion, boolean activo
) {
}
