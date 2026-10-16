package pe.cid.sgai.catalogo.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.catalogo.application.dto.CategoriaResponse;
import pe.cid.sgai.catalogo.domain.CategoriaProducto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "categoriaPadreId", source = "categoriaPadre.id")
    @Mapping(target = "categoriaPadreNombre", source = "categoriaPadre.nombre")
    CategoriaResponse aResponse(CategoriaProducto categoria);

    List<CategoriaResponse> aResponses(List<CategoriaProducto> categorias);
}
