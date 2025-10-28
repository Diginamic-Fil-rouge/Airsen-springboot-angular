import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Commune } from '@/core/models';
@Injectable({
  providedIn: 'root'
})
export class FavoritesService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/users`;
  private http = inject(HttpClient);

    getUserFavorites(userId: number): Observable<Commune[]> {
      return this.http.get<Commune[]>(`${this.apiUrl}/${userId}/favorites`);
    }

    addFavorite(userId: number, inseeCode: string): Observable<Commune> {
      return this.http.post<Commune>(`${this.apiUrl}/${userId}/favorites`, { communeInseeCode: inseeCode });
    }

    removeFavorite(userId: number, inseeCode: number): Observable<void> {
      return this.http.delete<void>(`${this.apiUrl}/${userId}/favorites/${inseeCode}`);
    }
}