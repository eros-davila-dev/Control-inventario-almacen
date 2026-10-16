package pe.cid.sgai.seguridad.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.seguridad.application.dto.UsuarioResponse;
import pe.cid.sgai.seguridad.domain.Usuario;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "nombreCompleto", expression = "java(usuario.nombreCompleto())")
    @Mapping(target = "roles", expression = "java(usuario.nombresRoles())")
    @Mapping(target = "permisos", expression = "java(usuario.codigosPermisos())")
    UsuarioResponse aResponse(Usuario usuario);

    List<UsuarioResponse> aResponses(List<Usuario> usuarios);
}
