import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { Vehicle, VehicleRequest } from '../models/vehicle.models';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private readonly http = inject(HttpClient);

  listMine(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(`${API_URL}/vehicles/me`);
  }

  create(request: VehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${API_URL}/vehicles`, request);
  }

  update(id: number, request: VehicleRequest): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${API_URL}/vehicles/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/vehicles/${id}`);
  }
}
