import { Pipe, PipeTransform } from '@angular/core';

/** Fechas sin hora que llegan del servidor como LocalDate (2026-03-15). */
const SOLO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

const OPCIONES: Record<string, Intl.DateTimeFormatOptions> = {
  corta: { day: '2-digit', month: '2-digit', year: 'numeric' },
  larga: { day: '2-digit', month: 'long', year: 'numeric' },
  hora: {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  },
};

/**
 * Fechas y marcas de tiempo en formato peruano (America/Lima).
 *
 * Las fechas sin hora (vencimiento de lote, emision de una orden) se formatean
 * tal como vinieron: si se interpretaran como medianoche UTC y se convirtieran
 * a Lima (UTC-5) se mostraria el dia anterior.
 */
@Pipe({ name: 'fechaSgai' })
export class FechaPipe implements PipeTransform {
  transform(valor: string | Date | null | undefined, formato: 'corta' | 'larga' | 'hora' = 'corta'):
    string {
    if (!valor) {
      return '-';
    }
    const esSoloFecha = typeof valor === 'string' && SOLO_FECHA.test(valor);
    const fecha = typeof valor === 'string'
      ? new Date(esSoloFecha ? `${valor}T00:00:00Z` : valor)
      : valor;
    if (Number.isNaN(fecha.getTime())) {
      return '-';
    }
    return new Intl.DateTimeFormat('es-PE', {
      ...OPCIONES[formato],
      timeZone: esSoloFecha ? 'UTC' : 'America/Lima',
    }).format(fecha);
  }
}
