import { etiquetaEstado, tonoDeEstado } from './tono-estado';

describe('tonoDeEstado', () => {
  it('pinta en peligro los estados que bloquean la operacion', () => {
    expect(tonoDeEstado('VENCIDO')).toBe('peligro');
    expect(tonoDeEstado('AGOTADO')).toBe('peligro');
    expect(tonoDeEstado('ANULADO')).toBe('peligro');
  });

  it('pinta en advertencia los estados que exigen atencion', () => {
    expect(tonoDeEstado('CUARENTENA')).toBe('advertencia');
    expect(tonoDeEstado('RECIBIDA_PARCIAL')).toBe('advertencia');
  });

  it('cae en neutral ante un estado desconocido', () => {
    expect(tonoDeEstado('ESTADO_QUE_NO_EXISTE')).toBe('neutral');
  });
});

describe('etiquetaEstado', () => {
  it('convierte el enumerado del servidor en texto legible', () => {
    expect(etiquetaEstado('RECIBIDA_PARCIAL')).toBe('Recibida parcial');
    expect(etiquetaEstado('EN_PUNTO_REORDEN')).toBe('En punto reorden');
  });

  it('tolera valores vacios', () => {
    expect(etiquetaEstado(undefined)).toBe('-');
  });
});
