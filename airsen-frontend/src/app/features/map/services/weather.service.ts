import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Weather } from '@/core/models';
@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/weather`;
  private http = inject(HttpClient);

  getCurrentWeather(inseeCode: string): Observable<Weather>{
    return this.http.get<Weather>(`${this.apiUrl}/current/${inseeCode}`);
  }

}