import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { BookingApiService } from '../../core/services/booking-api.service';
import { Review } from '../../core/models/booking.models';
import { applyServerErrors, controlError, getErrorMessage } from '../../core/utils/http-errors';
import { PHONE_PATTERN, fieldsMatch } from '../../core/utils/validators';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly users = inject(UserService);
  protected readonly auth = inject(AuthService);
  private readonly bookingsApi = inject(BookingApiService);
  protected readonly reviews = signal<Review[]>([]);

  protected readonly loadError = signal<string | null>(null);

  // --- informations personnelles ---
  protected readonly savingProfile = signal(false);
  protected readonly profileError = signal<string | null>(null);
  protected readonly profileSuccess = signal<string | null>(null);
  protected readonly profileForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
  });

  // --- mot de passe ---
  protected readonly savingPassword = signal(false);
  protected readonly passwordError = signal<string | null>(null);
  protected readonly passwordSuccess = signal<string | null>(null);
  protected readonly passwordForm = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: fieldsMatch('newPassword', 'confirmPassword') },
  );

  ngOnInit(): void {
    const cached = this.auth.user();
    if (cached) {
      this.profileForm.patchValue({ name: cached.name, phone: cached.phone });
    }
    // On recharge le profil depuis le serveur pour avoir des données à jour
    this.users.getProfile().subscribe({
      next: (user) => {
        this.auth.updateUser(user);
        this.profileForm.patchValue({ name: user.name, phone: user.phone });
        this.bookingsApi.reviewsOf(user.id).subscribe((list) => this.reviews.set(list));
      },
      error: (err: unknown) => this.loadError.set(getErrorMessage(err)),
    });
  }

  protected profileErr(name: string): string | null {
    return controlError(this.profileForm.get(name));
  }

  protected passwordErr(name: string): string | null {
    return controlError(this.passwordForm.get(name));
  }

  protected passwordMismatch(): boolean {
    const confirm = this.passwordForm.controls.confirmPassword;
    return (
      this.passwordForm.hasError('mismatch') &&
      (confirm.touched || confirm.dirty) &&
      !confirm.hasError('required')
    );
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileSuccess.set(null);

    this.users.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (user) => {
        this.auth.updateUser(user);
        this.savingProfile.set(false);
        this.profileSuccess.set('Profil mis à jour.');
      },
      error: (err: unknown) => {
        this.savingProfile.set(false);
        this.profileError.set(applyServerErrors(this.profileForm, err));
      },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.passwordError.set(null);
    this.passwordSuccess.set(null);

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.users.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordSuccess.set('Mot de passe modifié.');
        this.passwordForm.reset();
      },
      error: (err: unknown) => {
        this.savingPassword.set(false);
        this.passwordError.set(applyServerErrors(this.passwordForm, err));
      },
    });
  }
}
