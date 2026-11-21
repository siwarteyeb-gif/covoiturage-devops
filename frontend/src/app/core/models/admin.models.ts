import { Role } from './auth.models';

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: Role;
  blocked: boolean;
  averageRating: number;
  createdAt: string;
}

export interface Stats {
  usersCount: number;
  driversCount: number;
  tripsCount: number;
  openTripsCount: number;
  bookingsCount: number;
  acceptedBookings: number;
  fillRatePercent: number;
  popularRoutes: { departure: string; arrival: string; bookings: number }[];
}
