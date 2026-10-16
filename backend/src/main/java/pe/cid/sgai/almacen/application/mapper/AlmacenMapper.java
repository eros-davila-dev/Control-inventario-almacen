package pe.cid.sgai.almacen.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.almacen.application.dto.AlmacenResponse;
import pe.cid.sgai.almacen.application.dto.UbicacionResponse;
import pe.cid.sgai.almacen.domain.Almacen;
import pe.cid.sgai.almacen.domain.Ubicacion;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlmacenMapper {

    @Mapping(target = "tipoAlmacenDescripcion", expression = "java(almacen.getTipoAlmacen().descripcion())")
    @Mapping(target = "despachable", expression = "java(almacen.esDespachable())")
    AlmacenResponse aResponse(Almacen almacen);

    List<AlmacenResponse> aResponses(List<Almacen> almacenes);

    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    UbicacionResponse aResponse(Ubicacion ubicacion);

    List<UbicacionResponse> aUbicacionResponses(List<Ubicacion> ubicaciones);
}
