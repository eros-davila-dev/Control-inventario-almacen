package pe.cid.sgai.shared.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Genera numeros de documento correlativos y sin huecos por tipo y anio
 * (ej. {@code OC-2026-00042}), con bloqueo pesimista sobre el contador.
 */
@Service
@RequiredArgsConstructor
public class CorrelativoService {

    private final CorrelativoRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(TipoCorrelativo tipo) {
        return siguiente(tipo, LocalDate.now(FechaUtil.ZONA).getYear());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(TipoCorrelativo tipo, int anio) {
        Correlativo correlativo = repository.buscarParaActualizar(tipo.name(), anio)
                .orElseGet(() -> repository.saveAndFlush(new Correlativo(tipo.name(), anio)));
        long numero = correlativo.incrementar();
        repository.saveAndFlush(correlativo);
        return "%s-%d-%05d".formatted(tipo.name(), anio, numero);
    }
}
