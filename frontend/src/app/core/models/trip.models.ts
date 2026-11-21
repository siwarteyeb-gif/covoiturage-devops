import { Vehicle } from './vehicle.models';

export type TripStatus = 'OPEN' | 'FULL' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export interface PersonSummary {
  id: number;
  name: string;
  averageRating: number;
  phone: string | null;
}

export interface Trip {
  id: number;
  driver: PersonSummary;
  vehicle: Vehicle;
  departureAddress: string;
  departureLat: number;
  departureLng: number;
  arrivalAddress: string;
  arrivalLat: number;
  arrivalLng: number;
  departureDateTime: string;
  totalSeats: number;
  availableSeats: number;
  pricePerSeat: number;
  distanceKm: number;
  durationMin: number;
  status: TripStatus;
  /** Points [lat, lng] de l'itinéraire (uniquement dans le détail d'un trajet). */
  route: number[][] | null;
  seriesId: string | null;
}

export interface TripSearchResult {
  trip: Trip;
  detourKm: number | null;
}

export interface TripRequest {
  vehicleId: number;
  departureAddress: string;
  departureLat: number;
  departureLng: number;
  arrivalAddress: string;
  arrivalLat: number;
  arrivalLng: number;
  departureDateTime: string;
  totalSeats: number;
  pricePerSeat: number;
  recurrenceWeekdays?: number[];
  recurrenceWeeks?: number;
}

export interface TripUpdateRequest {
  departureDateTime: string;
  totalSeats: number;
  pricePerSeat: number;
}

export interface TripSearchParams {
  from?: string;
  to?: string;
  fromLat?: number;
  fromLng?: number;
  toLat?: number;
  toLng?: number;
  date?: string;
  seats?: number;
  maxPrice?: number;
  maxDetourKm?: number;
  sort?: string;
}

export interface PlaceResult {
  label: string;
  lat: number;
  lng: number;
}

export interface RouteEstimate {
  distanceKm: number;
  durationMin: number;
  route: number[][];
  suggestedPrice: number;
  maxPrice: number;
  source: string;
}
