import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Weather } from '../models/weather.model';
import { Commune } from '../models/commune.model';
@Injectable({
  providedIn: 'root'
})
export class GeographicService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/communes`;
  private http = inject(HttpClient);

  getCommunesWithCoordinates(): Observable<Commune[]>{
    return this.http.get<Commune[]>(`${this.apiUrl}/with-coordinates`);
  }

}