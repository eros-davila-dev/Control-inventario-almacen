import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ParametrosPagina } from '../models/api.model';

/**
 * Unico punto de acceso HTTP a la API. Centraliza la ruta base versionada y la
 * construccion de parametros, de modo que los servicios de feature no repiten
 * cadenas de URL ni manejo de HttpParams.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  /** La ruta es relativa: en desarrollo la resuelve el proxy y en produccion el reverse proxy. */
  private readonly base = '/api/v1';
  private readonly http = inject(HttpClient);

  get<T>(ruta: string, parametros?: Record<string, unknown>): Observable<T> {
    return this.http.get<T>(this.url(ruta), { params: this.params(parametros) });
  }

  getPage<T>(ruta: string, parametros?: ParametrosPagina): Observable<PageResponse<T>> {
    return this.http.get<PageResponse<T>>(this.url(ruta), { params: this.params(parametros) });
  }

  post<T>(ruta: string, cuerpo?: unknown, opciones?: { cabeceras?: Record<string, string> }):
    Observable<T> {
    return this.http.post<T>(this.url(ruta), cuerpo ?? {}, { headers: opciones?.cabeceras });
  }

  postConParametros<T>(ruta: string, parametros: Record<string, unknown>): Observable<T> {
    return this.http.post<T>(this.url(ruta), {}, { params: this.params(parametros) });
  }

  put<T>(ruta: string, cuerpo: unknown): Observable<T> {
    return this.http.put<T>(this.url(ruta), cuerpo);
  }

  patch<T>(ruta: string, cuerpo?: unknown, parametros?: Record<string, unknown>): Observable<T> {
    return this.http.patch<T>(this.url(ruta), cuerpo ?? {}, { params: this.params(parametros) });
  }

  delete<T>(ruta: string): Observable<T> {
    return this.http.delete<T>(this.url(ruta));
  }

  /** Descarga de reportes: el backend responde text/csv con nombre de archivo. */
  descargar(ruta: string, parametros?: Record<string, unknown>): Observable<Blob> {
    return this.http.get(this.url(ruta), {
      params: this.params(parametros),
      responseType: 'blob',
    });
  }

  private url(ruta: string): string {
    return `${this.base}${ruta.startsWith('/') ? ruta : '/' + ruta}`;
  }

  /** Omite los filtros vacios para no enviar parametros sin valor. */
  private params(parametros?: Record<string, unknown>): HttpParams {
    let params = new HttpParams();
    if (!parametros) {
      return params;
    }
    for (const [clave, valor] of Object.entries(parametros)) {
      if (valor === null || valor === undefined || valor === '') {
        continue;
      }
      params = params.set(clave, String(valor));
    }
    return params;
  }
}
