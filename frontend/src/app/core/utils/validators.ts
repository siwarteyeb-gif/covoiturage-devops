import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Même règle que le back-end : 8 chiffres, avec ou sans +216. */
export const PHONE_PATTERN = /^(\+216)?[0-9]{8}$/;

/** Vérifie que deux champs d'un formulaire ont la même valeur. */
export function fieldsMatch(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const a = group.get(field)?.value;
    const b = group.get(confirmField)?.value;
    return a === b ? null : { mismatch: true };
  };
}
