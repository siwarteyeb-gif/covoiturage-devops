import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { PlaceResult, RouteEstimate, TripRequest } from '../../core/models/trip.models';
import { Vehicle } from '../../core/models/vehicle.models';
import { MapApiService } from '../../core/services/map-api.service';
import { TripApiService } from '../../core/services/trip-api.service';
import { VehicleService } from '../../core/services/vehicle.service';
import { applyServerErrors, controlError, getErrorMessage } from '../../core/utils/http-errors';
import { formatDuration, todayIso } from '../../core/utils/labels';
import { AddressInputComponent } from '../../shared/address-input/address-input.component';
import { RouteMapComponent } from '../../shared/route-map/route-map.component';

@Component({
  selector: 'app-trip-publish',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, AddressInputComponent, RouteMapComponent],
  template: `
    <section class="page-header">
      <h1>Publier un trajet</h1>
      <p class="muted">La distance, la durée et le prix suggéré sont calculés automatiquement.</p>
    </section>

    @if (!loadingVehicles() && vehicles().length === 0) {
      <div class="alert alert-error" role="alert">
        Vous devez d'abord enregistrer un véhicule pour proposer un trajet.
        <a routerLink="/vehicles">Ajouter un véhicule</a>
      </div>
    }
    @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

    <div class="grid grid-2">
      <form class="card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <app-address-input label="Départ" inputId="dep" placeholder="Ex. Ariana" (selected)="onFrom($event)" />
        <app-address-input label="Arrivée" inputId="arr" placeholder="Ex. Université de la Manouba" (selected)="onTo($event)" />
        @if (submitted() && (!from() || !to())) {
          <small class="error-text">Choisissez le départ et l'arrivée dans la liste de suggestions.</small>
        }

        <div class="row-wrap">
          <div class="f"><label for="date">Date</label><input id="date" type="date" formControlName="date" [min]="today" />
            @if (err('date')) { <small class="error-text">{{ err('date') }}</small> }</div>
          <div class="f"><label for="time">Heure</label><input id="time" type="time" formControlName="time" />
            @if (err('time')) { <small class="error-text">{{ err('time') }}</small> }</div>
        </div>

        <div class="row-wrap">
          <div class="f">
            <label for="vehicle">Véhicule</label>
            <select id="vehicle" formControlName="vehicleId" (change)="onVehicleChange()">
              <option [value]="0">Choisir...</option>
              @for (v of vehicles(); track v.id) { <option [value]="v.id">{{ v.brand }} {{ v.model }} ({{ v.seats }} places)</option> }
            </select>
            @if (err('vehicleId')) { <small class="error-text">{{ err('vehicleId') }}</small> }
          </div>
          <div class="f narrow"><label for="seats">Places proposées</label><input id="seats" type="number" min="1" max="8" formControlName="totalSeats" />
            @if (err('totalSeats')) { <small class="error-text">{{ err('totalSeats') }}</small> }</div>
          <div class="f narrow"><label for="price">Prix / place (TND)</label><input id="price" type="number" min="0" step="0.1" formControlName="pricePerSeat" />
            @if (err('pricePerSeat')) { <small class="error-text">{{ err('pricePerSeat') }}</small> }</div>
        </div>
        @if (estimate(); as e) {
          <p class="muted">Prix suggéré : {{ e.suggestedPrice | number: '1.2-2' }} TND · maximum autorisé : {{ e.maxPrice | number: '1.2-2' }} TND (partage des frais).</p>
          @if (priceTooHigh()) { <small class="error-text">Le prix dépasse le maximum autorisé.</small> }
        }

        <fieldset class="recurrence">
          <label class="check"><input type="checkbox" formControlName="repeat" /> Trajet récurrent</label>
          @if (form.controls.repeat.value) {
            <div class="days">
              @for (d of days; track d.n) {
                <label class="check"><input type="checkbox" [checked]="weekdays().includes(d.n)" (change)="toggleDay(d.n)" /> {{ d.label }}</label>
              }
            </div>
            <div class="f narrow"><label for="weeks">Pendant (semaines)</label><input id="weeks" type="number" min="1" max="12" formControlName="weeks" /></div>
          }
        </fieldset>

        <button type="submit" class="btn btn-primary" [disabled]="saving()">{{ saving() ? 'Publication...' : 'Publier le trajet' }}</button>
      </form>

      <div class="stack">
        <div class="card map-card"><app-route-map [route]="estimate()?.route ?? null" [start]="startPoint()" [end]="endPoint()" /></div>
        <div class="card">
          <h2>Estimation</h2>
          @if (estimating()) {
            <p class="muted">Calcul de l'itinéraire...</p>
          } @else if (estimate(); as e) {
            <p>Distance : <strong>{{ e.distanceKm | number: '1.0-1' }} km</strong> · Durée : <strong>{{ duration(e.durationMin) }}</strong></p>
            @if (e.source === 'estimation') {
              <p class="muted">Mode démo : distance estimée à vol d'oiseau (aucune clé OpenRouteService configurée).</p>
            }
          } @else {
            <p class="muted">Choisissez le départ et l'arrivée pour voir l'itinéraire et le prix suggéré.</p>
          }
        </div>
      </div>
    </div>
  `,
})
export class TripPublishComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly vehiclesApi = inject(VehicleService);
  private readonly tripsApi = inject(TripApiService);
  private readonly maps = inject(MapApiService);
  private readonly router = inject(Router);

  protected readonly today = todayIso();
  protected readonly days = [
    { n: 1, label: 'Lun' }, { n: 2, label: 'Mar' }, { n: 3, label: 'Mer' }, { n: 4, label: 'Jeu' },
    { n: 5, label: 'Ven' }, { n: 6, label: 'Sam' }, { n: 7, label: 'Dim' },
  ];
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly loadingVehicles = signal(true);
  protected readonly from = signal<PlaceResult | null>(null);
  protected readonly to = signal<PlaceResult | null>(null);
  protected readonly estimate = signal<RouteEstimate | null>(null);
  protected readonly estimating = signal(false);
  protected readonly weekdays = signal<number[]>([]);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly startPoint = computed<[number, number] | null>(() => {
    const p = this.from();
    return p ? [p.lat, p.lng] : null;
  });
  protected readonly endPoint = computed<[number, number] | null>(() => {
    const p = this.to();
    return p ? [p.lat, p.lng] : null;
  });

  protected readonly form = this.fb.group({
    vehicleId: [0, [Validators.required, Validators.min(1)]],
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    totalSeats: [3, [Validators.required, Validators.min(1), Validators.max(8)]],
    pricePerSeat: [0, [Validators.required, Validators.min(0)]],
    repeat: [false],
    weeks: [4, [Validators.min(1), Validators.max(12)]],
  });

  protected readonly duration = formatDuration;

  constructor() {
    // Le prix suggéré dépend du nombre de places : on recalcule quand il change
    this.form.controls.totalSeats.valueChanges
      .pipe(debounceTime(400), takeUntilDestroyed())
      .subscribe(() => this.refreshEstimate());
  }

  ngOnInit(): void {
    this.vehiclesApi.listMine().subscribe({
      next: (list) => {
        this.vehicles.set(list);
        this.loadingVehicles.set(false);
        if (list.length === 1) {
          this.form.patchValue({ vehicleId: list[0].id, totalSeats: list[0].seats });
        }
      },
      error: (err: unknown) => {
        this.loadingVehicles.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }

  protected err(name: string): string | null {
    return controlError(this.form.get(name));
  }

  protected priceTooHigh(): boolean {
    const e = this.estimate();
    return !!e && this.form.controls.pricePerSeat.value > e.maxPrice;
  }

  protected onFrom(place: PlaceResult | null): void {
    this.from.set(place);
    this.refreshEstimate();
  }

  protected onTo(place: PlaceResult | null): void {
    this.to.set(place);
    this.refreshEstimate();
  }

  protected onVehicleChange(): void {
    const id = Number(this.form.controls.vehicleId.value);
    const v = this.vehicles().find((x) => x.id === id);
    if (v) this.form.patchValue({ totalSeats: v.seats });
  }

  protected toggleDay(n: number): void {
    this.weekdays.update((list) => (list.includes(n) ? list.filter((d) => d !== n) : [...list, n]));
  }

  private refreshEstimate(): void {
    const from = this.from();
    const to = this.to();
    const seats = this.form.controls.totalSeats.value;
    if (!from || !to || !seats || seats < 1 || seats > 8) {
      this.estimate.set(null);
      return;
    }
    this.estimating.set(true);
    this.maps.route(from, to, seats).subscribe({
      next: (e) => {
        this.estimate.set(e);
        this.estimating.set(false);
        if (!this.form.controls.pricePerSeat.dirty) {
          this.form.controls.pricePerSeat.setValue(e.suggestedPrice);
        }
      },
      error: (err: unknown) => {
        this.estimate.set(null);
        this.estimating.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }

  protected submit(): void {
    this.submitted.set(true);
    const from = this.from();
    const to = this.to();
    if (this.form.invalid || !from || !to || this.priceTooHigh()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request: TripRequest = {
      vehicleId: Number(v.vehicleId),
      departureAddress: from.label,
      departureLat: from.lat,
      departureLng: from.lng,
      arrivalAddress: to.label,
      arrivalLat: to.lat,
      arrivalLng: to.lng,
      departureDateTime: `${v.date}T${v.time}:00`,
      totalSeats: Number(v.totalSeats),
      pricePerSeat: Number(v.pricePerSeat),
    };
    if (v.repeat && this.weekdays().length > 0) {
      request.recurrenceWeekdays = this.weekdays();
      request.recurrenceWeeks = Number(v.weeks);
    }

    this.saving.set(true);
    this.error.set(null);
    this.tripsApi.create(request).subscribe({
      next: (created) => {
        this.saving.set(false);
        void this.router.navigateByUrl(created.length === 1 ? `/trips/${created[0].id}` : '/trips/mine');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(applyServerErrors(this.form, err));
      },
    });
  }
}
