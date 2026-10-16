import { TonoBadge } from './ui-badge.component';

/**
 * Mapa unico de estado -> tono visual. Mantener aqui la correspondencia evita
 * que cada pantalla invente sus propios colores para el mismo estado.
 */
const MAPA: Record<string, TonoBadge> = {
  // Lotes
  LIBERADO: 'exito',
  CUARENTENA: 'advertencia',
  RETENIDO: 'advertencia',
  RECHAZADO: 'peligro',
  VENCIDO: 'peligro',
  // Movimientos
  REGISTRADO: 'exito',
  ANULADO: 'peligro',
  // Documentos
  BORRADOR: 'neutral',
  ENVIADO: 'info',
  ENVIADA: 'info',
  APROBADO: 'exito',
  APROBADA: 'exito',
  RECHAZADA: 'peligro',
  ATENDIDO: 'exito',
  ATENDIDA: 'exito',
  ATENDIDA_PARCIAL: 'advertencia',
  EN_ATENCION: 'info',
  RECIBIDA: 'exito',
  RECIBIDA_PARCIAL: 'advertencia',
  ANULADA: 'peligro',
  PROGRAMADA: 'neutral',
  EN_PROCESO: 'info',
  CONTADA: 'advertencia',
  // Calidad
  CONFORME: 'exito',
  OBSERVADO: 'advertencia',
  // Reposicion
  AGOTADO: 'peligro',
  BAJO_MINIMO: 'peligro',
  EN_PUNTO_REORDEN: 'advertencia',
  SOBRE_STOCK: 'info',
  NORMAL: 'exito',
  // Severidad de alertas
  CRITICA: 'peligro',
  ALTA: 'advertencia',
  MEDIA: 'info',
  BAJA: 'neutral',
  ACTIVA: 'peligro',
  LEIDA: 'info',
  RESUELTA: 'exito',
  // Prioridad
  URGENTE: 'peligro',
};

export function tonoDeEstado(estado: string | null | undefined): TonoBadge {
  if (!estado) {
    return 'neutral';
  }
  return MAPA[estado] ?? 'neutral';
}

/** Texto legible de un enumerado: ATENDIDA_PARCIAL -> Atendida parcial. */
export function etiquetaEstado(estado: string | null | undefined): string {
  if (!estado) {
    return '-';
  }
  const texto = estado.replace(/_/g, ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
