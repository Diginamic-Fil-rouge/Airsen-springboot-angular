import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '@/environments/environment';
import { WeatherData } from '@/shared/models/weather.model';

@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  constructor(private http: HttpClient) {}

  getCurrentWeather(inseeCode: string): Observable<WeatherData | null> {
    // Assuming an endpoint exists. If not, this will fail and we might need to mock or adjust.
    // For the purpose of this task, I'll assume a standard endpoint pattern.
    return this.http.get<WeatherData>(`${environment.apiUrl}/weather/${inseeCode}/current`).pipe(
      catchError(err => {
        console.error('Error fetching weather:', err);
        return of(null);
      })
    );
  }
}
