import { of } from 'rxjs';
import { PageResponse, ParametrosPagina } from '../../core/models/api.model';
import { ListaPaginada } from './lista-paginada';

interface Fila {
  id: number;
}

function pagina(contenido: Fila[], total = contenido.length): PageResponse<Fila> {
  return {
    contenido,
    pagina: 0,
    tamano: 20,
    totalElementos: total,
    totalPaginas: Math.ceil(total / 20),
    primera: true,
    ultima: true,
  };
}

describe('ListaPaginada', () => {
  let recibidos: ParametrosPagina[];
  let lista: ListaPaginada<Fila>;

  beforeEach(() => {
    recibidos = [];
    lista = new ListaPaginada<Fila>((parametros) => {
      recibidos.push({ ...parametros });
      return of(pagina([{ id: 1 }, { id: 2 }], 45));
    }, { sort: 'fecha,desc' });
  });

  it('arranca con pagina cero, tamano 20 y el orden inicial', () => {
    lista.recargar();
    expect(recibidos[0]).toEqual({ page: 0, size: 20, sort: 'fecha,desc' });
    expect(lista.datos().length).toBe(2);
    expect(lista.total()).toBe(45);
  });

  it('al filtrar vuelve a la primera pagina', () => {
    lista.irAPagina(3);
    lista.filtrar({ estado: 'APROBADO' });
    const ultimo = recibidos[recibidos.length - 1];
    expect(ultimo.page).toBe(0);
    expect(ultimo['estado']).toBe('APROBADO');
  });

  it('al cambiar el tamano reinicia la paginacion y conserva los filtros', () => {
    lista.filtrar({ almacenId: 7 });
    lista.cambiarTamano(50);
    const ultimo = recibidos[recibidos.length - 1];
    expect(ultimo.size).toBe(50);
    expect(ultimo.page).toBe(0);
    expect(ultimo['almacenId']).toBe(7);
  });

  it('expone el orden vigente para que la tabla marque la columna', () => {
    lista.ordenarPor('numero,asc');
    expect(lista.orden()).toBe('numero,asc');
  });
});
