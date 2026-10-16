package pe.cid.sgai.catalogo.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.catalogo.application.dto.UnidadMedidaResponse;
import pe.cid.sgai.catalogo.domain.UnidadMedida;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UnidadMedidaMapper {

    @Mapping(target = "umBaseId", source = "umBase.id")
    @Mapping(target = "umBaseCodigo", source = "umBase.codigo")
    @Mapping(target = "esBase", expression = "java(unidad.esBase())")
    UnidadMedidaResponse aResponse(UnidadMedida unidad);

    List<UnidadMedidaResponse> aResponses(List<UnidadMedida> unidades);
}
