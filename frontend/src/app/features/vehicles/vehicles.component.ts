import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Vehicle, VehicleRequest } from '../../core/models/vehicle.models';
import { VehicleService } from '../../core/services/vehicle.service';
import { applyServerErrors, controlError, getErrorMessage } from '../../core/utils/http-errors';

@Component({
  selector: 'app-vehicles',
  imports: [ReactiveFormsModule],
  templateUrl: './vehicles.component.html',
})
export class VehiclesComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly vehicleService = inject(VehicleService);

  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly listError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  /** id du véhicule en cours de modification (null = mode ajout) */
  protected readonly editingId = signal<number | null>(null);

  protected readonly form = this.fb.group({
    brand: ['', [Validators.required, Validators.maxLength(50)]],
    model: ['', [Validators.required, Validators.maxLength(50)]],
    color: ['', [Validators.maxLength(30)]],
    seats: [4, [Validators.required, Validators.min(1), Validators.max(8)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected err(name: string): string | null {
    return controlError(this.form.get(name));
  }

  load(): void {
    this.loading.set(true);
    this.listError.set(null);
    this.vehicleService.listMine().subscribe({
      next: (list) => {
        this.vehicles.set(list);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.listError.set(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  edit(vehicle: Vehicle): void {
    this.editingId.set(vehicle.id);
    this.formError.set(null);
    this.form.setValue({
      brand: vehicle.brand,
      model: vehicle.model,
      color: vehicle.color ?? '',
      seats: vehicle.seats,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.formError.set(null);
    this.form.reset({ brand: '', model: '', color: '', seats: 4 });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.formError.set(null);

    const value = this.form.getRawValue();
    const request: VehicleRequest = {
      brand: value.brand.trim(),
      model: value.model.trim(),
      color: value.color.trim() || null,
      seats: value.seats,
    };

    const id = this.editingId();
    const call = id === null ? this.vehicleService.create(request) : this.vehicleService.update(id, request);

    call.subscribe({
      next: (saved) => {
        if (id === null) {
          this.vehicles.update((list) => [saved, ...list]);
        } else {
          this.vehicles.update((list) => list.map((v) => (v.id === saved.id ? saved : v)));
        }
        this.saving.set(false);
        this.cancelEdit();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }

  remove(vehicle: Vehicle): void {
    if (!confirm(`Supprimer le véhicule ${vehicle.brand} ${vehicle.model} ?`)) {
      return;
    }
    this.listError.set(null);
    this.vehicleService.delete(vehicle.id).subscribe({
      next: () => {
        this.vehicles.update((list) => list.filter((v) => v.id !== vehicle.id));
        if (this.editingId() === vehicle.id) {
          this.cancelEdit();
        }
      },
      error: (err: unknown) => this.listError.set(getErrorMessage(err)),
    });
  }
}
