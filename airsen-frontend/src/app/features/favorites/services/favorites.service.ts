import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { FavoriteCheckResponse, Favorite } from '@/shared/models';

@Injectable({
  providedIn: 'root'
})
export class FavoritesService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/users`;
  private http = inject(HttpClient);

    getUserFavorites(userId: number | undefined): Observable<Favorite[]>{
      return this.http.get<Favorite[]>(`${this.apiUrl}/${userId}/favorites`);
    }

    addFavorite(userId: number | undefined, inseeCode: string | undefined): Observable<Favorite> {
      return this.http.post<Favorite>(`${this.apiUrl}/${userId}/favorites`, { communeInseeCode: inseeCode });
    }

    removeFavorite(userId: number | undefined, inseeCode: string | undefined): Observable<void> {
      return this.http.delete<void>(`${this.apiUrl}/${userId}/favorites/${inseeCode}`);
    }

    checkIfIsFavorite(userId: number | undefined, inseeCode: string | undefined): Observable<FavoriteCheckResponse> {
      return this.http.get<FavoriteCheckResponse>(`${this.apiUrl}/${userId}/favorites/${inseeCode}/check`);
    }
}