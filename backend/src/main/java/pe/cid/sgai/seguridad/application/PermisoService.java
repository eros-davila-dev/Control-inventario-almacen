package pe.cid.sgai.seguridad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.cid.sgai.seguridad.application.dto.PermisoResponse;
import pe.cid.sgai.seguridad.application.mapper.PermisoMapper;
import pe.cid.sgai.seguridad.repository.PermisoRepository;
import pe.cid.sgai.shared.config.ConfiguracionCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo de permisos agrupado por modulo para la pantalla de roles. */
@Service
@RequiredArgsConstructor
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final PermisoMapper permisoMapper;

    @Transactional(readOnly = true)
    @Cacheable(ConfiguracionCache.PERMISOS)
    public Map<String, List<PermisoResponse>> listarPorModulo() {
        Map<String, List<PermisoResponse>> agrupado = new LinkedHashMap<>();
        permisoRepository.findAllByOrderByModuloAscCodigoAsc().forEach(permiso ->
                agrupado.computeIfAbsent(permiso.getModulo(), k -> new java.util.ArrayList<>())
                        .add(permisoMapper.aResponse(permiso)));
        return agrupado;
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listar() {
        return permisoMapper.aResponses(permisoRepository.findAllByOrderByModuloAscCodigoAsc());
    }
}
