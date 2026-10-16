package pe.cid.sgai.catalogo.application.dto;

public record ProveedorResponse(
        Long id, String ruc, String razonSocial, String nombreComercial, String contacto,
        String telefono, String email, String direccion, String condicionPago,
        int leadTimeDias, boolean activo
) {
}
