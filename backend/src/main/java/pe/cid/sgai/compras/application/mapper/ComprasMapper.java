package pe.cid.sgai.compras.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.compras.application.dto.OrdenCompraResponse;
import pe.cid.sgai.compras.application.dto.RecepcionResponse;
import pe.cid.sgai.compras.application.dto.RequerimientoResponse;
import pe.cid.sgai.compras.domain.OrdenCompra;
import pe.cid.sgai.compras.domain.OrdenCompraLinea;
import pe.cid.sgai.compras.domain.Recepcion;
import pe.cid.sgai.compras.domain.RecepcionLinea;
import pe.cid.sgai.compras.domain.RequerimientoCompra;
import pe.cid.sgai.compras.domain.RequerimientoCompraLinea;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComprasMapper {

    // ---------- Requerimiento ----------
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "estadoDescripcion", expression = "java(requerimiento.getEstado().descripcion())")
    @Mapping(target = "solicitante", source = "createdBy")
    RequerimientoResponse aResponse(RequerimientoCompra requerimiento);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    RequerimientoResponse.LineaRequerimientoResponse aResponse(RequerimientoCompraLinea linea);

    List<RequerimientoResponse.LineaRequerimientoResponse> aLineasRequerimiento(
            List<RequerimientoCompraLinea> lineas);

    // ---------- Orden de compra ----------
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorRuc", source = "proveedor.ruc")
    @Mapping(target = "proveedorRazonSocial", source = "proveedor.razonSocial")
    @Mapping(target = "requerimientoId", source = "requerimiento.id")
    @Mapping(target = "requerimientoNumero", source = "requerimiento.numero")
    @Mapping(target = "almacenDestinoId", source = "almacenDestino.id")
    @Mapping(target = "almacenDestinoNombre", source = "almacenDestino.nombre")
    @Mapping(target = "estadoDescripcion", expression = "java(orden.getEstado().descripcion())")
    @Mapping(target = "creadoPor", source = "createdBy")
    OrdenCompraResponse aResponse(OrdenCompra orden);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    @Mapping(target = "controlaLote", source = "producto.controlaLote")
    @Mapping(target = "controlaVencimiento", source = "producto.controlaVencimiento")
    @Mapping(target = "pendiente", expression = "java(linea.pendiente())")
    OrdenCompraResponse.LineaOrdenCompraResponse aResponse(OrdenCompraLinea linea);

    List<OrdenCompraResponse.LineaOrdenCompraResponse> aLineasOrden(List<OrdenCompraLinea> lineas);

    // ---------- Recepcion ----------
    @Mapping(target = "ordenId", source = "orden.id")
    @Mapping(target = "ordenNumero", source = "orden.numero")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "resultadoDescripcion",
            expression = "java(recepcion.getResultadoCalidad().descripcion())")
    RecepcionResponse aResponse(Recepcion recepcion);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "loteId", source = "lote.id")
    RecepcionResponse.LineaRecepcionResponse aResponse(RecepcionLinea linea);

    List<RecepcionResponse.LineaRecepcionResponse> aLineasRecepcion(List<RecepcionLinea> lineas);
}
