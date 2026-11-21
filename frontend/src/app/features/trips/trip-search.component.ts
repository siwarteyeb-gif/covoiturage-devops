import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PlaceResult, Trip, TripSearchParams, TripSearchResult } from '../../core/models/trip.models';
import { TripApiService } from '../../core/services/trip-api.service';
import { getErrorMessage } from '../../core/utils/http-errors';
import { formatDuration, todayIso, tripBadge } from '../../core/utils/labels';
import { AddressInputComponent } from '../../shared/address-input/address-input.component';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { RouteMapComponent } from '../../shared/route-map/route-map.component';

@Component({
  selector: 'app-trip-search',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe, AddressInputComponent, RouteMapComponent, BadgeComponent],
  template: `
    <section class="page-header">
      <h1>Rechercher un trajet</h1>
      <p class="muted">Choisissez vos adresses dans la liste pour activer le matching par détour.</p>
    </section>

    <form class="card search-form" [formGroup]="form" (ngSubmit)="search()" novalidate>
      <div class="row-wrap">
        <app-address-input label="Départ" inputId="from" placeholder="Ex. Ariana" [initial]="initialFrom"
          (selected)="from.set($event)" (textChange)="fromText.set($event)" />
        <app-address-input label="Arrivée" inputId="to" placeholder="Ex. Université de la Manouba" [initial]="initialTo"
          (selected)="to.set($event)" (textChange)="toText.set($event)" />
        <div class="f narrow"><label for="date">Date</label><input id="date" type="date" formControlName="date" [min]="today" /></div>
        <div class="f narrow"><label for="seats">Places</label><input id="seats" type="number" min="1" max="8" formControlName="seats" /></div>
      </div>
      <div class="row-wrap">
        <div class="f narrow"><label for="maxPrice">Prix max (TND)</label><input id="maxPrice" type="number" min="0" step="0.1" formControlName="maxPrice" /></div>
        <div class="f narrow"><label for="detour">Détour max (km)</label><input id="detour" type="number" min="0" max="50" formControlName="maxDetourKm" /></div>
        <div class="f narrow">
          <label for="sort">Trier par</label>
          <select id="sort" formControlName="sort">
            <option value="">Heure de départ</option>
            <option value="price">Prix</option>
            <option value="rating">Note du conducteur</option>
            <option value="detour">Détour</option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary" [disabled]="loading()">{{ loading() ? 'Recherche...' : 'Rechercher' }}</button>
      </div>
    </form>

    @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

    <div class="grid grid-2 results">
      <div class="stack">
        @if (results() === null) {
          <p class="muted">Lancez une recherche pour voir les trajets disponibles.</p>
        } @else if (results()!.length === 0) {
          <div class="card"><p class="muted">Aucun trajet ne correspond. Essayez une autre date ou augmentez le détour maximum.</p></div>
        } @else {
          <h2>{{ results()!.length }} trajet{{ results()!.length > 1 ? 's' : '' }} trouvé{{ results()!.length > 1 ? 's' : '' }}</h2>
          @for (r of results()!; track r.trip.id) {
            <article class="card trip-card" [class.selected]="selected()?.id === r.trip.id" (click)="selected.set(r.trip)">
              <div>
                <h3>{{ r.trip.departureDateTime | date: 'dd/MM/yyyy · HH:mm' }}</h3>
                <p>{{ r.trip.departureAddress }} → {{ r.trip.arrivalAddress }}</p>
                <p class="muted">
                  {{ r.trip.driver.name }} · {{ r.trip.driver.averageRating | number: '1.1-1' }}/5 ·
                  {{ r.trip.distanceKm | number: '1.0-1' }} km · {{ duration(r.trip.durationMin) }} ·
                  {{ r.trip.availableSeats }} place{{ r.trip.availableSeats > 1 ? 's' : '' }}
                </p>
              </div>
              <div class="trip-side">
                @if (r.detourKm !== null) { <span class="t g">détour {{ r.detourKm | number: '1.0-1' }} km</span> }
                <app-badge [badge]="badge(r.trip)" />
                <strong class="price">{{ r.trip.pricePerSeat | number: '1.2-2' }} TND</strong>
                <a class="btn btn-primary btn-sm" [routerLink]="['/trips', r.trip.id]">Voir</a>
              </div>
            </article>
          }
        }
      </div>
      <div class="card map-card"><app-route-map [start]="selStart()" [end]="selEnd()" /></div>
    </div>
  `,
})
export class TripSearchComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly trips = inject(TripApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly today = todayIso();
  protected initialFrom = '';
  protected initialTo = '';
  protected readonly from = signal<PlaceResult | null>(null);
  protected readonly to = signal<PlaceResult | null>(null);
  protected readonly fromText = signal('');
  protected readonly toText = signal('');
  protected readonly results = signal<TripSearchResult[] | null>(null);
  protected readonly selected = signal<Trip | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly selStart = computed<[number, number] | null>(() => {
    const t = this.selected();
    return t ? [t.departureLat, t.departureLng] : null;
  });
  protected readonly selEnd = computed<[number, number] | null>(() => {
    const t = this.selected();
    return t ? [t.arrivalLat, t.arrivalLng] : null;
  });

  protected readonly form = this.fb.group({
    date: [''],
    seats: [1, [Validators.required, Validators.min(1), Validators.max(8)]],
    maxPrice: this.fb.control<number | null>(null),
    maxDetourKm: this.fb.control<number | null>(5),
    sort: [''],
  });

  protected readonly duration = formatDuration;
  protected badge = (t: Trip) => tripBadge(t.status);

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.initialFrom = q.get('from') ?? '';
    this.initialTo = q.get('to') ?? '';
    this.fromText.set(this.initialFrom);
    this.toText.set(this.initialTo);
    const date = q.get('date');
    if (date) this.form.patchValue({ date });
    if (this.initialFrom || this.initialTo) this.search();
  }

  protected search(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const params: TripSearchParams = {
      date: v.date || undefined,
      seats: v.seats,
      maxPrice: v.maxPrice ?? undefined,
      maxDetourKm: v.maxDetourKm ?? undefined,
      sort: v.sort || undefined,
    };
    const from = this.from();
    const to = this.to();
    if (from && to) {
      Object.assign(params, { fromLat: from.lat, fromLng: from.lng, toLat: to.lat, toLng: to.lng });
    } else {
      params.from = this.fromText().trim();
      params.to = this.toText().trim();
    }

    this.loading.set(true);
    this.error.set(null);
    this.selected.set(null);
    this.trips.search(params).subscribe({
      next: (list) => {
        this.results.set(list);
        this.selected.set(list.length > 0 ? list[0].trip : null);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
