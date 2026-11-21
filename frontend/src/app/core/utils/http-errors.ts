import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiError } from '../models/auth.models';

/** Message lisible à afficher à l'utilisateur pour une erreur HTTP. */
export function getErrorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return "Impossible de contacter le serveur. Vérifiez que le back-end est démarré.";
    }
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body.message === 'string' && body.message.length > 0) {
      return body.message;
    }
    if (err.status === 401) return 'Authentification requise.';
    if (err.status === 403) return 'Accès refusé.';
    if (err.status === 404) return 'Ressource introuvable.';
  }
  return 'Une erreur est survenue. Veuillez réessayer.';
}

/**
 * Affiche sur les champs du formulaire les erreurs de validation renvoyées par le back-end
 * (fieldErrors) et retourne le message général à afficher en haut du formulaire.
 */
export function applyServerErrors(form: FormGroup, err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const fields = (err.error as Partial<ApiError> | null)?.fieldErrors;
    if (fields && Object.keys(fields).length > 0) {
      for (const [name, message] of Object.entries(fields)) {
        const control = form.get(name);
        if (control) {
          control.setErrors({ server: message });
          control.markAsTouched();
        }
      }
      return 'Veuillez corriger les champs signalés.';
    }
  }
  return getErrorMessage(err);
}

/** Message d'erreur d'un champ (uniquement s'il a été touché ou modifié). */
export function controlError(control: AbstractControl | null): string | null {
  if (!control || !control.invalid || !(control.touched || control.dirty)) {
    return null;
  }
  const e = control.errors ?? {};
  if (e['server']) return String(e['server']);
  if (e['required']) return 'Ce champ est obligatoire';
  if (e['email']) return 'Email invalide';
  if (e['minlength']) return `Minimum ${e['minlength'].requiredLength} caractères`;
  if (e['maxlength']) return `Maximum ${e['maxlength'].requiredLength} caractères`;
  if (e['min']) return `Valeur minimale : ${e['min'].min}`;
  if (e['max']) return `Valeur maximale : ${e['max'].max}`;
  if (e['pattern']) return 'Format invalide';
  if (e['mismatch']) return 'Les mots de passe ne correspondent pas';
  return 'Valeur invalide';
}
