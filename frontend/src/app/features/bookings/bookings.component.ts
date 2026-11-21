import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Booking } from '../../core/models/booking.models';
import { BookingApiService } from '../../core/services/booking-api.service';
import { getErrorMessage } from '../../core/utils/http-errors';
import { bookingBadge } from '../../core/utils/labels';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { ReviewFormComponent } from '../../shared/review-form/review-form.component';

@Component({
  selector: 'app-bookings',
  imports: [RouterLink, DatePipe, BadgeComponent, ReviewFormComponent],
  template: `
    <section class="page-header"><h1>Mes réservations</h1></section>
    @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

    <div class="grid grid-2">
      <div class="stack">
        <h2>En tant que passager</h2>
        @if (loading()) {
          <p class="muted">Chargement...</p>
        } @else if (mine().length === 0) {
          <div class="card"><p class="muted">Aucune réservation. <a routerLink="/trips/search">Rechercher un trajet</a></p></div>
        }
        @for (b of mine(); track b.id) {
          <article class="card column">
            <div class="item-row">
              <div>
                <h3><a [routerLink]="['/trips', b.trip.id]">{{ b.trip.departureAddress }} → {{ b.trip.arrivalAddress }}</a></h3>
                <p class="muted">{{ b.trip.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }} · {{ b.trip.driver.name }} · {{ b.seats }} place{{ b.seats > 1 ? 's' : '' }}</p>
                @if (b.status === 'ACCEPTED' && b.trip.driver.phone) { <p class="muted">Téléphone du conducteur : {{ b.trip.driver.phone }}</p> }
              </div>
              <div class="item-actions">
                <app-badge [badge]="badge(b)" />
                @if (b.status === 'PENDING' || (b.status === 'ACCEPTED' && (b.trip.status === 'OPEN' || b.trip.status === 'FULL'))) {
                  <button type="button" class="btn btn-outline btn-sm" (click)="cancel(b)">Annuler</button>
                }
              </div>
            </div>
            @if (b.status === 'ACCEPTED' && b.trip.status === 'DONE' && !b.reviewedByMe) {
              <app-review-form [bookingId]="b.id" label="Noter le conducteur" (done)="load()" />
            }
          </article>
        }
      </div>

      <div class="stack">
        <h2>Demandes reçues (conducteur)</h2>
        @if (!loading() && received().length === 0) {
          <div class="card"><p class="muted">Aucune demande reçue.</p></div>
        }
        @for (b of received(); track b.id) {
          <article class="card column">
            <div class="item-row">
              <div>
                <h3>{{ b.passenger.name }} · {{ b.seats }} place{{ b.seats > 1 ? 's' : '' }}</h3>
                <p class="muted"><a [routerLink]="['/trips', b.trip.id]">{{ b.trip.departureAddress }} → {{ b.trip.arrivalAddress }}</a>,
                  {{ b.trip.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }}</p>
                @if (b.status === 'ACCEPTED' && b.passenger.phone) { <p class="muted">Téléphone : {{ b.passenger.phone }}</p> }
              </div>
              <div class="item-actions">
                <app-badge [badge]="badge(b)" />
                @if (b.status === 'PENDING') {
                  <button type="button" class="btn btn-primary btn-sm" (click)="run(accept(b))">Accepter</button>
                  <button type="button" class="btn btn-outline btn-sm" (click)="run(reject(b))">Refuser</button>
                }
                @if (b.status === 'ACCEPTED' && (b.trip.status === 'OPEN' || b.trip.status === 'FULL')) {
                  <button type="button" class="btn btn-outline btn-sm" (click)="cancel(b)">Annuler</button>
                }
              </div>
            </div>
            @if (b.status === 'ACCEPTED' && b.trip.status === 'DONE' && !b.reviewedByMe) {
              <app-review-form [bookingId]="b.id" [label]="'Noter ' + b.passenger.name" (done)="load()" />
            }
          </article>
        }
      </div>
    </div>
  `,
})
export class BookingsComponent implements OnInit {
  private readonly api = inject(BookingApiService);
  protected readonly mine = signal<Booking[]>([]);
  protected readonly received = signal<Booking[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected badge = (b: Booking) => bookingBadge(b.status);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.api.mine().subscribe({
      next: (list) => {
        this.mine.set(list);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
    this.api.received().subscribe({
      next: (list) => this.received.set(list),
      error: (err: unknown) => this.error.set(getErrorMessage(err)),
    });
  }

  protected accept(b: Booking): Observable<unknown> {
    return this.api.accept(b.id);
  }

  protected reject(b: Booking): Observable<unknown> {
    return this.api.reject(b.id);
  }

  protected cancel(b: Booking): void {
    if (confirm('Annuler cette réservation ?')) this.run(this.api.cancel(b.id));
  }

  protected run(call: Observable<unknown>): void {
    this.error.set(null);
    call.subscribe({
      next: () => this.load(),
      error: (err: unknown) => this.error.set(getErrorMessage(err)),
    });
  }
}
