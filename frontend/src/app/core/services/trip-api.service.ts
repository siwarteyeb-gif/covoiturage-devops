import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import {
  Trip,
  TripRequest,
  TripSearchParams,
  TripSearchResult,
  TripStatus,
  TripUpdateRequest,
} from '../models/trip.models';

@Injectable({ providedIn: 'root' })
export class TripApiService {
  private readonly http = inject(HttpClient);

  /** F08, F15 : publie un trajet (ou une série) ; renvoie la liste des trajets créés. */
  create(request: TripRequest): Observable<Trip[]> {
    return this.http.post<Trip[]>(`${API_URL}/trips`, request);
  }

  /** F12, F13, F14 */
  search(params: TripSearchParams): Observable<TripSearchResult[]> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<TripSearchResult[]>(`${API_URL}/trips/search`, { params: httpParams });
  }

  mine(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${API_URL}/trips/me`);
  }

  get(id: number): Observable<Trip> {
    return this.http.get<Trip>(`${API_URL}/trips/${id}`);
  }

  update(id: number, request: TripUpdateRequest): Observable<Trip> {
    return this.http.put<Trip>(`${API_URL}/trips/${id}`, request);
  }

  changeStatus(id: number, status: TripStatus): Observable<Trip> {
    return this.http.patch<Trip>(`${API_URL}/trips/${id}/status`, { status });
  }
}
