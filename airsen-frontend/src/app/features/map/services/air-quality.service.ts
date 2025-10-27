import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { AirQuality } from '../models/airQuality.model';
@Injectable({
  providedIn: 'root'
})
export class AirQualityService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/atmo`;
  private http = inject(HttpClient);

  getAirLatestQuality(inseeCode: string): Observable<any>{
    return this.http.get(`${this.apiUrl}/air-quality/${inseeCode}/latest`);
  }

  getAirQuality(inseeCode: string): Observable<AirQuality[]>{
    return this.http.get<AirQuality[]>(`${this.apiUrl}/air-quality/${inseeCode}`);
  }

}