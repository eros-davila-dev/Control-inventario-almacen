package pe.cid.sgai.inventario.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.cid.sgai.inventario.application.dto.KardexResponse;
import pe.cid.sgai.inventario.application.dto.LoteResponse;
import pe.cid.sgai.inventario.application.dto.MovimientoLineaResponse;
import pe.cid.sgai.inventario.application.dto.MovimientoResponse;
import pe.cid.sgai.inventario.application.dto.StockResponse;
import pe.cid.sgai.inventario.application.dto.TransferenciaResponse;
import pe.cid.sgai.inventario.domain.Kardex;
import pe.cid.sgai.inventario.domain.Lote;
import pe.cid.sgai.inventario.domain.Movimiento;
import pe.cid.sgai.inventario.domain.MovimientoLinea;
import pe.cid.sgai.inventario.domain.Stock;
import pe.cid.sgai.inventario.domain.Transferencia;
import pe.cid.sgai.inventario.domain.TransferenciaLinea;
import pe.cid.sgai.shared.util.CantidadUtil;

import java.math.BigDecimal;
import java.util.List;

/** Mapeo entidad -> DTO del modulo de inventario. */
@Mapper(componentModel = "spring", imports = {CantidadUtil.class})
public interface InventarioMapper {

    // ---------- Lote ----------
    @Mapping(target = "id", source = "lote.id")
    @Mapping(target = "productoId", source = "lote.producto.id")
    @Mapping(target = "productoCodigo", source = "lote.producto.codigo")
    @Mapping(target = "productoNombre", source = "lote.producto.nombre")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    @Mapping(target = "proveedorId", source = "lote.proveedor.id")
    @Mapping(target = "proveedorNombre", source = "lote.proveedor.razonSocial")
    @Mapping(target = "fechaFabricacion", source = "lote.fechaFabricacion")
    @Mapping(target = "fechaVencimiento", source = "lote.fechaVencimiento")
    @Mapping(target = "diasParaVencer", expression = "java(lote.getFechaVencimiento() == null "
            + "? null : Long.valueOf(lote.diasParaVencer()))")
    @Mapping(target = "estadoLote", source = "lote.estadoLote")
    @Mapping(target = "estadoDescripcion", expression = "java(lote.getEstadoLote().descripcion())")
    @Mapping(target = "motivoEstado", source = "lote.motivoEstado")
    @Mapping(target = "fechaLiberacion", source = "lote.fechaLiberacion")
    @Mapping(target = "liberadoPor", source = "lote.liberadoPor")
    @Mapping(target = "costoUnitario", source = "lote.costoUnitario")
    @Mapping(target = "saldoTotal", source = "saldoTotal")
    @Mapping(target = "despachable", expression = "java(lote.esDespachable())")
    @Mapping(target = "vencido", expression = "java(lote.estaVencido())")
    LoteResponse aResponse(Lote lote, BigDecimal saldoTotal);

    // ---------- Stock ----------
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    @Mapping(target = "fechaVencimiento", source = "lote.fechaVencimiento")
    @Mapping(target = "estadoLote", source = "lote.estadoLote")
    @Mapping(target = "diasParaVencer", expression = "java(stock.getLote() == null "
            + "|| stock.getLote().getFechaVencimiento() == null "
            + "? null : Long.valueOf(stock.getLote().diasParaVencer()))")
    @Mapping(target = "ubicacionId", source = "ubicacion.id")
    @Mapping(target = "ubicacionCodigo", source = "ubicacion.codigo")
    @Mapping(target = "reservada", source = "cantidadReservada")
    @Mapping(target = "disponible", expression = "java(stock.disponible())")
    @Mapping(target = "costoPromedio", source = "producto.costoPromedio")
    @Mapping(target = "valorizado", expression = "java(CantidadUtil.importe("
            + "stock.getCantidad().multiply(stock.getProducto().getCostoPromedio())))")
    @Mapping(target = "despachable", expression = "java(stock.getLote() == null "
            + "|| stock.getLote().esDespachable())")
    StockResponse aResponse(Stock stock);

    List<StockResponse> aStockResponses(List<Stock> stocks);

    // ---------- Movimiento ----------
    @Mapping(target = "tipoDescripcion", expression = "java(movimiento.getTipoMovimiento().descripcion())")
    @Mapping(target = "almacenOrigenId", source = "almacenOrigen.id")
    @Mapping(target = "almacenOrigenNombre", source = "almacenOrigen.nombre")
    @Mapping(target = "almacenDestinoId", source = "almacenDestino.id")
    @Mapping(target = "almacenDestinoNombre", source = "almacenDestino.nombre")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.razonSocial")
    @Mapping(target = "valorTotal", expression = "java(movimiento.valorTotal())")
    @Mapping(target = "registradoPor", source = "createdBy")
    @Mapping(target = "registradoEn", source = "createdAt")
    @Mapping(target = "lineas", source = "lineas")
    MovimientoResponse aResponse(Movimiento movimiento);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "unidadMedida", source = "producto.unidadMedida.codigo")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    @Mapping(target = "fechaVencimiento", source = "lote.fechaVencimiento")
    @Mapping(target = "ubicacionId", source = "ubicacion.id")
    @Mapping(target = "ubicacionCodigo", source = "ubicacion.codigo")
    MovimientoLineaResponse aResponse(MovimientoLinea linea);

    List<MovimientoLineaResponse> aLineaResponses(List<MovimientoLinea> lineas);

    // ---------- Kardex ----------
    @Mapping(target = "tipoDescripcion", expression = "java(kardex.getTipoMovimiento().descripcion())")
    KardexResponse aResponse(Kardex kardex);

    List<KardexResponse> aKardexResponses(List<Kardex> kardex);

    // ---------- Transferencia ----------
    @Mapping(target = "almacenOrigenId", source = "almacenOrigen.id")
    @Mapping(target = "almacenOrigenNombre", source = "almacenOrigen.nombre")
    @Mapping(target = "almacenDestinoId", source = "almacenDestino.id")
    @Mapping(target = "almacenDestinoNombre", source = "almacenDestino.nombre")
    @Mapping(target = "estadoDescripcion", expression = "java(transferencia.getEstado().descripcion())")
    @Mapping(target = "tieneDiferencias", expression = "java(transferencia.tieneDiferencias())")
    TransferenciaResponse aResponse(Transferencia transferencia);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoCodigo", source = "producto.codigo")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    @Mapping(target = "fechaVencimiento", source = "lote.fechaVencimiento")
    @Mapping(target = "diferencia", expression = "java(linea.diferencia())")
    TransferenciaResponse.LineaTransferenciaResponse aResponse(TransferenciaLinea linea);

    List<TransferenciaResponse.LineaTransferenciaResponse> aTransferenciaLineaResponses(
            List<TransferenciaLinea> lineas);
}
