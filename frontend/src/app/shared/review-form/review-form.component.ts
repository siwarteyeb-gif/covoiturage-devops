import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BookingApiService } from '../../core/services/booking-api.service';
import { applyServerErrors } from '../../core/utils/http-errors';

/** Formulaire d'avis (note de 1 à 5 + commentaire) pour une réservation (F24). */
@Component({
  selector: 'app-review-form',
  imports: [ReactiveFormsModule],
  template: `
    <form class="review-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <p class="m">{{ label() }}</p>
      @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }
      <div class="r" style="align-items: flex-end">
        <div class="f" style="max-width: 120px">
          <label [attr.for]="'rating-' + bookingId()">Note</label>
          <select [id]="'rating-' + bookingId()" formControlName="rating">
            @for (n of [5, 4, 3, 2, 1]; track n) { <option [value]="n">{{ n }} / 5</option> }
          </select>
        </div>
        <div class="f">
          <label [attr.for]="'comment-' + bookingId()">Commentaire (facultatif)</label>
          <input [id]="'comment-' + bookingId()" type="text" formControlName="comment" maxlength="500" />
        </div>
        <button type="submit" class="p" [disabled]="saving()">Envoyer</button>
      </div>
    </form>
  `,
})
export class ReviewFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly bookings = inject(BookingApiService);

  readonly bookingId = input.required<number>();
  readonly label = input('Laisser un avis');
  readonly done = output<void>();

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = this.fb.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.maxLength(500)]],
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    const { rating, comment } = this.form.getRawValue();
    this.bookings.review(this.bookingId(), { rating: Number(rating), comment: comment.trim() || null }).subscribe({
      next: () => {
        this.saving.set(false);
        this.done.emit();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(applyServerErrors(this.form, err));
      },
    });
  }
}
