package pe.cid.sgai.catalogo.application.mapper;

import org.mapstruct.Mapper;
import pe.cid.sgai.catalogo.application.dto.ProveedorResponse;
import pe.cid.sgai.catalogo.domain.Proveedor;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    ProveedorResponse aResponse(Proveedor proveedor);

    List<ProveedorResponse> aResponses(List<Proveedor> proveedores);
}
