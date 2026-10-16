package pe.cid.sgai.catalogo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.catalogo.application.dto.ProveedorRequest;
import pe.cid.sgai.catalogo.application.dto.ProveedorResponse;
import pe.cid.sgai.catalogo.application.mapper.ProveedorMapper;
import pe.cid.sgai.catalogo.domain.Proveedor;
import pe.cid.sgai.catalogo.repository.ProveedorRepository;
import pe.cid.sgai.shared.application.BaseCrudService;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;
import pe.cid.sgai.shared.web.PageResponse;

import java.util.List;

/** Proveedores de insumos y medicamentos (HU-10). */
@Service
@RequiredArgsConstructor
public class ProveedorService extends BaseCrudService<Proveedor, ProveedorRequest, ProveedorResponse> {

    private final ProveedorRepository repository;
    private final ProveedorMapper mapper;

    @Override
    protected JpaRepository<Proveedor, Long> repositorio() {
        return repository;
    }

    @Override
    protected String nombreEntidad() {
        return "proveedor";
    }

    @Transactional(readOnly = true)
    public PageResponse<ProveedorResponse> buscar(String texto, Boolean activo, Pageable pageable) {
        return PageResponse.de(repository.buscar(
                texto == null || texto.isBlank() ? null : texto.trim(), activo, pageable), mapper::aResponse);
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarActivos() {
        return mapper.aResponses(repository.findAllByActivoTrueOrderByRazonSocialAsc());
    }

    @Override
    protected void validarCreacion(ProveedorRequest request) {
        if (repository.existsByRuc(request.ruc())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El RUC ya esta registrado");
        }
    }

    @Override
    protected void validarActualizacion(Proveedor entidad, ProveedorRequest request) {
        if (repository.existsByRucAndIdNot(request.ruc(), entidad.getId())) {
            throw new NegocioException(CodigoError.DUPLICADO, "El RUC ya esta registrado");
        }
    }

    @Override
    protected Proveedor nuevaEntidad(ProveedorRequest request) {
        return Proveedor.builder()
                .ruc(request.ruc())
                .razonSocial(request.razonSocial().trim())
                .nombreComercial(request.nombreComercial())
                .contacto(request.contacto())
                .telefono(request.telefono())
                .email(request.email())
                .direccion(request.direccion())
                .condicionPago(request.condicionPago())
                .leadTimeDias(request.leadTimeDias() == null ? 7 : request.leadTimeDias())
                .activo(request.activo() == null || request.activo())
                .build();
    }

    @Override
    protected void aplicarCambios(Proveedor entidad, ProveedorRequest request) {
        entidad.setRuc(request.ruc());
        entidad.setRazonSocial(request.razonSocial().trim());
        entidad.setNombreComercial(request.nombreComercial());
        entidad.setContacto(request.contacto());
        entidad.setTelefono(request.telefono());
        entidad.setEmail(request.email());
        entidad.setDireccion(request.direccion());
        entidad.setCondicionPago(request.condicionPago());
        if (request.leadTimeDias() != null) {
            entidad.setLeadTimeDias(request.leadTimeDias());
        }
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
    }

    @Override
    protected ProveedorResponse aResponse(Proveedor entidad) {
        return mapper.aResponse(entidad);
    }
}
