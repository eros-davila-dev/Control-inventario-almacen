package pe.cid.sgai.despacho.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.despacho.application.dto.DespachoResponse;
import pe.cid.sgai.despacho.application.dto.RequisicionResponse;
import pe.cid.sgai.despacho.domain.Despacho;
import pe.cid.sgai.despacho.domain.DespachoLinea;
import pe.cid.sgai.despacho.domain.Requisicion;
import pe.cid.sgai.despacho.domain.RequisicionLinea;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DespachoMapper {

    @Mapping(target = "almacenSolicitanteId", source = "almacenSolicitante.id")
    @Mapping(target = "almacenSolicitanteNombre", source = "almacenSolicitante.nombre")
    @Mapping(target = "almacenAtencionId", source = "almacenAtencion.id")
    @Mapping(target = "almacenAtencionNombre", source = "almacenAtencion.nombre")
    @Mapping(target = "estadoDescripcion", expression = "java(requisicion.getEstado().descripcion())")
    @Mapping(target = "solicitante", source = "createdBy")
    RequisicionResponse aResponse(Requisicion requisicion);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    @Mapping(target = "controlaLote", source = "producto.controlaLote")
    @Mapping(target = "pendiente", expression = "java(linea.pendiente())")
    RequisicionResponse.LineaRequisicionResponse aResponse(RequisicionLinea linea);

    List<RequisicionResponse.LineaRequisicionResponse> aLineasRequisicion(List<RequisicionLinea> lineas);

    @Mapping(target = "requisicionId", source = "requisicion.id")
    @Mapping(target = "requisicionNumero", source = "requisicion.numero")
    @Mapping(target = "area", source = "requisicion.area")
    @Mapping(target = "centroCosto", source = "requisicion.centroCosto")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "valorTotal", expression = "java(despacho.valorTotal())")
    DespachoResponse aResponse(Despacho despacho);

    @Mapping(target = "requisicionLineaId", source = "requisicionLinea.id")
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    @Mapping(target = "fechaVencimiento", source = "lote.fechaVencimiento")
    DespachoResponse.LineaDespachoResponse aResponse(DespachoLinea linea);

    List<DespachoResponse.LineaDespachoResponse> aLineasDespacho(List<DespachoLinea> lineas);
}
