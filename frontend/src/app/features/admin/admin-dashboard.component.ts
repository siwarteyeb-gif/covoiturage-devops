import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminUser, Stats } from '../../core/models/admin.models';
import { Review } from '../../core/models/booking.models';
import { Trip } from '../../core/models/trip.models';
import { AdminApiService } from '../../core/services/admin-api.service';
import { getErrorMessage } from '../../core/utils/http-errors';
import { tripBadge } from '../../core/utils/labels';
import { BadgeComponent } from '../../shared/badge/badge.component';

type Tab = 'stats' | 'users' | 'trips' | 'reviews';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, DecimalPipe, BadgeComponent],
  template: `
    <section class="page-header"><h1>Administration</h1></section>

    <div class="tabs" role="tablist">
      @for (t of tabs; track t.id) {
        <button type="button" role="tab" class="tab" [class.active]="tab() === t.id" [attr.aria-selected]="tab() === t.id" (click)="open(t.id)">{{ t.label }}</button>
      }
    </div>

    @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

    @switch (tab()) {
      @case ('stats') {
        @if (stats(); as s) {
          <div class="grid stats">
            <div class="card"><p class="muted">Utilisateurs</p><p class="stat">{{ s.usersCount }}</p></div>
            <div class="card"><p class="muted">Conducteurs</p><p class="stat">{{ s.driversCount }}</p></div>
            <div class="card"><p class="muted">Trajets publiés</p><p class="stat">{{ s.tripsCount }}</p></div>
            <div class="card"><p class="muted">Trajets ouverts</p><p class="stat">{{ s.openTripsCount }}</p></div>
            <div class="card"><p class="muted">Réservations</p><p class="stat">{{ s.bookingsCount }}</p></div>
            <div class="card"><p class="muted">Réservations acceptées</p><p class="stat">{{ s.acceptedBookings }}</p></div>
            <div class="card"><p class="muted">Places remplies</p><p class="stat">{{ s.fillRatePercent | number: '1.0-1' }} %</p></div>
          </div>
          <div class="card">
            <h2>Trajets populaires</h2>
            @if (s.popularRoutes.length === 0) {
              <p class="muted">Aucune réservation pour le moment.</p>
            } @else {
              @for (r of s.popularRoutes; track $index) {
                <div class="bar-row">
                  <span>{{ r.departure }} → {{ r.arrival }}</span>
                  <div class="bar"><div class="bar-fill" [style.width.%]="barWidth(r.bookings, s)"></div></div>
                  <strong>{{ r.bookings }}</strong>
                </div>
              }
            }
          </div>
        } @else { <p class="muted">Chargement...</p> }
      }
      @case ('users') {
        <div class="card">
          <ul class="item-list">
            @for (u of users(); track u.id) {
              <li class="item">
                <div>
                  <strong>{{ u.name }}</strong> <span class="t z">{{ u.role }}</span>
                  <div class="muted">{{ u.email }} · {{ u.phone }} · inscrit le {{ u.createdAt | date: 'dd/MM/yyyy' }}</div>
                </div>
                <div class="item-actions">
                  <span class="t" [class]="u.blocked ? 'r' : 'g'">{{ u.blocked ? 'Bloqué' : 'Actif' }}</span>
                  @if (u.role !== 'ADMIN') {
                    <button type="button" class="btn btn-outline btn-sm" (click)="toggleBlock(u)">{{ u.blocked ? 'Débloquer' : 'Bloquer' }}</button>
                  }
                </div>
              </li>
            }
          </ul>
        </div>
      }
      @case ('trips') {
        <div class="card">
          <ul class="item-list">
            @for (t of trips(); track t.id) {
              <li class="item">
                <div>
                  <strong>{{ t.departureAddress }} → {{ t.arrivalAddress }}</strong>
                  <div class="muted">{{ t.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }} · conducteur : {{ t.driver.name }}</div>
                </div>
                <div class="item-actions">
                  <app-badge [badge]="badge(t)" />
                  <button type="button" class="btn btn-danger btn-sm" (click)="deleteTrip(t)">Supprimer</button>
                </div>
              </li>
            }
          </ul>
        </div>
      }
      @case ('reviews') {
        <div class="card">
          @if (reviews().length === 0) { <p class="muted">Aucun avis.</p> }
          <ul class="item-list">
            @for (r of reviews(); track r.id) {
              <li class="item">
                <div>
                  <strong>{{ r.author.name }} → {{ r.target.name }}</strong> · {{ r.rating }}/5
                  <div class="muted">{{ r.comment || 'Sans commentaire' }}</div>
                </div>
                <button type="button" class="btn btn-danger btn-sm" (click)="deleteReview(r)">Supprimer</button>
              </li>
            }
          </ul>
        </div>
      }
    }
  `,
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly tabs: { id: Tab; label: string }[] = [
    { id: 'stats', label: 'Tableau de bord' },
    { id: 'users', label: 'Utilisateurs' },
    { id: 'trips', label: 'Trajets' },
    { id: 'reviews', label: 'Avis' },
  ];
  protected readonly tab = signal<Tab>('stats');
  protected readonly stats = signal<Stats | null>(null);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly trips = signal<Trip[]>([]);
  protected readonly reviews = signal<Review[]>([]);
  protected readonly error = signal<string | null>(null);
  protected badge = (t: Trip) => tripBadge(t.status);

  ngOnInit(): void {
    this.open('stats');
  }

  protected open(tab: Tab): void {
    this.tab.set(tab);
    this.error.set(null);
    switch (tab) {
      case 'stats':
        this.fetch(this.api.stats(), (v) => this.stats.set(v));
        break;
      case 'users':
        this.fetch(this.api.users(), (v) => this.users.set(v));
        break;
      case 'trips':
        this.fetch(this.api.trips(), (v) => this.trips.set(v));
        break;
      case 'reviews':
        this.fetch(this.api.reviews(), (v) => this.reviews.set(v));
        break;
    }
  }

  protected barWidth(value: number, s: Stats): number {
    const max = Math.max(...s.popularRoutes.map((r) => r.bookings), 1);
    return Math.round((value / max) * 100);
  }

  protected toggleBlock(u: AdminUser): void {
    this.fetch(this.api.setBlocked(u.id, !u.blocked), (updated) =>
      this.users.update((list) => list.map((x) => (x.id === updated.id ? updated : x))),
    );
  }

  protected deleteTrip(t: Trip): void {
    if (!confirm('Supprimer définitivement ce trajet et ses réservations ?')) return;
    this.fetch(this.api.deleteTrip(t.id), () => this.trips.update((list) => list.filter((x) => x.id !== t.id)));
  }

  protected deleteReview(r: Review): void {
    if (!confirm('Supprimer cet avis ?')) return;
    this.fetch(this.api.deleteReview(r.id), () => this.reviews.update((list) => list.filter((x) => x.id !== r.id)));
  }

  private fetch<T>(call: Observable<T>, onSuccess: (value: T) => void): void {
    call.subscribe({
      next: onSuccess,
      error: (err: unknown) => this.error.set(getErrorMessage(err)),
    });
  }
}
