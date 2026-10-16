import { CantidadPipe } from './cantidad.pipe';
import { FechaPipe } from './fecha.pipe';
import { MonedaPipe } from './moneda.pipe';

describe('MonedaPipe', () => {
  const pipe = new MonedaPipe();

  it('formatea soles con dos decimales por omision', () => {
    expect(pipe.transform(1234.5)).toBe('S/ 1,234.50');
  });

  it('usa el simbolo de la moneda indicada', () => {
    expect(pipe.transform(100, 'USD')).toBe('US$ 100.00');
  });

  it('devuelve cero ante valores vacios o invalidos', () => {
    expect(pipe.transform(null)).toBe('S/ 0.00');
    expect(pipe.transform('abc')).toBe('S/ 0.00');
  });
});

describe('CantidadPipe', () => {
  const pipe = new CantidadPipe();

  it('formatea cantidades sin decimales innecesarios', () => {
    expect(pipe.transform(100)).toBe('100');
  });

  it('respeta los decimales solicitados', () => {
    expect(pipe.transform(12.3456, 2)).toBe('12.35');
  });
});

describe('FechaPipe', () => {
  const pipe = new FechaPipe();

  it('formatea una fecha ISO al formato peruano', () => {
    expect(pipe.transform('2026-03-15')).toBe('15/03/2026');
  });

  it('no desplaza un dia las fechas sin hora', () => {
    // El servidor envia LocalDate; interpretarlo como UTC y convertirlo a Lima
    // mostraria el dia anterior, que en un vencimiento de lote es un error.
    expect(pipe.transform('2026-01-01')).toBe('01/01/2026');
  });

  it('muestra las marcas de tiempo en hora de Lima', () => {
    // Intl separa la hora con espacios no separables: se normalizan al comparar.
    const texto = pipe.transform('2026-03-15T02:30:00Z', 'hora').replace(/\s/g, ' ');
    expect(texto).toBe('14/03/2026, 09:30 p. m.');
  });

  it('devuelve guion ante valores vacios', () => {
    expect(pipe.transform(null)).toBe('-');
    expect(pipe.transform('no-es-fecha')).toBe('-');
  });
});
