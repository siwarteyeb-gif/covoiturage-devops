import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { PlaceResult, RouteEstimate } from '../models/trip.models';

@Injectable({ providedIn: 'root' })
export class MapApiService {
  private readonly http = inject(HttpClient);

  /** F09 : autocomplétion d'adresses (le back-end appelle OpenRouteService). */
  geocode(query: string): Observable<PlaceResult[]> {
    return this.http.get<PlaceResult[]>(`${API_URL}/maps/geocode`, { params: { query } });
  }

  /** F10, F16 : itinéraire, distance, durée et prix suggéré. */
  route(from: PlaceResult, to: PlaceResult, seats: number): Observable<RouteEstimate> {
    const params = new HttpParams()
      .set('fromLat', from.lat)
      .set('fromLng', from.lng)
      .set('toLat', to.lat)
      .set('toLng', to.lng)
      .set('seats', seats);
    return this.http.get<RouteEstimate>(`${API_URL}/maps/route`, { params });
  }
}
