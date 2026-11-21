import { Component, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { PlaceResult } from '../../core/models/trip.models';
import { MapApiService } from '../../core/services/map-api.service';

/** Champ d'adresse avec autocomplétion (F09). Émet le lieu choisi, ou null quand on modifie le texte. */
@Component({
  selector: 'app-address-input',
  template: `
    <div class="f address">
      <label [attr.for]="inputId()">{{ label() }}</label>
      <input
        [id]="inputId()"
        type="text"
        autocomplete="off"
        [placeholder]="placeholder()"
        [value]="text()"
        (input)="onInput($event)"
        (keydown.escape)="open.set(false)"
      />
      @if (open() && options().length > 0) {
        <ul class="suggest" role="listbox">
          @for (p of options(); track p.label) {
            <li><button type="button" (click)="choose(p)">{{ p.label }}</button></li>
          }
        </ul>
      }
    </div>
  `,
})
export class AddressInputComponent {
  private readonly maps = inject(MapApiService);

  readonly label = input.required<string>();
  readonly inputId = input.required<string>();
  readonly placeholder = input('');
  /** Texte de départ (ex. venu d'un lien) */
  readonly initial = input('');
  readonly selected = output<PlaceResult | null>();
  /** Texte saisi (utile pour une recherche libre sans choisir de suggestion) */
  readonly textChange = output<string>();

  protected readonly text = signal('');
  protected readonly options = signal<PlaceResult[]>([]);
  protected readonly open = signal(false);
  private readonly query$ = new Subject<string>();

  constructor() {
    effect(() => this.text.set(this.initial()));

    this.query$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => (q.length < 2 ? of<PlaceResult[]>([]) : this.maps.geocode(q).pipe(catchError(() => of<PlaceResult[]>([]))))),
        takeUntilDestroyed(),
      )
      .subscribe((list) => {
        this.options.set(list);
        this.open.set(list.length > 0);
      });
  }

  protected onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.text.set(value);
    this.selected.emit(null); // le texte a changé : le lieu choisi n'est plus valable
    this.textChange.emit(value);
    this.query$.next(value.trim());
  }

  protected choose(place: PlaceResult): void {
    this.text.set(place.label);
    this.options.set([]);
    this.open.set(false);
    this.textChange.emit(place.label);
    this.selected.emit(place);
  }
}
