package pe.cid.sgai.shared.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura estable de paginacion: el contrato no depende de la serializacion
 * interna de {@code Page} de Spring Data.
 */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {

    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <E, T> PageResponse<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PageResponse<>(page.getContent().stream().map(mapeador).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }

    public static <T> PageResponse<T> deLista(List<T> contenido) {
        return new PageResponse<>(contenido, 0, contenido.size(), contenido.size(), 1, true, true);
    }
}
