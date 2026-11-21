import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Trip } from '../../core/models/trip.models';
import { TripApiService } from '../../core/services/trip-api.service';
import { getErrorMessage } from '../../core/utils/http-errors';
import { tripBadge } from '../../core/utils/labels';
import { BadgeComponent } from '../../shared/badge/badge.component';

@Component({
  selector: 'app-my-trips',
  imports: [RouterLink, DatePipe, DecimalPipe, BadgeComponent],
  template: `
    <section class="page-header row-between">
      <div><h1>Mes trajets</h1><p class="muted">Les trajets que vous proposez en tant que conducteur.</p></div>
      <a routerLink="/trips/new" class="btn btn-primary">Publier un trajet</a>
    </section>

    @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

    @if (loading()) {
      <p class="muted">Chargement...</p>
    } @else if (trips().length === 0) {
      <div class="card"><p class="muted">Vous n'avez publié aucun trajet.</p></div>
    } @else {
      <div class="stack">
        @for (t of trips(); track t.id) {
          <article class="card trip-card">
            <div>
              <h3>{{ t.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }}</h3>
              <p>{{ t.departureAddress }} → {{ t.arrivalAddress }}</p>
              <p class="muted">{{ t.availableSeats }} / {{ t.totalSeats }} places libres · {{ t.distanceKm | number: '1.0-1' }} km</p>
            </div>
            <div class="trip-side">
              <app-badge [badge]="badge(t)" />
              <strong class="price">{{ t.pricePerSeat | number: '1.2-2' }} TND</strong>
              <a class="btn btn-outline btn-sm" [routerLink]="['/trips', t.id]">Gérer</a>
            </div>
          </article>
        }
      </div>
    }
  `,
})
export class MyTripsComponent implements OnInit {
  private readonly tripsApi = inject(TripApiService);
  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected badge = (t: Trip) => tripBadge(t.status);

  ngOnInit(): void {
    this.tripsApi.mine().subscribe({
      next: (list) => {
        this.trips.set(list);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }
}
