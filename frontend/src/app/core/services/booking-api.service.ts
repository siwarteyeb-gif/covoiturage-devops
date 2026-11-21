import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { Booking, Review, ReviewRequest } from '../models/booking.models';

@Injectable({ providedIn: 'root' })
export class BookingApiService {
  private readonly http = inject(HttpClient);

  /** F17 */
  request(tripId: number, seats: number): Observable<Booking> {
    return this.http.post<Booking>(`${API_URL}/trips/${tripId}/bookings`, { seats });
  }

  /** F21 : mes réservations (en tant que passager) */
  mine(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${API_URL}/bookings/me`);
  }

  /** Demandes reçues sur tous mes trajets (en tant que conducteur) */
  received(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${API_URL}/bookings/received`);
  }

  forTrip(tripId: number): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${API_URL}/trips/${tripId}/bookings`);
  }

  /** F18 */
  accept(id: number): Observable<Booking> {
    return this.http.patch<Booking>(`${API_URL}/bookings/${id}/accept`, {});
  }

  reject(id: number): Observable<Booking> {
    return this.http.patch<Booking>(`${API_URL}/bookings/${id}/reject`, {});
  }

  /** F19 */
  cancel(id: number): Observable<Booking> {
    return this.http.patch<Booking>(`${API_URL}/bookings/${id}/cancel`, {});
  }

  /** F24 */
  review(bookingId: number, request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${API_URL}/bookings/${bookingId}/review`, request);
  }

  reviewsOf(userId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${API_URL}/users/${userId}/reviews`);
  }
}
