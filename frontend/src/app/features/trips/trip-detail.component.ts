import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Booking } from '../../core/models/booking.models';
import { Trip, TripStatus } from '../../core/models/trip.models';
import { AuthService } from '../../core/services/auth.service';
import { BookingApiService } from '../../core/services/booking-api.service';
import { TripApiService } from '../../core/services/trip-api.service';
import { getErrorMessage } from '../../core/utils/http-errors';
import { bookingBadge, formatDuration, todayIso, tripBadge } from '../../core/utils/labels';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { ReviewFormComponent } from '../../shared/review-form/review-form.component';
import { RouteMapComponent } from '../../shared/route-map/route-map.component';

@Component({
  selector: 'app-trip-detail',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe, RouteMapComponent, BadgeComponent, ReviewFormComponent],
  template: `
    @if (loading()) {
      <p class="muted">Chargement...</p>
    } @else if (trip(); as t) {
      <section class="page-header">
        <a routerLink="/trips/search" class="muted">← Retour à la recherche</a>
        <h1>{{ t.departureAddress }} → {{ t.arrivalAddress }}</h1>
        <p class="muted">
          {{ t.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }} · {{ t.distanceKm | number: '1.0-1' }} km · {{ duration(t.durationMin) }}
          <app-badge [badge]="badge(t.status)" />
        </p>
      </section>

      @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }
      @if (success()) { <div class="alert alert-success" role="status">{{ success() }}</div> }

      <div class="grid grid-2">
        <div class="card map-card tall"><app-route-map [route]="t.route" [start]="[t.departureLat, t.departureLng]" [end]="[t.arrivalLat, t.arrivalLng]" /></div>

        <div class="stack">
          <div class="card">
            <p class="big-price">{{ t.pricePerSeat | number: '1.2-2' }} TND <span class="muted">par place</span></p>
            <p class="muted">{{ t.availableSeats }} place{{ t.availableSeats > 1 ? 's' : '' }} restante{{ t.availableSeats > 1 ? 's' : '' }} sur {{ t.totalSeats }}</p>
          </div>
          <div class="card">
            <h3>{{ t.driver.name }}</h3>
            <p class="muted">Note {{ t.driver.averageRating | number: '1.1-1' }} / 5 · {{ t.vehicle.brand }} {{ t.vehicle.model }}{{ t.vehicle.color ? ', ' + t.vehicle.color : '' }}</p>
          </div>

          @if (!isOwner()) {
            <div class="card">
              @if (myBooking(); as b) {
                <h3>Votre réservation</h3>
                <p><app-badge [badge]="bBadge(b)" /> {{ b.seats }} place{{ b.seats > 1 ? 's' : '' }}</p>
                @if (b.status === 'ACCEPTED' && b.trip.driver.phone) { <p class="muted">Téléphone du conducteur : {{ b.trip.driver.phone }}</p> }
                @if (b.status === 'PENDING') { <p class="muted">Le conducteur doit accepter votre demande.</p> }
                @if (b.status === 'PENDING' || (b.status === 'ACCEPTED' && (t.status === 'OPEN' || t.status === 'FULL'))) {
                  <button type="button" class="btn btn-outline" (click)="cancelBooking(b)">Annuler ma réservation</button>
                }
              } @else if (t.status === 'OPEN') {
                <h3>Réserver</h3>
                <div class="f narrow">
                  <label for="bseats">Nombre de places</label>
                  <input id="bseats" type="number" min="1" [max]="t.availableSeats" [formControl]="seatsControl" />
                </div>
                <button type="button" class="btn btn-primary" [disabled]="busy()" (click)="requestBooking(t)">Demander {{ seatsControl.value }} place{{ seatsControl.value > 1 ? 's' : '' }}</button>
                <p class="muted">Le conducteur doit accepter votre demande.</p>
              } @else {
                <p class="muted">Ce trajet n'accepte plus de réservations.</p>
              }
            </div>
            @if (reviewBooking(); as rb) {
              <div class="card"><app-review-form [bookingId]="rb.id" label="Noter le conducteur" (done)="reload()" /></div>
            }
          } @else {
            <div class="card">
              <h3>Gérer mon trajet</h3>
              <div class="form-actions">
                @if (t.status === 'OPEN' || t.status === 'FULL') {
                  <button type="button" class="btn btn-primary" (click)="setStatus('IN_PROGRESS')">Démarrer le trajet</button>
                  <button type="button" class="btn btn-danger" (click)="setStatus('CANCELLED')">Annuler le trajet</button>
                }
                @if (t.status === 'IN_PROGRESS') {
                  <button type="button" class="btn btn-primary" (click)="setStatus('DONE')">Terminer le trajet</button>
                }
              </div>
            </div>
          }
        </div>
      </div>

      @if (isOwner()) {
        <div class="grid grid-2">
          <div class="card">
            <h2>Demandes de réservation</h2>
            @if (requests().length === 0) {
              <p class="muted">Aucune demande pour le moment.</p>
            } @else {
              <ul class="item-list">
                @for (b of requests(); track b.id) {
                  <li class="item column">
                    <div class="item-row">
                      <div>
                        <strong>{{ b.passenger.name }}</strong>
                        <div class="muted">{{ b.seats }} place{{ b.seats > 1 ? 's' : '' }} · note {{ b.passenger.averageRating | number: '1.1-1' }}/5
                          @if (b.passenger.phone) { · {{ b.passenger.phone }} }</div>
                      </div>
                      <div class="item-actions">
                        <app-badge [badge]="bBadge(b)" />
                        @if (b.status === 'PENDING') {
                          <button type="button" class="btn btn-primary btn-sm" (click)="accept(b)">Accepter</button>
                          <button type="button" class="btn btn-outline btn-sm" (click)="reject(b)">Refuser</button>
                        }
                        @if (b.status === 'ACCEPTED' && (t.status === 'OPEN' || t.status === 'FULL')) {
                          <button type="button" class="btn btn-outline btn-sm" (click)="cancel(b)">Annuler</button>
                        }
                      </div>
                    </div>
                    @if (b.status === 'ACCEPTED' && t.status === 'DONE' && !b.reviewedByMe) {
                      <app-review-form [bookingId]="b.id" [label]="'Noter ' + b.passenger.name" (done)="reload()" />
                    }
                  </li>
                }
              </ul>
            }
          </div>

          @if (t.status === 'OPEN' || t.status === 'FULL') {
            <form class="card" [formGroup]="editForm" (ngSubmit)="saveEdit()" novalidate>
              <h2>Modifier le trajet</h2>
              <div class="row-wrap">
                <div class="f"><label for="edate">Date</label><input id="edate" type="date" formControlName="date" [min]="today" /></div>
                <div class="f"><label for="etime">Heure</label><input id="etime" type="time" formControlName="time" /></div>
              </div>
              <div class="row-wrap">
                <div class="f"><label for="eseats">Places</label><input id="eseats" type="number" min="1" max="8" formControlName="totalSeats" /></div>
                <div class="f"><label for="eprice">Prix / place (TND)</label><input id="eprice" type="number" min="0" step="0.1" formControlName="pricePerSeat" /></div>
              </div>
              <button type="submit" class="btn btn-primary" [disabled]="busy()">Enregistrer</button>
            </form>
          }
        </div>
      }
    } @else {
      <div class="alert alert-error" role="alert">{{ error() ?? 'Trajet introuvable.' }}</div>
    }
  `,
})
export class TripDetailComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly tripsApi = inject(TripApiService);
  private readonly bookingsApi = inject(BookingApiService);
  protected readonly auth = inject(AuthService);

  protected readonly today = todayIso();
  protected readonly trip = signal<Trip | null>(null);
  protected readonly requests = signal<Booking[]>([]);
  protected readonly myBooking = signal<Booking | null>(null);
  protected readonly reviewBooking = signal<Booking | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly isOwner = computed(() => {
    const t = this.trip();
    const u = this.auth.user();
    return !!t && !!u && t.driver.id === u.id;
  });

  protected readonly seatsControl = this.fb.control(1, [Validators.required, Validators.min(1)]);
  protected readonly editForm = this.fb.group({
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    totalSeats: [1, [Validators.required, Validators.min(1), Validators.max(8)]],
    pricePerSeat: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly duration = formatDuration;
  protected badge = (s: TripStatus) => tripBadge(s);
  protected bBadge = (b: Booking) => bookingBadge(b.status);

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  protected reload(): void {
    this.tripsApi.get(this.id).subscribe({
      next: (t) => {
        this.trip.set(t);
        this.loading.set(false);
        this.busy.set(false);
        if (this.isOwner()) {
          this.editForm.patchValue({
            date: t.departureDateTime.slice(0, 10),
            time: t.departureDateTime.slice(11, 16),
            totalSeats: t.totalSeats,
            pricePerSeat: t.pricePerSeat,
          });
          this.bookingsApi.forTrip(t.id).subscribe((list) => this.requests.set(list));
        } else {
          this.bookingsApi.mine().subscribe((list) => {
            const mine = list.filter((b) => b.trip.id === t.id);
            this.myBooking.set(mine.find((b) => b.status === 'PENDING' || b.status === 'ACCEPTED') ?? null);
            this.reviewBooking.set(
              mine.find((b) => b.status === 'ACCEPTED' && t.status === 'DONE' && !b.reviewedByMe) ?? null,
            );
          });
        }
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }

  private act(call: Observable<unknown>, message: string): void {
    this.busy.set(true);
    this.error.set(null);
    this.success.set(null);
    call.subscribe({
      next: () => {
        this.success.set(message);
        this.reload();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }

  protected requestBooking(t: Trip): void {
    const seats = Number(this.seatsControl.value);
    if (!seats || seats < 1 || seats > t.availableSeats) {
      this.error.set(`Choisissez entre 1 et ${t.availableSeats} place(s).`);
      return;
    }
    this.act(this.bookingsApi.request(t.id, seats), 'Demande envoyée au conducteur.');
  }

  protected cancelBooking(b: Booking): void {
    if (confirm('Annuler cette réservation ?')) this.act(this.bookingsApi.cancel(b.id), 'Réservation annulée.');
  }

  protected accept(b: Booking): void {
    this.act(this.bookingsApi.accept(b.id), 'Demande acceptée.');
  }

  protected reject(b: Booking): void {
    this.act(this.bookingsApi.reject(b.id), 'Demande refusée.');
  }

  protected cancel(b: Booking): void {
    if (confirm('Annuler cette réservation ?')) this.act(this.bookingsApi.cancel(b.id), 'Réservation annulée.');
  }

  protected setStatus(status: TripStatus): void {
    if (status === 'CANCELLED' && !confirm('Annuler ce trajet ? Les réservations seront annulées.')) return;
    this.act(this.tripsApi.changeStatus(this.id, status), 'Statut du trajet mis à jour.');
  }

  protected saveEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const v = this.editForm.getRawValue();
    this.act(
      this.tripsApi.update(this.id, {
        departureDateTime: `${v.date}T${v.time}:00`,
        totalSeats: Number(v.totalSeats),
        pricePerSeat: Number(v.pricePerSeat),
      }),
      'Trajet modifié.',
    );
  }
}
