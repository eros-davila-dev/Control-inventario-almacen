import { Pipe, PipeTransform } from '@angular/core';

/** Simbolos de las monedas que la empresa maneja en sus compras. */
const SIMBOLOS: Record<string, string> = { PEN: 'S/', USD: 'US$', EUR: '€' };

/**
 * Importes con el simbolo de su moneda. Por omision usa soles (PEN), moneda de
 * operacion de la empresa; las ordenes de compra pueden venir en otra divisa.
 */
@Pipe({ name: 'moneda' })
export class MonedaPipe implements PipeTransform {
  transform(valor: number | string | null | undefined, moneda: string | null = 'PEN'): string {
    const codigo = (moneda || 'PEN').toUpperCase();
    const simbolo = SIMBOLOS[codigo] ?? codigo;
    const numero = typeof valor === 'string' ? Number(valor) : (valor ?? 0);
    if (Number.isNaN(numero)) {
      return `${simbolo} 0.00`;
    }
    const formateado = new Intl.NumberFormat('es-PE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(numero);
    return `${simbolo} ${formateado}`;
  }
}
