import { PersonSummary, TripStatus } from './trip.models';

export type BookingStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

export interface Booking {
  id: number;
  trip: {
    id: number;
    departureAddress: string;
    arrivalAddress: string;
    departureDateTime: string;
    pricePerSeat: number;
    status: TripStatus;
    driver: PersonSummary;
  };
  passenger: PersonSummary;
  seats: number;
  status: BookingStatus;
  createdAt: string;
  reviewedByMe: boolean;
}

export interface Review {
  id: number;
  bookingId: number;
  author: PersonSummary;
  target: PersonSummary;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ReviewRequest {
  rating: number;
  comment: string | null;
}
