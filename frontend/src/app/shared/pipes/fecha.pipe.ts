import { Pipe, PipeTransform } from '@angular/core';

/** Fechas y marcas de tiempo en formato peruano (America/Lima). */
@Pipe({ name: 'fechaSgai' })
export class FechaPipe implements PipeTransform {
  transform(valor: string | Date | null | undefined, formato: 'corta' | 'larga' | 'hora' = 'corta'):
    string {
    if (!valor) {
      return '-';
    }
    const fecha = typeof valor === 'string' ? new Date(valor) : valor;
    if (Number.isNaN(fecha.getTime())) {
      return '-';
    }
    const opciones: Record<string, Intl.DateTimeFormatOptions> = {
      corta: { day: '2-digit', month: '2-digit', year: 'numeric' },
      larga: { day: '2-digit', month: 'long', year: 'numeric' },
      hora: {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      },
    };
    return new Intl.DateTimeFormat('es-PE', {
      ...opciones[formato],
      timeZone: 'America/Lima',
    }).format(fecha);
  }
}
