import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { AdminUser, Stats } from '../models/admin.models';
import { Review } from '../models/booking.models';
import { Trip } from '../models/trip.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  stats(): Observable<Stats> {
    return this.http.get<Stats>(`${API_URL}/admin/stats`);
  }

  users(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${API_URL}/admin/users`);
  }

  setBlocked(id: number, blocked: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${API_URL}/admin/users/${id}/${blocked ? 'block' : 'unblock'}`, {});
  }

  trips(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${API_URL}/admin/trips`);
  }

  deleteTrip(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/trips/${id}`);
  }

  reviews(): Observable<Review[]> {
    return this.http.get<Review[]>(`${API_URL}/admin/reviews`);
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/reviews/${id}`);
  }
}
