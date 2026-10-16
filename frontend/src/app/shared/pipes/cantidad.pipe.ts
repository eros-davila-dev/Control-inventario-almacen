import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formato de cantidades del almacen: separador de miles y hasta 4 decimales,
 * eliminando los ceros a la derecha (1.5000 -> 1.5; 120.0000 -> 120).
 */
@Pipe({ name: 'cantidad' })
export class CantidadPipe implements PipeTransform {
  transform(valor: number | string | null | undefined, decimalesMaximos = 4): string {
    if (valor === null || valor === undefined || valor === '') {
      return '0';
    }
    const numero = typeof valor === 'string' ? Number(valor) : valor;
    if (Number.isNaN(numero)) {
      return '0';
    }
    return new Intl.NumberFormat('es-PE', {
      minimumFractionDigits: 0,
      maximumFractionDigits: decimalesMaximos,
    }).format(numero);
  }
}
