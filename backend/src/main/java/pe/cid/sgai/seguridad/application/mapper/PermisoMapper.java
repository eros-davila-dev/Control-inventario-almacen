package pe.cid.sgai.seguridad.application.mapper;

import org.mapstruct.Mapper;
import pe.cid.sgai.seguridad.application.dto.PermisoResponse;
import pe.cid.sgai.seguridad.domain.Permiso;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermisoMapper {

    PermisoResponse aResponse(Permiso permiso);

    List<PermisoResponse> aResponses(List<Permiso> permisos);
}
