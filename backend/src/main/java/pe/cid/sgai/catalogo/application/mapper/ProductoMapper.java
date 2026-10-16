package pe.cid.sgai.catalogo.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.catalogo.application.dto.ProductoResponse;
import pe.cid.sgai.catalogo.application.dto.ProductoResumenResponse;
import pe.cid.sgai.catalogo.domain.Producto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    @Mapping(target = "unidadMedidaId", source = "unidadMedida.id")
    @Mapping(target = "unidadMedidaCodigo", source = "unidadMedida.codigo")
    @Mapping(target = "unidadMedidaNombre", source = "unidadMedida.nombre")
    @Mapping(target = "requiereCadenaFrio", expression = "java(producto.requiereCadenaFrio())")
    @Mapping(target = "registroSanitarioVigente", expression = "java(producto.registroSanitarioVigente())")
    ProductoResponse aResponse(Producto producto);

    @Mapping(target = "unidadMedida", source = "unidadMedida.codigo")
    ProductoResumenResponse aResumen(Producto producto);

    List<ProductoResponse> aResponses(List<Producto> productos);

    List<ProductoResumenResponse> aResumenes(List<Producto> productos);
}
