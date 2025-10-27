import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Weather } from '../models/weather.model';
import { Commune } from '../models/commune.model';
@Injectable({
  providedIn: 'root'
})
export class GeographicService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/communes`;
  private http = inject(HttpClient);

  getCommunesWithCoordinatesAndMinPop(): Observable<Commune[]>{
    let communes = this.http.get<Commune[]>(`${this.apiUrl}/with-coordinates`);
     return communes.pipe(
      map(communes => communes.filter(commune => commune.population >= 100000))
    );
  }

  getAllCommunesWithCoordinates(): Observable<Commune[]>{
    return this.http.get<Commune[]>(`${this.apiUrl}/with-coordinates`);
  }

  getAllCommunesByDepartment(departmentId: number){
    return this.http.get<Commune[]>(`${environment.apiUrl}/api/v1/department/${departmentId}/communes`);
  }

  searchCommunes(query: string): Observable<Commune[]> {
    return this.http.get<Commune[]>(`${this.apiUrl}/search`, { params: { q: query }});
  }

}