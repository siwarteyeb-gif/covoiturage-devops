import { BookingStatus } from '../models/booking.models';
import { TripStatus } from '../models/trip.models';

export interface Badge {
  text: string;
  tone: 'g' | 'y' | 'z' | 'r' | 'b';
}

const TRIP: Record<TripStatus, Badge> = {
  OPEN: { text: 'Ouvert', tone: 'g' },
  FULL: { text: 'Complet', tone: 'y' },
  IN_PROGRESS: { text: 'En cours', tone: 'b' },
  DONE: { text: 'Terminé', tone: 'z' },
  CANCELLED: { text: 'Annulé', tone: 'r' },
};

const BOOKING: Record<BookingStatus, Badge> = {
  PENDING: { text: 'En attente', tone: 'y' },
  ACCEPTED: { text: 'Acceptée', tone: 'g' },
  REJECTED: { text: 'Refusée', tone: 'r' },
  CANCELLED: { text: 'Annulée', tone: 'z' },
};

export const tripBadge = (status: TripStatus): Badge => TRIP[status];
export const bookingBadge = (status: BookingStatus): Badge => BOOKING[status];

/** Date du jour au format AAAA-MM-JJ (valeur d'un champ date). */
export function todayIso(): string {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

/** Formate la durée : 25 min, 1 h 05. */
export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = String(minutes % 60).padStart(2, '0');
  return `${h} h ${m}`;
}
