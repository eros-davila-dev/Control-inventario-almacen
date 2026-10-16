import { Injectable, inject } from '@angular/core';
import { ApiService } from '../http/api.service';
import { NotificacionService } from './notificacion.service';

/** Descarga de reportes CSV generados por el servidor. */
@Injectable({ providedIn: 'root' })
export class DescargaService {
  private readonly api = inject(ApiService);
  private readonly notificacion = inject(NotificacionService);

  descargarCsv(ruta: string, nombreArchivo: string, parametros?: Record<string, unknown>): void {
    this.api.descargar(ruta, parametros).subscribe({
      next: (blob) => this.guardar(blob, nombreArchivo),
      error: () => this.notificacion.error('No se pudo generar el reporte'),
    });
  }

  private guardar(blob: Blob, nombreArchivo: string): void {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombreArchivo;
    enlace.click();
    URL.revokeObjectURL(url);
    this.notificacion.exito('Reporte descargado', nombreArchivo);
  }
}
