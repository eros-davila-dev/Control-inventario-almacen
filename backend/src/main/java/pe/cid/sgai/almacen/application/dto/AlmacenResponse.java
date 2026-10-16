package pe.cid.sgai.almacen.application.dto;

import pe.cid.sgai.almacen.domain.TipoAlmacen;

public record AlmacenResponse(
        Long id, String codigo, String nombre, String sede, String direccion,
        TipoAlmacen tipoAlmacen, String tipoAlmacenDescripcion, Long responsableId,
        String telefono, boolean permiteNegativo, boolean despachable, boolean activo
) {
}
