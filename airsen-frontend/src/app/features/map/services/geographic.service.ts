import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Commune, CommuneDatas, ExportData } from '@/shared/models';
@Injectable({
  providedIn: 'root'
})
export class GeographicService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/communes`;
  private http = inject(HttpClient);

  getCommunesWithCoordinatesAndMinPop(): Observable<Commune[]>{
    let communes = this.http.get<Commune[]>(`${this.apiUrl}/with-coordinates`);
     return communes.pipe(
      map(communes => communes.filter(commune => (commune.population ?? 0) >= 100000))
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

  getCommuneExportDatas(inseeCode: string | undefined): Observable<ExportData> {
    return this.http.get<ExportData>(`${this.apiUrl}/${inseeCode}/export-data`);
  }

  getCommuneDatas(inseeCode: string | undefined): Observable<CommuneDatas> {
    return this.http.get<CommuneDatas>(`${this.apiUrl}/${inseeCode}/detail`);
  }

}