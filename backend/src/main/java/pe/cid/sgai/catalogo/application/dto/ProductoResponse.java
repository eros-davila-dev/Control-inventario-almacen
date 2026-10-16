package pe.cid.sgai.catalogo.application.dto;

import pe.cid.sgai.catalogo.domain.ClasificacionAbc;
import pe.cid.sgai.catalogo.domain.CondicionAlmacenamiento;
import pe.cid.sgai.catalogo.domain.TipoProducto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoriaNombre,
        Long unidadMedidaId,
        String unidadMedidaCodigo,
        String unidadMedidaNombre,
        TipoProducto tipoProducto,
        String presentacion,
        String marca,
        String registroSanitario,
        LocalDate venceRegistroSanitario,
        boolean registroSanitarioVigente,
        CondicionAlmacenamiento condicionAlmacenamiento,
        boolean requiereCadenaFrio,
        boolean controlaLote,
        boolean controlaVencimiento,
        int vidaUtilMinimaDias,
        ClasificacionAbc clasificacionAbc,
        BigDecimal stockMinimo,
        BigDecimal puntoReorden,
        BigDecimal stockMaximo,
        int diasCobertura,
        BigDecimal costoPromedio,
        boolean critico,
        boolean activo
) {
}
