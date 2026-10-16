import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/http/api.service';
import { Dashboard } from '../../core/models/reportes.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  indicadores(almacenId: number | null): Observable<Dashboard> {
    return this.api.get<Dashboard>('/dashboard', { almacenId });
  }
}
